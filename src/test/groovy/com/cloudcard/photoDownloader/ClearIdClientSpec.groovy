package com.cloudcard.photoDownloader

import spock.lang.*

import java.net.http.HttpClient
import java.net.http.HttpResponse


class ClearIdClientSpec extends Specification {

    ClearIdClient client
    HttpClient mockHttpClient
    HttpResponse<String> mockHttpResponse

    static final String mockApiUrl = "https://clear-id-api.mock"
    static final String mockStsUrl = "https://clear-id-sts.mock"
    static final String mockAccountId = "mockAccountId"
    static final String mockClientId = "mockClientId"
    static final String mockClientSecret = "mockClientSecret"

    void setup() {
        client = new ClearIdClient([
            apiUrl      : mockApiUrl,
            stsUrl      : mockStsUrl,
            accountId   : mockAccountId,
            clientId    : mockClientId,
            clientSecret: mockClientSecret,
        ])

        client.init()

        mockHttpClient = Mock(HttpClient)
        client.client = mockHttpClient

        mockHttpResponse = Mock(HttpResponse<String>)
    }

    void "test getSystemName"() {
        expect:
        client.systemName == "ClearId"
    }

    void "test authenticate VIA getValidToken when authToken is empty"() {
        when:
        String token = client.validToken

        then:
        token == "asdf"
        //TODO I'd like to figure out how to assert on the individual properties of the built request.
//        1 * mockHttpClient.send({
//            it.uri == URI.create("${mockStsUrl}/connect/token")
//        }, HttpResponse.BodyHandlers.ofString()) >> mockHttpResponse
        1 * mockHttpClient.send(_, HttpResponse.BodyHandlers.ofString()) >> mockHttpResponse
        1 * mockHttpResponse.statusCode() >> 200
        1 * mockHttpResponse.body() >> "{\"access_token\":\"asdf\"}"


        and: "no unexpected mocks"
        0 * _
    }

    void "test getValidToken when authToken already set"() {
        setup:
        client.authToken = "asdfasdf"

        when:
        String token = client.authToken

        then:
        token == "asdfasdf"
        0 * _
    }


    //TODO build out the rest of these tests.
    void "test getIdentity"() {

    }

    void "test putIdentityPicture"() {

    }

    void "test happyPath of putPhoto"() {

    }

    /**
     * This test actually calls out to the ClearId API.
     * It is really an integration test, should only be turned on when integration with this api needs to be tested directly.
     *
     * basically this test verifies that these requests all work without throwing exceptions.
     * Not really a unit test, just a driver to allow manual invoking of the full client chain.
     */
    @Ignore
    void "test all requests"() {
        setup:
        String base64Photo = "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";

        Properties appProps = new Properties()
        appProps.load(new FileInputStream("application-test.properties"))

        client = new ClearIdClient([
            apiUrl      : appProps.getProperty("ClearIdClient.apiUrl"),
            stsUrl      : appProps.getProperty("ClearIdClient.stsUrl"),
            accountId   : appProps.getProperty("ClearIdClient.accountId"),
            clientId    : appProps.getProperty("ClearIdClient.clientId"),
            clientSecret: appProps.getProperty("ClearIdClient.clientSecret"),
        ])

        expect:
        String clearIdIdentifier = client.getIdentity("100044616")
        assert client.putPhoto(clearIdIdentifier, Base64.decoder.decode(base64Photo))
        assert client.close()
    }

}
