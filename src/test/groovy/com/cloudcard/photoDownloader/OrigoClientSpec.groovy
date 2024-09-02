package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import spock.lang.Specification
import spock.lang.Unroll
import io.jsonwebtoken.security.SignatureException

class OrigoClientSpec extends Specification {
    OrigoClient origoClient

    String accessToken = "this-is-the-original-access-token"

    def setup() {
        UnirestWrapper unirestWrapper = Mock()
        SimpleResponseLogger simpleResponseLogger = Mock()

        origoClient = new OrigoClient(simpleResponseLogger: simpleResponseLogger, unirestWrapper: unirestWrapper)

        origoClient.eventManagementApi = "https://api.mock-event-management.com"
        origoClient.mobileIdentitiesApi = "https://api.mock-mobile-identities.com"
        origoClient.certIdpApi = "https://api.mock-cert-idp.com"
        origoClient.organizationId = "12345"
        origoClient.contentType = "application/vnd.assaabloy.ma.credential-management-2.2+json"
        origoClient.applicationVersion = "2.2"
        origoClient.applicationId = "ORIGO-APPLICATION"
        origoClient.clientId = "67890"
        origoClient.clientSecret = "Password1234"
    }

    def "Should be initialized"() {
        expect:
        origoClient != null
    }

    def "should instantiate unirest wrapper"() {
        expect:
        origoClient.unirestWrapper != null
    }

    def "should instantiate simpleResponseLogger"() {
        expect:
        origoClient.simpleResponseLogger != null
    }

    def "authenticate should hydrate properties with token"() {
        given:
        Object responseBody = '{"access_token" : "this-is-your-mock-access-token"}'
        ResponseWrapper responseWithToken = new ResponseWrapper(200, responseBody)
        origoClient = Spy(OrigoClient) {
            it.isAuthenticated = false
            it.requestHeaders = null
        }

        when:
        origoClient.authenticate(responseWithToken)

        then:
        origoClient.isAuthenticated
        origoClient.requestHeaders == [
                'Authorization'      : 'Bearer this-is-your-mock-access-token',
                'Content-Type'       : origoClient.contentType,
                'Application-Version': origoClient.applicationVersion,
                'Application-ID'     : origoClient.applicationId
        ]
    }

    def "getJswt should throw IllegalArgumentException if given no input"() {
        when:
        origoClient.getJswt()

        then:
        thrown(IllegalArgumentException)
    }

    def "getJswt should output a Json web token from an unencrypted private key"() {
        given:
        String privKey = """-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCXO+hKPW7oqP1L
uh76Z1zZduYg53T7VJ95DoPJicNTZkPMwsTSHrPVoreU/oAk5bTRjWTx95Ll/MUV
CKoxRlQR2qL1Gr8hDPkaDN5PE263u48zRad4QRT7brXjJjWREAxbHq3kUeC6Mgqs
TnNpGyo3AUvIAL0nQVzMxkjf6jNarnX/pgoBYApS9P2c3wP8Xi26QH65tvExEEi7
+gNrZkvc3J73ptXmZVfibsoA9f8j9Y9RHXyVEeDtkjj1ITaCduBO6HFZN87u1ErR
xgggWlpHipIjhSFYAakJT3cKRpQdYsZ5luB4HDlGsweiQ/YEdcTfaWUDgs4on74u
OAfmVy91AgMBAAECggEACVcniKD0tE/uR6Vje78IirHU6BwDgJurDHSrX7YvfHt+
HSu7sOfj3tehk6/dmunJBNbrxiSuoPUUowZET1jIS8tUxzieAgeQFwjLw4HavmRK
JYuGxV7s/H52Tg+HvWw1UdIPlrYWislubqfwaYHGVgaxdyp8hEEoNHQxVLWynZtn
2p4ZvQG/texp2KyjwWamNcek5hVT6V9ZBkKnPgDDoWotF9bV3O4wTGQFhdSLwPe5
Dhf+QH7GpNLJTLSk36CWZe9oobfWEXUW6hbEXdvzDeFXkTKmfcZCMVNzy3gg5SVg
lOWmZHbBNXxX4yRFhhODmI6HKS+/dsjppvtsXCLnAQKBgQDIFsdjMW1u7bnLRbiu
kMfbd+xeqistDFY2SirnZEA67JNPxqO6gdHs1WHqkA3c1+U4XE2y6w+vFg8PHG2Z
8PvkuPbaIgQSPavpgBJJZ5Qr37LmB/donHuRTzLGyNXNDLAYqJ+frF5qzuBESNEW
zvFMFRt2sencTUOJip7dbhb5wQKBgQDBflLa0KedYHV2B44dYacrYKzLunyGLlhP
cFUjiKfZhgYRRbgjBLDwkci4R6UOUisnmK4I1l0SI3xzmk1ZC9U2afUjh7TF6Ext
mUZiZ+IZdfan4M4a3SpjOiqIJC71EfIXD9bPflGSEt5K4G1pZr3mLkn8QECszOUl
/l+Be3satQKBgE6xXsOtWdvJ1UuT5TmKqX+wX58vkAGMm4+Ihe0xaW2DQ8CZYCVn
D1f06CindTxJENakvs5dUo3KwCtyQ8zKkVb4Q+WwgSdnZ+hSvV5vRUAoH6UGtxBG
kvaoTlEOBcA6YDuiffz/frbFHEDe3pT42L/SgVWiTVaecJR0l1yBArpBAoGAfwdA
u9i6Aa6+zoL9QLrIdcjdCi7gy3KHlXdo3ZAsqbi8KQC9d3fv9a+vt5OJf9Jf9Hne
33xj07GqVXaaivgivLtpLS89daQg8N7sf1Q18oMoMGR8ytQhrM0V+RTQIyHp7kv+
uW/ze1OZzkxyZ/0EdKY0+j7wdsF5Oq055Ba95fUCgYEAnulTbGtQrIa3n/A8uq52
bU1ku6f6FGtdLNpXjD79C9PxvV0wQns+P6ozilfBJmEQXnjC6kOz4O3o6fyJqpeH
uCPKTvD6Pl+uTfGy8WFiKSgLLkDO4RDR53x8DOKV9f3iQu9mEg0hrYl+Csq6mbP+
qFxHSfOBBcJqv7iTzI972is=
-----END PRIVATE KEY-----

"""

        when:
        String jswt = origoClient.getJswt(privKey)

        then:
        jswt != null
        jswt.length() > 10
    }

