package com.citadel.vault;

import com.citadel.model.VaultItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles serialization of VaultItems to/from JSON.
 *
 * <p>Uses Jackson with polymorphic type handling so lists containing mixed
 * types ({@link com.citadel.model.PasswordCredential},
 * {@link com.citadel.model.ApiTokenCredential}, etc.) can be serialized
 * and deserialized accurately.
 *
 * <p>This class only deals with JSON conversion. Encryption and file I/O
 * are handled by the VaultManager.
 *
 * @author Ayush Kishan
 */
public class VaultSerializer {

    private static final Logger logger = LoggerFactory.getLogger(VaultSerializer.class);
    
    private final ObjectMapper mapper;

    public VaultSerializer() {
        this.mapper = new ObjectMapper();
        
        // Support for Java 8 Time (Instant)
        mapper.registerModule(new JavaTimeModule());

        // Let Jackson rely entirely on the @JsonTypeInfo and @JsonSubTypes annotations
        // present on the VaultItem interface. Explicitly activating default typing here
        // can sometimes conflict with annotation-based polymorphic deserialization.
    }

    /**
     * Serializes a list of VaultItems into a UTF-8 JSON byte array.
     *
     * @param items The items to serialize.
     * @return UFT-8 encoded JSON bytes.
     * @throws RuntimeException if serialization fails.
     */
    public byte[] serialize(List<VaultItem> items) {
        try {
            logger.debug("Serializing {} vault items to JSON...", items.size());
            String json = mapper.writerFor(new TypeReference<List<VaultItem>>() {})
                                .withDefaultPrettyPrinter()
                                .writeValueAsString(items);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize vault items.", e);
            throw new RuntimeException("Serialization failure.", e);
        }
    }

    /**
     * Deserializes a UTF-8 JSON byte array back into a list of VaultItems.
     *
     * @param jsonBytes The raw JSON bytes.
     * @return The deserialized list of items.
     * @throws RuntimeException if deserialization fails (e.g. malformed JSON).
     */
    public List<VaultItem> deserialize(byte[] jsonBytes) {
        if (jsonBytes == null || jsonBytes.length == 0) {
            return new ArrayList<>();
        }
        try {
            logger.debug("Deserializing vault items from JSON bytes (size: {})", jsonBytes.length);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            return mapper.readValue(json, new TypeReference<List<VaultItem>>() {});
        } catch (IOException e) {
            logger.error("Failed to deserialize vault items.", e);
            throw new RuntimeException("Deserialization failure or file corruption.", e);
        }
    }
}
