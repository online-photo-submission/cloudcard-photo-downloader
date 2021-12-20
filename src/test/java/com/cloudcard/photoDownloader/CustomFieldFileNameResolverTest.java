package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class CustomFieldFileNameResolverTest {

    CustomFieldFileNameResolver resolver = new CustomFieldFileNameResolver();
    Photo photo;

    @Before
    public void before() {
        resolver.delimiter = "_";
        resolver.include = new String[] {"John", "Mary", "Bob", "identifier"};
        photo = new Photo();
        photo.setPerson(new Person());
        photo.getPerson().setIdentifier("100");
        photo.getPerson().setCustomFields(new HashMap<>());
        photo.getPerson().setEmail("foo@bar.edu");
        Map customFields = photo.getPerson().getCustomFields();
        customFields.put("John", "bacon");
        customFields.put("Mary", "bacon1");
        customFields.put("Bob", "bacon2");
    }

    @Test
    public void testWith3CustomFields() {
        assertThat(resolver.getBaseName(photo)).isEqualTo("bacon_bacon1_bacon2_100");
    }

    @Test
    public void testWith2CustomFields() {
        resolver.include = new String[] {"John", "Mary", "identifier"};
        assertThat(resolver.getBaseName(photo)).isEqualTo("bacon_bacon1_100");
    }

    @Test
    public void testWith1CustomFieldAndNoIdentifier() {
        resolver.include = new String[] {"John"};
        assertThat(resolver.getBaseName(photo)).isEqualTo("bacon");
    }

    @Test
    public void testDelimiter() {
        resolver.delimiter = ".";
        assertThat(resolver.getBaseName(photo)).isEqualTo("bacon.bacon1.bacon2.100");
    }

    @Test
    public void testMissingCustomFields() {
        Map customFields = photo.getPerson().getCustomFields();
        customFields.put("John", "");
        customFields.put("Mary", "");
        customFields.put("Bob", "");
        try {
            resolver.getBaseName(photo);
            fail("Exception should have been thrown");
        } catch(RuntimeException exception) {
            assertThat(true).isEqualTo(true);
        }
    }

    @Test
    public void testNullCustomFields() {
        photo.getPerson().setCustomFields(new HashMap<>());
        try {
            resolver.getBaseName(photo);
            fail("Exception should have been thrown");
        } catch(DownloaderException exception) {
            assertThat(true).isEqualTo(true);
        }
    }
}
