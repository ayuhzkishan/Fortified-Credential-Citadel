package com.citadel.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a free-form encrypted text note stored in the vault.
 *
 * <p>Typical use: recovery codes, SSH passphrases, license keys, PINs.
 */
public class SecureNoteCredential implements VaultItem {

    private final UUID    id;
    private       String  label;
    private       String  content;
    private final Instant createdAt;
    private       Instant updatedAt;

    @JsonCreator
    public SecureNoteCredential(
            @JsonProperty("id")        UUID    id,
            @JsonProperty("label")     String  label,
            @JsonProperty("content")   String  content,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt) {

        this.id        = (id        != null) ? id        : UUID.randomUUID();
        this.label     = label;
        this.content   = content;
        this.createdAt = (createdAt != null) ? createdAt : Instant.now();
        this.updatedAt = (updatedAt != null) ? updatedAt : this.createdAt;
    }

    /** Convenience constructor for new notes. */
    public SecureNoteCredential(String label, String content) {
        this(null, label, content, null, null);
    }

    // ---- VaultItem contract ----

    @Override public UUID   getId()       { return id; }
    @Override public String getLabel()    { return label; }
    @Override public CredentialType getType() { return CredentialType.SECURE_NOTE; }
    @Override public Instant getCreatedAt() { return createdAt; }
    @Override public Instant getUpdatedAt() { return updatedAt; }

    // ---- Getters ----

    public String getContent() { return content; }

    // ---- Setters ----

    public void setLabel(String label)     { this.label   = label;   touch(); }
    public void setContent(String content) { this.content = content; touch(); }

    private void touch() { this.updatedAt = Instant.now(); }

    @Override
    public String toString() {
        return "SecureNoteCredential{id=" + id + ", label='" + label + "'}";
    }
}
