package com.cloudcard.photoDownloader

import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList;

import jakarta.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

import javax.xml.namespace.NamespaceContext
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank;

@Component
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "WorkdayStorageService")
class WorkdayClient {

    static final Logger log = LoggerFactory.getLogger(WorkdayClient.class);

    @Value('${WorkdayClient.apiUrl}')
    String apiUrl

    @Value('${WorkdayClient.tenantName}')
    String tenantName

    //TODO currently I am reusing the ISU credentials that we had for the Person importer.
    // We need to validate an ISU with just permissions to the Personal Data: Personal Photo domain, make sure that works.
    @Value('${WorkdayClient.isu.username}')
    String username

    @Value('${WorkdayClient.isu.password}')
    String password

    String connectivityUrl
    String humanResourcesApi

    HttpClient client
    DocumentBuilder documentBuilder
    XPath xPath
    NamespaceContextUtil namespaceContextUtil


    @PostConstruct
    void init() {
        throwIfBlank(apiUrl, "The Workday API URL must be specified.")
        throwIfBlank(tenantName, "The Workday tenantName must be specified")
        throwIfBlank(username, "The Workday ISU username must be specified")
        throwIfBlank(password, "The Workday ISU password must be specified")

        log.info("     Workday API URL : $apiUrl")
        log.info(" Workday Tenant Name : tenantName")
        log.info("Workday ISU Username : $username")
        log.info("Workday ISU Password : ${password.length() > 0 ? "......" : ""}")

        humanResourcesApi = "$apiUrl/ccx/service/$tenantName/Human_Resources/v45.0"

        client = HttpClient.newBuilder().build()

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        factory.setNamespaceAware(true)
        documentBuilder = factory.newDocumentBuilder()

        namespaceContextUtil = new NamespaceContextUtil()
        xPath = XPathFactory.newInstance().newXPath()
        xPath.setNamespaceContext(namespaceContextUtil.createNamespaceContext([
            "env": "http://schemas.xmlsoap.org/soap/envelope/",
            "wd" : "urn:com.workday/bsvc"
        ]))
    }

    WorkdayResponse doWorkdayRequest(String requestName, String soapBody) {
        HttpRequest postRequest = HttpRequest.newBuilder()
            .uri(new URI(humanResourcesApi))
            .header("Content-Type", "application/xml")
            .header("Authorization", getBasicAuthenticationHeader(username, password))
            .POST(
                //TODO replace "bogus" nonce with a real one.
                HttpRequest.BodyPublishers.ofString("""\
                    <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:bsvc=\"urn:com.workday/bsvc\">
                        <soapenv:Header>
                            <wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">
                                <wsse:UsernameToken wsu:Id=\"bogus\">
                                    <wsse:Username>$username@$tenantName</wsse:Username>
                                    <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">$password</wsse:Password>
                                    <wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">bogus</wsse:Nonce>
                                </wsse:UsernameToken>
                            </wsse:Security>
                        </soapenv:Header>
                        <soapenv:Body>
                            $soapBody
                        </soapenv:Body>
                    </soapenv:Envelope>""".stripIndent()
                )
            ).build()
        log.debug("$requestName request: $postRequest")
        HttpResponse<String> postResponse = client.send(postRequest, HttpResponse.BodyHandlers.ofString())
        log.debug("$requestName status: ${postResponse.statusCode()}")

        return new WorkdayResponse(postResponse, documentBuilder.parse(new ByteArrayInputStream(postResponse.body().bytes)))
    }

    //TODO I don't think we need this; if we do, getWorker requires Get Access to Worker Data: Public Worker Reports.
    /*
    Node getWorker(String workerId) {
        WorkdayResponse getWorkerResponse = doWorkdayRequest("getWorker", """
            <bsvc:Get_Workers_Request xmlns:bsvc="urn:com.workday/bsvc">
                <bsvc:Request_References bsvc:Skip_Non_Existing_Instances="false">
                    <bsvc:Worker_Reference>
                        <bsvc:ID bsvc:type="Employee_ID">$workerId</bsvc:ID>
                    </bsvc:Worker_Reference>
                </bsvc:Request_References>
                <bsvc:Response_Group>
                    <bsvc:Include_Photo>true</bsvc:Include_Photo>
                </bsvc:Response_Group>
            </bsvc:Get_Workers_Request>
        """)

        if (getWorkerResponse.statusCode != 200) {
            throw new RuntimeException("Failed to get worker $workerId: ${getWorkerResponse.statusCode}")
        }

        //TODO confirm that this is the correct Path to the worker node.
        NodeList workers = xPath.evaluate("/env:Envelope/env:Body/wd:Get_Workers_Response/wd:Response_Data/wd:Worker", getWorkerResponse.body, XPathConstants.NODESET) as NodeList

        if (workers.length == 0) {
            throw new RuntimeException("Worker with ID $workerId not found")
        }

        //TODO convert this to a worker object.
        return workers.item(0)
    }
     */

    void putWorkerPhoto(String workerId, String photoBase64) {
        WorkdayResponse putWorkerPhotoResponse = doWorkdayRequest("putWorkerPhoto", """
            <bsvc:Put_Worker_Photo_Request xmlns:bsvc="urn:com.workday/bsvc">
                <bsvc:Worker_Reference>
                    <bsvc:ID bsvc:type="Employee_ID">$workerId</bsvc:ID>
                </bsvc:Worker_Reference>
                <bsvc:Worker_Photo_Data>
                    <bsvc:Filename>${workerId}.jpg</bsvc:Filename>
                    <bsvc:File>$photoBase64</bsvc:File>
                </bsvc:Worker_Photo_Data>
            </bsvc:Put_Worker_Photo_Request>"""
        )

        if (putWorkerPhotoResponse.statusCode != 200) {
            log.error(putWorkerPhotoResponse.response.body())
            throw new RuntimeException("Failed to put worker photo for $workerId: ${putWorkerPhotoResponse.statusCode}")
        }

        //todo - figure out if we need to assert anything about the response body.
    }

    static final String getBasicAuthenticationHeader(String username, String password) {
        String valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
}

class WorkdayResponse {
    int statusCode
    HttpResponse<String> response
    Document body

    WorkdayResponse (HttpResponse<String> response, Document document) {
        this.response = response
        this.statusCode = response.statusCode()
        this.body = document
    }
}

class NamespaceContextUtil {
    static NamespaceContext createNamespaceContext(Map<String, String> namespaceMap) {
        return new NamespaceContext() {
            @Override
            String getNamespaceURI(String prefix) {
                namespaceMap.get(prefix)
            }

            @Override
            String getPrefix(String namespaceURI) {
                namespaceMap.find { it.value == namespaceURI }?.key
            }

            @Override
            Iterator<String> getPrefixes(String namespaceURI) {
                namespaceMap.keySet().iterator()
            }
        }
    }
}