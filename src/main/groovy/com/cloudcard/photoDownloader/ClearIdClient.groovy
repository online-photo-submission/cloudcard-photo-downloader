package com.cloudcard.photoDownloader

import org.w3c.dom.Document
import jakarta.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

@Component
@ConditionalOnProperty(value = "IntegrationStorageService.client", havingValue = "ClearIdClient")
class ClearIdClient implements IntegrationStorageClient {

    static final Logger log = LoggerFactory.getLogger(ClearIdClient.class)

    @Value('${ClearIdClient.apiUrl}')
    String apiUrl

    @Value('${ClearIdClient.accountId}')
    String accountId

    @Value('${ClearIdClient.clientId}')
    String clientId

    @Value('${ClearIdClient.clientSecret}')
    String clientSecret

    @PostConstruct
    void init() {
        throwIfBlank(apiUrl, "The ClearId API URL must be specified.")
        throwIfBlank(accountId, "The ClearId accountId must be specified")
        throwIfBlank(clientId, "The ClearId clientId must be specified")
        throwIfBlank(clientSecret, "The ClearId clientSecret must be specified")

        log.info("     ClearId API URL : $apiUrl")
        log.info("   ClearId accountId : $accountId")
        log.info("    ClearId clientId : $clientId")
        log.info("ClearId clientSecret : ${clientSecret.length() > 0 ? "......" : ""}")
    }

    @Override
    String getSystemName() {
        return "ClearId"
    }

    WorkdayResponse doWorkdayRequest(String requestName, String soapBody) {
        HttpRequest postRequest = HttpRequest.newBuilder()
            .uri(new URI(humanResourcesApi))
            .header("Content-Type", "application/xml")
            .POST(
                HttpRequest.BodyPublishers.ofString("""\
                    <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:bsvc=\"urn:com.workday/bsvc\">
                        <soapenv:Header>
                            ${generateSecurityHeader()}
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

    @Override
    void putPhoto(String workerId, String photoBase64) {
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
    }

    @Override
    void close() {
        // WorkdayClient does not maintain any resources that need to be closed.
    }

    /**
     * Generates wsse security header for Workday API requests.
     * Documentation for the security header can be found here:
     *     https://docs.oasis-open.org/wss/v1.1/wss-v1.1-spec-pr-UsernameTokenProfile-01.htm#_Toc104276211
     *
     * @return String containing the wsse:Security header XML.
     */
    String generateSecurityHeader() {
        """
            <wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">
                <wsse:UsernameToken>
                    <wsse:Username>$username@$tenantName</wsse:Username>
                    <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">$password</wsse:Password>
                </wsse:UsernameToken>
            </wsse:Security>
        """
    }
}
