package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.type.CollectionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdditionalPhotosDeserializer extends JsonDeserializer<List<AdditionalPhoto>> {

    @Override
    public List<AdditionalPhoto> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            // Normal array deserialization
            CollectionType type = ctxt.getTypeFactory().constructCollectionType(List.class, AdditionalPhoto.class);
            return ctxt.readValue(p, type);
        } else if (p.currentToken() == JsonToken.START_OBJECT) {
            // If it's an object instead of array, skip it and return empty list
            p.skipChildren();
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }
}
