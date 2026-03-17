package com.citadel.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all credential types stored in the vault.
 *
 * <p>Every item stored in the vault must have a unique identifier,
 * a human-readable label, and timestamps for auditing purposes.
 *
 * <p>Implementations: {@link PasswordCredential}, {@link ApiTokenCredential},
 * {@link SecureNoteCredential}.
 *
 * @author Ayush Kishan
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = PasswordCredential.class, name = "password"),
        @JsonSubTypes.Type(value = ApiTokenCredential.class, name = "api_token"),
        @JsonSubTypes.Type(value = SecureNoteCredential.class, name = "secure_note")
})
public interface VaultItem {

    /**
     * Returns the unique identifier for this vault item.
     * Generated automatically at creation time via {@link UUID#randomUUID()}.
     *
     * @return The item's UUID.
     */
    UUID getId();

    /**
     * Returns the human-readable label for this item (e.g., "GitHub Login").
     *
     * @return The item label.
     */
    String getLabel();

    /**
     * Returns the type of this credential item.
     *
     * @return A {@link CredentialType} enum value.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    CredentialType getType();

    /**
     * Returns the timestamp when this item was first created.
     *
     * @return Creation timestamp as {@link Instant}.
     */
    Instant getCreatedAt();

    /**
     * Returns the timestamp of the most recent modification to this item.
     *
     * @return Last-updated timestamp as {@link Instant}.
     */
    Instant getUpdatedAt();

    /**
     * Enum representing all supported credential types.
     */
    enum CredentialType {
        /** A username/password credential (e.g., a website login). */
        PASSWORD,
        /** An API token or secret key (e.g., GitHub token, AWS secret). */
        API_TOKEN,
        /** A free-form encrypted text note. */
        SECURE_NOTE
    }
}
