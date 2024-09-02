package com.cloudcard.photoDownloader

import io.jsonwebtoken.Jwts
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper

import java.security.Key
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

@Component
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "OrigoStorageService")
class OrigoClient {
    // Makes requests to Origo API

    private static final Logger log = LoggerFactory.getLogger(OrigoClient.class)

    @Autowired
    UnirestWrapper unirestWrapper

    @Autowired
    SimpleResponseLogger simpleResponseLogger

    @Value('${Origo.eventManagementApi}')
    String eventManagementApi

    @Value('${Origo.mobileIdentitiesApi}')
    String mobileIdentitiesApi

    @Value('${Origo.certIdpApi}')
    String certIdpApi

    @Value('${Origo.organizationId}')
    String organizationId

    @Value('${Origo.contentType}')
    String contentType

    @Value('${Origo.applicationVersion}')
    String applicationVersion

    @Value('${Origo.applicationId}')
    String applicationId

    @Value('${Origo.clientId}')
    String clientId

    @Value('${Origo.clientSecret}')
    String clientSecret

    @Value('${Origo.usePkiAuth}')
    boolean usePkiAuth

    @Value('${Origo.tokenUrl}')
    String tokenUrl

    @Value('${Origo.privateKey}')
    String privateKey

    String accessToken

    boolean isAuthenticated = false

    Map<String, String> requestHeaders

    @PostConstruct
    init() {
        throwIfBlank(eventManagementApi, "The Origo Event Management API URL must be specified.")
        throwIfBlank(mobileIdentitiesApi, "The Origo Mobile Identities API URL must be specified.")
        throwIfBlank(organizationId, "The Origo organization ID must be specified.")
        throwIfBlank(clientSecret, "The Origo client secret must be specified.")
        throwIfBlank(clientId, "The Origo client ID must be specified.")
        throwIfBlank(contentType, "The Origo content-type header must be specified.")
        throwIfBlank(applicationVersion, "The Origo application version must be specified.")
        throwIfBlank(applicationId, "Your organization's Origo Application ID must be specified.")
        throwIfBlank(usePkiAuth.toString(), "Authentication Preference must be specified.")
        throwIfBlank((usePkiAuth && tokenUrl).toString(), "Token URL must be specified.")

        log.info('=================== Initializing Origo Client ===================')
        log.info("          Origo Event Management API URL : $eventManagementApi")
        log.info("         Origo Mobile Identities API URL : $mobileIdentitiesApi")
        log.info("                   Origo organization ID : $organizationId")
        log.info("               Origo content type header : $contentType")
        log.info("               Origo application version : $applicationVersion")
        log.info("                    Origo application ID : $applicationId")
        log.info("               Authentication Preference : ${usePkiAuth ? "PKI" : "Password"}")

        simpleResponseLogger.source = this.class.simpleName

    }

    void authenticate(ResponseWrapper responseWithToken) {
        // Stores token for future requests

        String token = ""

        if (responseWithToken.success) {
            token = responseWithToken.body.access_token
            accessToken = token
            requestHeaders = [
                    'Authorization'      : "Bearer $token" as String,
                    'Content-Type'       : contentType,
                    'Application-Version': applicationVersion,
                    'Application-ID'     : applicationId
            ]
            isAuthenticated = true
        } else {
            log.error("Error while authenticating with Origo.")
            isAuthenticated = false
        }

    }

    ResponseWrapper makeAuthenticatedRequest(Closure request) {

        ResponseWrapper response

        if (!isAuthenticated && authenticate(requestAccessToken()) || isAuthenticated) {
            response = request()
        } else {
            response = new ResponseWrapper(new AuthException())
        }

        response
    }

    String getJswt(String keyStr) {
        // Input is an UNENCRYPTED RSA / PEM-encoded PKCS#8 private key.

        if (!keyStr) {
            throw new IllegalArgumentException("Key string not provided")
        }

        keyStr = keyStr.replaceAll("-----BEGIN PRIVATE KEY-----", "").replaceAll("-----END PRIVATE KEY-----", "").replaceAll("\\s", "")
                .replaceAll("\\n", "").trim()

        byte[] keyBytes = Base64.getDecoder().decode(keyStr)
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes)
        KeyFactory keyFactory = KeyFactory.getInstance("RSA")
        Key key = keyFactory.generatePrivate(keySpec)

        long currentTimeMils = System.currentTimeMillis() - 5
        Date currentTime = new Date(currentTimeMils)
        Date expiration = new Date(currentTimeMils + 3600000)

