package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import org.springframework.stereotype.Component

@Component
class UnirestWrapper {
    HttpResponse<String> post(String url, Map headers, String body) {
        Unirest.post(url).headers(headers).body(body).asString()
    }

    HttpResponse<String> post(String url, Map headers, byte[] body) {
        Unirest.post(url).headers(headers).body(body).asString()
    }

    HttpResponse<String> put(String url, Map headers, String body) {
        Unirest.put(url).headers(headers).body(body).asString()
    }

    HttpResponse<String> put(String url, Map headers, byte[] body) {
        Unirest.put(url).headers(headers).body(body).asString()
    }

    HttpResponse<String> patch(String url, Map headers, String body) {
        Unirest.patch(url).headers(headers).body(body).asString()
    }

    HttpResponse<String> patch(String url, Map headers, byte[] body) {
        Unirest.patch(url).headers(headers).body(body).asString()
    }

    HttpResponse<String> get(String url, Map headers) {
        Unirest.get(url).headers(headers).asString()
    }

    HttpResponse<String> delete(String url, Map headers) {
        Unirest.delete(url).headers(headers).asString()
    }
}
