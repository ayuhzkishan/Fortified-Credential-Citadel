package com.citadel.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a free-form encrypted text note stored in the vault.
 *
 * <p>Typical use: SSH key passphrases, 2FA recovery codes,
 * software license keys, PIN numbers, or any sensitive text
 * that doesn't fit the username/password or API token categories.
 *
 * @author Ayush Kishan
 */
public class SecureNoteCredential implements VaultItem {

    private final UUID id;
    private String label;
    private String content;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * Constructs a new {@code SecureNoteCredential}.
     *
     * @param label   Human-readable name (e.g., "AWS Recovery Codes").
     * @param content The sensitive text content to store encrypted.
     */
    public SecureNoteCredential(String label, String content) {
        this.id        = UUID.randomUUID();
        this.label     = label;
        this.content   = content;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // ---- VaultItem contract ----

    @Override public UUID getId()             { return id; }
    @Override public String getLabel()        { return label; }
    @Override public CredentialType getType() { return CredentialType.SECURE_NOTE; }
    @Override public Instant getCreatedAt()   { return createdAt; }
    @Override public Instant getUpdatedAt()   { return updatedAt; }

    // ---- Getters ----

    public String getContent() { return content; }

    // ---- Setters ----

    public void setLabel(String label)     { this.label = label; touch(); }
    public void setContent(String content) { this.content = content; touch(); }

    private void touch() { this.updatedAt = Instant.now(); }

    @Override
    public String toString() {
        return "SecureNoteCredential{id=" + id + ", label='" + label + "'}";
    }
}