        Jwts.builder()
                .subject(clientId)
                .issuer(clientId)
                .audience().add(tokenUrl).and()
                .notBefore(currentTime)
                .issuedAt(currentTime)
                .expiration(expiration)
                .id(UUID.randomUUID().toString())
                .signWith(key)
                .compact()
    }

    ResponseWrapper requestAccessToken() {
        if (usePkiAuth) requestAccessTokenPki()
        else requestAccessTokenPassword()
    }

    ResponseWrapper requestAccessTokenPki() {
        // https://doc.origo.hidglobal.com/api/authentication/

        String pkiCredentials = getJswt(privateKey)

        String url = "$certIdpApi/authentication/customer/$organizationId/token"
        Map<String, String> headers = ["Content-Type": "application/x-www-form-urlencoded"]
        String body = "grant_type=client_credentials&client_assertion=$pkiCredentials"

        ResponseWrapper response
        try {
            response = new ResponseWrapper(unirestWrapper.post(url, headers, body))
        } catch (Exception e) {
            response = new ResponseWrapper(e)
        }
        simpleResponseLogger.log("requestAccessTokenPki", response, "Error while authenticating with Origo.")

        return response

    }

    ResponseWrapper requestAccessTokenPassword() {
        // https://doc.origo.hidglobal.com/api/authentication/

        String url = "$certIdpApi/authentication/customer/$organizationId/token"
        Map<String, String> headers = ["Content-Type": "application/x-www-form-urlencoded"]
        String body = "client_id=${clientId}&client_secret=${clientSecret}&grant_type=client_credentials"

        ResponseWrapper response
        try {
            response = new ResponseWrapper(unirestWrapper.post(url, headers, body))
        } catch (Exception e) {
            response = new ResponseWrapper(e)
        }
        simpleResponseLogger.log("requestAccessTokenPassword", response, "Error while authenticating with Origo.")

        return response

    }

    ResponseWrapper uploadUserPhoto(Photo photo, String fileType) {
        // https://doc.origo.hidglobal.com/api/mobile-identities/#/Photo%20ID/post-customer-organization_id-users-user_id-photo
        ResponseWrapper response

        String url = "$mobileIdentitiesApi/customer/$organizationId/users/${photo.person.identifier}/photo"
        Map<String, String> headers = requestHeaders.clone() as Map
        headers["Content-Type"] = "application/vnd.assaabloy.ma.credential-management-2.2+$fileType" as String

        try {
            response = new ResponseWrapper(unirestWrapper.post(url, headers, photo.bytes))
        } catch (Exception e) {
            response = new ResponseWrapper(e)
        }

        simpleResponseLogger.log("uploadUserPhoto", response)

        return response
    }

    ResponseWrapper accountPhotoApprove(String userId, String id) {
        // https://doc.origo.hidglobal.com/api/mobile-identities/#/Photo%20ID/put-customer-organization_id-users-user_id-photo-photo_id-status

        ResponseWrapper response

        String url = "$mobileIdentitiesApi/customer/$organizationId/users/$userId/photo/$id/status"
        String serializedBody = new ObjectMapper().writeValueAsString([status: "APPROVE"])

        try {
            response = new ResponseWrapper(unirestWrapper.put(url, requestHeaders, serializedBody))
        } catch (Exception e) {
            response = new ResponseWrapper(e)
        }

        simpleResponseLogger.log("accountPhotoApprove", response)

        return response
    }

    ResponseWrapper getUserDetails(String userId) {
        // https://doc.origo.hidglobal.com/api/mobile-identities/#/Users/get-customer-organization_id-users-user_id

        ResponseWrapper response

        String url = "$mobileIdentitiesApi/customer/$organizationId/users/$userId"

        try {
            response = new ResponseWrapper(unirestWrapper.get(url, requestHeaders))
        } catch (Exception e) {
            response = new ResponseWrapper(e)
        }

        simpleResponseLogger.log("getUserDetails", response)

        return response
    }

    ResponseWrapper deletePhoto(String userId, String photoId) {
        // https://doc.origo.hidglobal.com/api/mobile-identities/#/Photo%20ID/delete-customer-organization_id-users-user_id-photo-photo_id

        ResponseWrapper response

        String url = "$mobileIdentitiesApi/customer/$organizationId/users/$userId/photo/$photoId"

        try {
            response = new ResponseWrapper(unirestWrapper.delete(url, requestHeaders))
        } catch (Exception e) {
            response = new ResponseWrapper(e)
        }

        simpleResponseLogger.log("deletePhoto", response)

        return response
    }
}