    def "getJswt should throw SignatureException if passed invalid key"() {
        given:
        String privKey = """-----BEGIN PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCXO+hKPW7oqP1L
uh76Z1zZduYg53T7VJ95DoPJicNTZkPMwsTSHrPVoreU/oAk5bTRjWTx95Ll/MUV
CKoxRlQR2qL1Gr8hDPkaDN5PE263u48zRad4QRT7brXjJjWREAxbHq3kUeC6Mgqs
TnNpGyo3AUvIAL0nQVzMxkjf6jNarnX/pgoBYApS9P2c3wP8Xi26QH65tvExEEi7
+gNrZkvc3J73ptXmZINVALID9f8j9Y9RHXyVEeDtkjj1ITaCduBO6HFZN87u1ErR
xgggWlpHipIjhSFYAakJT3cKRpQdYsZ5luB4HDlGsweiQ/YEdcTfaWUDgs4on74u
OAfmVy91AgMBAAECggEACVcniKD0tE/uR6Vje78IirHU6BwDgJurDHSrX7YvfHt+
HSu7sOfj3tehk6/dmunJBNbrxiSuoPUUowZET1jIS8tUxzieAgeQFwjLw4HavmRK
JYuGxV7s/H52Tg+HvWw1UdIPlrYWislubqfwaYHGVgaxdyp8hEEoNHQxVLWynZtn
2p4ZvQG/texp2KyjwWamNcek5hVT6V9ZBkKnPgDDoWotF9bV3O4wTGQFhdSLwPe5
Dhf+QH7GpNLJTLSk36CWZe9oobfWEXUW6hbEXdvzDeFXkTKmfcZCMVNzy3gg5SVg
lOWmZHbBNXxX4yRFhhODmI6HKS+/dsjppvtsXCLnAQKBgQDIFsdjMW1u7bnLRbiu
kMfbd+xeqistDFY2SirnZEA67JNPxqO6gdHs1WHqkA3c1+U4XE2y6w+vFg8PHG2Z
8PvkuPbaIgQSPavpgBJJZ5Qr37LmB/donHuRTzLGyNXNDLAYqJ+frF5qzuBESNEW
zvFMFRt2sencTUOJip7dbhb5wQKBgQDBflLa0KedYHV2B44dYacrYKzLunyGLlhP
cFUjiKfZhgYRRbgjBLDwkci4R6UOUisnmK4I1l0SI3xzmk1ZC9U2afUjh7TF6Ext
mUZiZ+IZdfan4M4a3SpjOiqIJC71EfIXD9bPflGSEt5K4G1pZr3mLkn8QECszOUl
/l+Be3satQKBgE6xXsOtWdvJ1UuT5TmKqX+wX58vkAGMm4+Ihe0xaW2DQ8CZYCVn
D1f06CindTxJENakvs5dUo3KwCtyQ8zKkVb4Q+WwgSdnZ+hSvV5vRUAoH6UGtxBG
kvaoTlEOBcA6YDuiffz/frbFHEDe3pT42L/SgVWiTVaecJR0l1yBArpBAoGAfwdA
u9i6Aa6+zoL9QLrIdcjdCi7gy3KHlXdo3ZAsqbi8KQC9d3fv9a+vt5OJf9Jf9Hne
33xj07GqVXaaivgivLtpLS89daQg8N7sf1Q18oMoMGR8ytQhrM0V+RTQIyHp7kv+
uW/ze1OZzkxyZ/0EdKY0+j7wdsF5Oq055Ba95fUCgYEAnulTbGtQrIa3n/A8uq52
bU1ku6f6FGtdLNpXjD79C9PxvV0wQns+P6ozilfBJmEQXnjC6kOz4O3o6fyJqpeH
uCPKTvD6Pl+uTfGy8WFiKSgLLkDO4RDR53x8DOKV9f3iQu9mEg0hrYl+Csq6mbP+
qFxHSfOBBcJqv7iTzI972is=
-----END PRIVATE KEY-----

"""

        when:
        String jswt = origoClient.getJswt(privKey)

        then:
        jswt == null
        thrown(SignatureException)
    }

