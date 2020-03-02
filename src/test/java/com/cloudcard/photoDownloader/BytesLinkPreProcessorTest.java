package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BytesLinkPreProcessorTest {

    BytesLinkPreProcessor preProcessor;

    @Before
    public void setUp() {

        preProcessor = new BytesLinkPreProcessor();
    }

    @Test
    public void testProcess() {

        preProcessor.urlTemplate = "https://www.bacon.com/{publicKey}/bytes";
        testHelper("eggs", "https://www.bacon.com/eggs/bytes");
        testHelper("sausage", "https://www.bacon.com/sausage/bytes");

        preProcessor.urlTemplate = "https://www.pancakes.com/{publicKey}/biscuits/and/gravy";
        testHelper("hashbrowns", "https://www.pancakes.com/hashbrowns/biscuits/and/gravy");
        testHelper("corned-beef", "https://www.pancakes.com/corned-beef/biscuits/and/gravy");
    }

    private void testHelper(String publicKey, String expected) {

        Photo photo = new Photo();

        photo.setPublicKey(publicKey);
        photo.setLinks(new Links());
        Photo result = preProcessor.process(photo);
        assertThat(result.getExternalURL()).isEqualTo(expected);
        assertThat(preProcessor.urlTemplate).contains("{publicKey}");
    }
}