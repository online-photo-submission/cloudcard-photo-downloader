package com.cloudcard.photoDownloader;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class RestService {
    private static final Logger log = LoggerFactory.getLogger(RestService.class);

    public void fetchBytes(AdditionalPhoto additionalPhoto) throws Exception {
        additionalPhoto.setBytes(fetchBytes(additionalPhoto.getExternalURL()));
    }

    public static byte[] fetchBytes(String externalURL) throws Exception {

        HttpResponse<String> response = Unirest.get(externalURL).header("accept", "image/jpeg;charset=utf-8").header("Content-Type", "image/jpeg;charset=utf-8").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + "returned from CloudCard API when retrieving photos bytes.");
            return null;
        }

        return getBytes(response);
    }

    /**
     * Gets the bytes from the response body
     *
     * @param response
     * @return binary from response body
     * @throws IOException
     */
    private static byte[] getBytes(HttpResponse<String> response) throws IOException {

        InputStream rawBody = response.getRawBody();
        byte[] bytes = new byte[ rawBody.available() ];
        rawBody.read(bytes);
        return bytes;
    }
}