    def "requestAccessToken should call password authentication when usePki == false"() {
        given:
        origoClient = Spy(OrigoClient) {
            it.usePkiAuth = false
        }

        when:
        origoClient.requestAccessToken()

        then:
        1 * origoClient.requestAccessTokenPassword() >> new ResponseWrapper(200)
        0 * origoClient.requestAccessTokenPki() >> new ResponseWrapper(200)

    }

    def "requestAccessToken should call pki authentication when usePki == true"() {
        given:
        origoClient = Spy(OrigoClient) {
            it.usePkiAuth = true
        }

        when:
        origoClient.requestAccessToken()

        then:
        0 * origoClient.requestAccessTokenPassword() >> new ResponseWrapper(200)
        1 * origoClient.requestAccessTokenPki() >> new ResponseWrapper(200)

    }

    @Unroll
    def "requestAccessTokenPki should properly handle different Http responses - status: #status, body: #body"() {
        given:
        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.post(_, _, _) >> httpResponse

        SimpleResponseLogger simpleResponseLogger = Mock()

        origoClient = Spy(OrigoClient) {
            it.usePkiAuth = true
            it.privateKey = "1234"
            it.getJswt(_) >> { "123456789abcdefghijklmnopqrstuvwxyz" }
        }
        origoClient.simpleResponseLogger = simpleResponseLogger
        origoClient.unirestWrapper = unirestWrapper

        when:
        ResponseWrapper response = origoClient.requestAccessTokenPki()

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)

        where:

