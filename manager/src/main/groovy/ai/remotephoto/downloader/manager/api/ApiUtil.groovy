package ai.remotephoto.downloader.manager.api

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class ApiUtil {

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    AuthenticationToken authenticate(String persistentAccessToken, String apiUrl) {

        Map requestBody = [
            persistentAccessToken: persistentAccessToken
        ]

//        TODO: Paramaterize a lot of this
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("$apiUrl/authentication-tokens"))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                JsonOutput.toJson(requestBody)
            ))
            .build()

        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        )

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                "Authentication failed (${response.statusCode()}): ${response.body()}"
            )
        }

        Map json = new JsonSlurper().parseText(response.body()) as Map

        return new AuthenticationToken(
            id: json.id as Long,
            tokenValue: json.tokenValue?.toString(),
            username: json.username?.toString(),
            expirationDate: json.expirationDate?.toString(),
            rawResponse: response.body()
        )
    }
}

class AuthenticationToken {
    Long id
    String tokenValue
    String username
    String expirationDate
    String rawResponse
}
