package com.citadel.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an API token or secret key stored in the vault.
 *
 * <p>Typical use: GitHub personal access tokens, AWS secret keys,
 * Stripe API keys, OpenAI API keys, etc.
 *
 * @author Ayush Kishan
 */
public class ApiTokenCredential implements VaultItem {

    private final UUID id;
    private String label;
    private String serviceName;
    private String token;
    private String notes;
    private Instant expiresAt;        // nullable — not all tokens expire
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * Constructs a new {@code ApiTokenCredential}.
     *
     * @param label       Human-readable name (e.g., "GitHub CI Token").
     * @param serviceName The name of the service this token belongs to (e.g., "GitHub").
     * @param token       The actual API token or secret string.
     * @param notes       Optional freeform notes (may be null).
     * @param expiresAt   Optional expiry timestamp (may be null if the token never expires).
     */
    public ApiTokenCredential(String label, String serviceName, String token,
                              String notes, Instant expiresAt) {
        this.id          = UUID.randomUUID();
        this.label       = label;
        this.serviceName = serviceName;
        this.token       = token;
        this.notes       = notes;
        this.expiresAt   = expiresAt;
        this.createdAt   = Instant.now();
        this.updatedAt   = this.createdAt;
    }

    // ---- VaultItem contract ----

    @Override public UUID getId()             { return id; }
    @Override public String getLabel()        { return label; }
    @Override public CredentialType getType() { return CredentialType.API_TOKEN; }
    @Override public Instant getCreatedAt()   { return createdAt; }
    @Override public Instant getUpdatedAt()   { return updatedAt; }

    // ---- Getters ----

    public String getServiceName() { return serviceName; }
    public String getToken()       { return token; }
    public String getNotes()       { return notes; }
    public Instant getExpiresAt()  { return expiresAt; }

    /**
     * Returns {@code true} if this token has a defined expiry and that
     * expiry is in the past.
     *
     * @return {@code true} if expired.
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    // ---- Setters ----

    public void setLabel(String label)             { this.label = label; touch(); }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; touch(); }
    public void setToken(String token)             { this.token = token; touch(); }
    public void setNotes(String notes)             { this.notes = notes; touch(); }
    public void setExpiresAt(Instant expiresAt)    { this.expiresAt = expiresAt; touch(); }

    private void touch() { this.updatedAt = Instant.now(); }

    @Override
    public String toString() {
        return "ApiTokenCredential{id=" + id + ", label='" + label
                + "', service='" + serviceName + "', expired=" + isExpired() + "}";
    }
}