        status | body
        200    | "{'access_token': 'some-access-token'"
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null
    }

    @Unroll
    def "requestAccessTokenPassword should properly handle different Http responses - status: #status, body: #body"() {
        given:
        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.post(_, _, _) >> httpResponse

        SimpleResponseLogger simpleResponseLogger = Mock()

        origoClient = Spy(OrigoClient) {
            origoClient.usePkiAuth = false
        }
        origoClient.simpleResponseLogger = simpleResponseLogger
        origoClient.unirestWrapper = unirestWrapper

        when:
        ResponseWrapper response = origoClient.requestAccessTokenPassword()

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)

        where:

        status | body
        200    | "{'access_token': 'some-access-token'"
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null
    }

    def "Should fail to authenticate with bad request"() {
        given:
        origoClient = Spy(OrigoClient) {
            it.requestAccessToken() >> new ResponseWrapper(400)
        }

        when:
        origoClient.authenticate(origoClient.requestAccessToken())

        then:
        !origoClient.isAuthenticated
        origoClient.accessToken == null
    }

    def "Should authenticate with good request"() {
        given:
        Object responseBody = '{"access_token" : "this-is-your-mock-access-token"}'

        origoClient = Spy(OrigoClient) {
            it.requestAccessToken() >> new ResponseWrapper(200, responseBody)
        }

        when:
        origoClient.authenticate(origoClient.requestAccessToken())

        then:
        origoClient.isAuthenticated
        origoClient.accessToken == "this-is-your-mock-access-token"
    }

    def "makeAuthenticatedRequest should authenticate if !isAuthenticated"() {
        given:
        Object responseBody = '{"access_token" : "this-is-your-mock-access-token"}'
        ResponseWrapper responseWithToken = new ResponseWrapper(200, responseBody)

        origoClient = Spy(OrigoClient) {
            it.isAuthenticated = false
            it.requestAccessToken() >> responseWithToken
            it.authenticate(it.requestAccessToken()) >> {
                origoClient.isAuthenticated = true
                return true
            }

        }

        when:
        origoClient.makeAuthenticatedRequest { new ResponseWrapper(200, "Some information") }

        then:
        1 * origoClient.authenticate(responseWithToken)
        origoClient.isAuthenticated

    }

    def "makeAuthenticatedRequest should skip authentication if isAuthenticated == true"() {
        given:
        Object responseBody = '{"access_token" : "this-is-your-mock-access-token"}'
        ResponseWrapper responseWithToken = new ResponseWrapper(200, responseBody)

        origoClient = Spy(OrigoClient) {
            it.isAuthenticated = true
        }
        origoClient.requestAccessToken() >> responseWithToken

        when:
        origoClient.makeAuthenticatedRequest { new ResponseWrapper(200, "Some info") }

        then:
        0 * origoClient.authenticate(_)
        origoClient.isAuthenticated
    }

    def "makeAuthenticatedRequest should return response with AuthException if authentication fails"() {
        given:
        ResponseWrapper responseWithToken = new ResponseWrapper(400, "Bad Request")

        origoClient = Spy(OrigoClient) {
            it.isAuthenticated = false
            it.requestAccessToken() >> responseWithToken
        }

        when:
        ResponseWrapper response = origoClient.makeAuthenticatedRequest { new ResponseWrapper(200, "Some info") }

        then:
        1 * origoClient.authenticate(_)
        !origoClient.isAuthenticated
        response.exception != null
        !response.success

    }

    @Unroll
    def "uploadUserPhoto should properly handle different Http responses. - body: #body status: #status"() {
        given:

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[]{1})

        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.post(_, _, _) >> httpResponse

        origoClient.unirestWrapper = unirestWrapper
        origoClient.requestHeaders = [
                'Authorization'      : "Bearer $accessToken" as String,
                'Content-Type'       : origoClient.contentType,
                'Application-Version': origoClient.applicationVersion,
                'Application-ID'     : origoClient.applicationId
        ]

        when:
        ResponseWrapper response = origoClient.uploadUserPhoto(photo, "jpg")

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)

        where:

        status | body
        200    | '{"id" : "new-photo-id"}'
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null

    }

    @Unroll
    def "accountPhotoApprove should properly handle different Http responses. - body: #body status: #status"() {
        given:

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[]{1})

        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.put(_, _, _) >> httpResponse

        origoClient.unirestWrapper = unirestWrapper
        origoClient.requestHeaders = [
                'Authorization'      : "Bearer $accessToken" as String,
                'Content-Type'       : origoClient.contentType,
                'Application-Version': origoClient.applicationVersion,
                'Application-ID'     : origoClient.applicationId
        ]

        when:
        ResponseWrapper response = origoClient.accountPhotoApprove(photo.person.identifier, "12345")

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)

        where:

        status | body
        200    | '{"body" : "Photo approved"}'
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null

    }

    @Unroll
    def "getUserDetails should properly handle different Http responses. - body: #body status: #status"() {
        given:

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[]{1})

        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.get(_, _) >> httpResponse

        origoClient.unirestWrapper = unirestWrapper
        origoClient.requestHeaders = [
                'Authorization'      : "Bearer $accessToken" as String,
                'Content-Type'       : origoClient.contentType,
                'Application-Version': origoClient.applicationVersion,
                'Application-ID'     : origoClient.applicationId
        ]

        when:
        ResponseWrapper response = origoClient.getUserDetails("12345")

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)

        where:

        status | body
        200    | '{"body" : "some user details"}'
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null

    }

    @Unroll
    def "deletePhoto should properly handle different Http responses. - body: #body status: #status"() {
        given:
        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.delete(_, _) >> httpResponse

        origoClient.unirestWrapper = unirestWrapper
        origoClient.requestHeaders = [
                'Authorization'      : "Bearer $accessToken" as String,
                'Content-Type'       : origoClient.contentType,
                'Application-Version': origoClient.applicationVersion,
                'Application-ID'     : origoClient.applicationId
        ]

        when:
        ResponseWrapper response = origoClient.deletePhoto("12345", "6789")

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)

        where:

        status | body
        204    | ''
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null

    }

}