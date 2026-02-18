package com.cloudcard.photoDownloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BytesLinkPreProcessorTest {

    BytesLinkPreProcessor preProcessor;

    @BeforeEach
    public void setUp() {

        preProcessor = new BytesLinkPreProcessor();
        preProcessor.urlTemplate = "https://www.bacon.com/{publicKey}/bytes";
        preProcessor.additionalPhotoUrlTemplate = "https://www.bacon.com/additional-photos/{publicKey}/bytes";
    }

    @Test
    public void testProcess() {

        testHelper("eggs", "https://www.bacon.com/eggs/bytes");
        testHelper("sausage", "https://www.bacon.com/sausage/bytes");

        preProcessor.urlTemplate = "https://www.pancakes.com/{publicKey}/biscuits/and/gravy";
        testHelper("hashbrowns", "https://www.pancakes.com/hashbrowns/biscuits/and/gravy");
        testHelper("corned-beef", "https://www.pancakes.com/corned-beef/biscuits/and/gravy");
    }

    @Test
    public void testProcces_WithAdditionalPhoto() {

        AdditionalPhoto additionalPhoto = new AdditionalPhoto();
        additionalPhoto.setPublicKey("pancakes");
        Photo result = testHelper("eggs", "https://www.bacon.com/eggs/bytes", additionalPhoto);
        assertThat(result.getPerson().getAdditionalPhotos().get(0).getExternalURL()).isEqualTo("https://www.bacon.com/additional-photos/pancakes/bytes");
    }

    @Test
    public void testProcces_WithAdditionalPhoto_AndDifferentTemplates() {

        //setup
        AdditionalPhoto additionalPhoto = new AdditionalPhoto();
        additionalPhoto.setPublicKey("syrup");

        // test w/ a different template
        preProcessor.additionalPhotoUrlTemplate = "https://www.biscuits.com/additional-photos/{publicKey}/bytes";
        Photo result = testHelper("eggs", "https://www.bacon.com/eggs/bytes", additionalPhoto);
        assertThat(result.getPerson().getAdditionalPhotos().get(0).getExternalURL()).isEqualTo("https://www.biscuits.com/additional-photos/syrup/bytes");

        String unchangedUrl = additionalPhoto.getExternalURL();

        // test w/ null template
        preProcessor.additionalPhotoUrlTemplate = null;
        result = testHelper("eggs", "https://www.bacon.com/eggs/bytes", additionalPhoto);
        assertThat(result.getPerson().getAdditionalPhotos().get(0).getExternalURL()).isEqualTo(unchangedUrl);

        // test w/ template that doesn't contain the public key
        preProcessor.additionalPhotoUrlTemplate = "hashbrowns";
        result = testHelper("eggs", "https://www.bacon.com/eggs/bytes", additionalPhoto);
        assertThat(result.getPerson().getAdditionalPhotos().get(0).getExternalURL()).isEqualTo(unchangedUrl);
    }

    @Test
    public void testProcces_WithMultipleAdditionalPhotos() {

        List<AdditionalPhoto> additionalPhotos = new ArrayList<>();

        additionalPhotos.add(new AdditionalPhoto());
        additionalPhotos.get(0).setPublicKey("pancakes");

        additionalPhotos.add(new AdditionalPhoto());
        additionalPhotos.get(1).setPublicKey("syrup");


        Photo result = testHelper("eggs", "https://www.bacon.com/eggs/bytes", additionalPhotos);
        assertThat(result.getPerson().getAdditionalPhotos().get(0).getExternalURL()).isEqualTo("https://www.bacon.com/additional-photos/pancakes/bytes");
        assertThat(result.getPerson().getAdditionalPhotos().get(1).getExternalURL()).isEqualTo("https://www.bacon.com/additional-photos/syrup/bytes");
    }

    /* *** PRIVATE HELPERS *** */

    private void testHelper(String publicKey, String expected) {

        Photo photo = new Photo();
        photo.setPerson(new Person());

        photo.setPublicKey(publicKey);
        Photo result = preProcessor.process(photo);
        assertThat(result.getExternalURL()).isEqualTo(expected);
        assertThat(preProcessor.urlTemplate).contains("{publicKey}");
    }

    private Photo testHelper(String publicKey, String expected, AdditionalPhoto additionalPhoto) {
        List<AdditionalPhoto> additionalPhotos = new ArrayList<>();
        additionalPhotos.add(additionalPhoto);
        return testHelper(publicKey, expected, additionalPhotos);
    }

    private Photo testHelper(String publicKey, String expected, List<AdditionalPhoto> additionalPhotos) {

        Photo photo = new Photo();

        //add person
        photo.setPerson(new Person());
        photo.getPerson().setAdditionalPhotos(additionalPhotos);

        photo.setPublicKey(publicKey);
        Photo result = preProcessor.process(photo);
        assertThat(result.getExternalURL()).isEqualTo(expected);
        assertThat(preProcessor.urlTemplate).contains("{publicKey}");

        return result;
    }
}