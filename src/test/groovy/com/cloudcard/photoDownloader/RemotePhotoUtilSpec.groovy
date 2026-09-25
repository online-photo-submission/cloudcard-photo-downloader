package com.cloudcard.photoDownloader

import com.cloudcard.photoDownloader.exception.CloudCardApiException
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant

import static com.cloudcard.photoDownloader.CloudCardClient.*

/**
 * Wire contract for RemotePhotoUtil, tested against a loopback HTTP server so it holds regardless
 * of which HTTP client RemotePhotoUtil uses. Each route matches method + path exactly; a request to
 * the wrong one gets a 404 and the test fails.
 *
 * Every feature gets its own server, so no state is shared between features.
 */
class RemotePhotoUtilSpec extends Specification {

    private static final String TOKEN = "auth-token"

    HttpServer server
    String host
    Map<String, List> routes = [:].asSynchronized()
    List<Map> requests = [].asSynchronized()

    def setup() {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0)
        server.createContext("/", { HttpExchange ex -> handle(ex) } as HttpHandler)
        server.start()
        host = "http://localhost:${server.address.port}"
    }

    def cleanup() { server.stop(0) }

    def "a #status is permanent: #permanent"() {
        given:
        route("POST", "/api/authenticationTokens", status)

        when:
        RemotePhotoUtil.login("${host}/api", "pat")

        then:
        CloudCardApiException e = thrown()
        e.permanent == permanent

        where:
        status || permanent
        400    || true
        401    || true
        403    || true
        422    || true
        404    || false
        503    || false
    }

    def "login"() {
        given:
        route("POST", "/api/authenticationTokens", 200, JsonOutput.toJson([tokenValue: "abc", expirationDate: "2026-09-25T12:00:00Z"]))

        expect:
        RemotePhotoUtil.login("${host}/api", "pat").tokenValue == "abc"
        body().persistentAccessToken == "pat"
        !authHeader()
    }

    def "logout, which never throws"() {
        given:
        route("POST", "/api/people/me/logout", 500)

        when:
        RemotePhotoUtil.logout("${host}/api", TOKEN)

        then:
        noExceptionThrown()
        authHeader() == TOKEN
        body().authenticationToken == TOKEN
    }

    def "getRemoteConfig"() {
        given:
        route("GET", "/api/integrations/downloader", 200, JsonOutput.toJson([version: 7]))

        expect:
        RemotePhotoUtil.getRemoteConfig("${host}/api", "downloader", TOKEN).version == 7
        queryParams() == [findBy: "name"]
        authHeader() == TOKEN
    }

    def "getRemoteConfig rejects an integration name of '#name' without making a request"() {
        when:
        RemotePhotoUtil.getRemoteConfig("${host}/api", name, TOKEN)

        then:
        thrown(IllegalArgumentException)
        requests.isEmpty()

        where:
        name << [null, ""]
    }

    def "fetchPhotos"() {
        given:
        route("GET", "/api/trucredential/${TOKEN}/photos", 200, JsonOutput.toJson([[id: 1]]))

        expect:
        RemotePhotoUtil.fetchPhotos("${host}/api", TOKEN, READY_FOR_DOWNLOAD)*.id == [1]
        queryParams().status == READY_FOR_DOWNLOAD
        authHeader() == TOKEN
    }

    def "updateStatus sends the failure reason intact"() {
        given:
        String reason = "bad reason & retry"
        route("PUT", "/api/photos/42", 200, JsonOutput.toJson([id: 42, status: FAILED]))

        expect:
        RemotePhotoUtil.updateStatus("${host}/api", TOKEN, new Photo(id: 42), FAILED, reason).status == FAILED

        // One parameter, decoded back to the original -- the & did not split it and nothing was double-encoded.
        queryParams() == [failedReason: reason]
        body().status == FAILED
        authHeader() == TOKEN
    }

    def "fetchStsCredentials"() {
        given:
        route("POST", "/api/status-queues/credentials", 200, JsonOutput.toJson([
            accessKeyId: "id", secretAccessKey: "secret", sessionToken: "session", expiration: "2026-09-25T13:00:00Z"]))

        when:
        def credentials = RemotePhotoUtil.fetchStsCredentials("${host}/api", TOKEN, "https://sqs.example/queue")

        then:
        credentials.accessKeyId() == "id"
        credentials.secretAccessKey() == "secret"
        credentials.sessionToken() == "session"
        credentials.expiration() == Instant.parse("2026-09-25T13:00:00Z")
        body().queueUrl == "https://sqs.example/queue"
        authHeader() == TOKEN
    }

    def "fetchBytes, without auth"() {
        given:
        route("GET", "/photos/1.jpg", 200, [1, 2, 3] as byte[])

        expect:
        RemotePhotoUtil.fetchBytes("${host}/photos/1.jpg") == [1, 2, 3] as byte[]
        !authHeader()
    }

    def "fetchBytes treats an empty 200 as a transient failure"() {
        given:
        route("GET", "/photos/1.jpg", 200, new byte[0])

        when:
        RemotePhotoUtil.fetchBytes("${host}/photos/1.jpg")

        then:
        CloudCardApiException e = thrown()
        !e.permanent
    }

    /* *** STUB SERVER *** */

    private void route(String method, String path, int status, body = null) {
        byte[] bytes = body instanceof byte[] ? body : (body?.toString()?.getBytes(StandardCharsets.UTF_8) ?: new byte[0])
        routes["${method} ${path}".toString()] = [status, bytes]
    }

    private Map last() { requests.last() }

    private def body() { new JsonSlurper().parseText(last().body) }

    private String authHeader() { last().headers.getFirst("X-Auth-Token") }

    /** The raw query, split and decoded as a server would see it. */
    private Map<String, String> queryParams() {
        String raw = last().rawQuery
        if (!raw) return [:]

        raw.split("&").collectEntries { String pair ->
            List<String> parts = pair.split("=", 2) as List
            [(decode(parts[0])): decode(parts.size() > 1 ? parts[1] : "")]
        }
    }

    private static String decode(String value) { URLDecoder.decode(value, StandardCharsets.UTF_8) }

    private void handle(HttpExchange ex) {
        requests << [rawQuery: ex.requestURI.rawQuery, headers: ex.requestHeaders, body: ex.requestBody.text]

        def (int status, byte[] bytes) = routes["${ex.requestMethod} ${ex.requestURI.path}".toString()] ?: [404, new byte[0]]

        ex.sendResponseHeaders(status, bytes.length ?: -1)
        if (bytes.length) ex.responseBody.write(bytes)
        ex.close()
    }
}