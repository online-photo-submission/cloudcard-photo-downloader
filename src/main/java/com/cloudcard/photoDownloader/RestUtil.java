package com.cloudcard.photoDownloader;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class RestUtil {
    private static final Logger log = LoggerFactory.getLogger(RestUtil.class);

    public static byte[] fetchBytes(String externalURL) throws Exception {

        HttpResponse<String> response = Unirest.get(externalURL).header("accept", "image/jpeg;charset=utf-8").header("Content-Type", "image/jpeg;charset=utf-8").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + "returned from CloudCard API when retrieving photos bytes.");
            return null;
        }

        return getBytes(response);
    }

    private static byte[] getBytes(HttpResponse<String> response) throws IOException {

        InputStream rawBody = response.getRawBody();
        byte[] bytes = new byte[ rawBody.available() ];
        rawBody.read(bytes);
        return bytes;
    }

}
