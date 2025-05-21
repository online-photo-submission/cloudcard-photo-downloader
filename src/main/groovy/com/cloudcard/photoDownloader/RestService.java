package com.cloudcard.photoDownloader;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.RawResponse;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RestService {
    private static final Logger log = LoggerFactory.getLogger(RestService.class);

    public void fetchBytes(Photo photo) throws Exception {
        photo.setBytes(fetchBytes(photo.getExternalURL()));
    }

    public void fetchBytes(AdditionalPhoto additionalPhoto) throws Exception {
        additionalPhoto.setBytes(fetchBytes(additionalPhoto.getExternalURL()));
    }

    private static byte[] fetchBytes(String externalURL) throws Exception {

        HttpResponse<byte[]> response = Unirest.get(externalURL).header("accept", "image/jpeg;charset=utf-8").header("Content-Type", "image/jpeg;charset=utf-8").asObject(RawResponse::getContentAsBytes);

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + "returned from CloudCard API when retrieving photos bytes.");
            return null;
        }

        return response.getBody();
    }
//
//    /**
//     * Gets the bytes from the response body
//     *
//     * @param response
//     * @return binary from response body
//     * @throws IOException
//     */
//    private static byte[] getBytes(HttpResponse<String> response) throws IOException {
//
//        InputStream rawBody = response.getRawBody();
//        byte[] bytes = new byte[ rawBody.available() ];
//        rawBody.read(bytes);
//        return bytes;
//    }
}
