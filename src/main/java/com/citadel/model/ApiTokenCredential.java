package com.citadel.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an API token or secret key stored in the vault.
 *
 * <p>Typical use: GitHub tokens, AWS secret keys, Stripe API keys, etc.
 */
public class ApiTokenCredential implements VaultItem {

    private final UUID    id;
    private       String  label;
    private       String  serviceName;
    private       String  token;
    private       String  notes;
    private       Instant expiresAt;
    private final Instant createdAt;
    private       Instant updatedAt;

    @JsonCreator
    public ApiTokenCredential(
            @JsonProperty("id")          UUID    id,
            @JsonProperty("label")       String  label,
            @JsonProperty("serviceName") String  serviceName,
            @JsonProperty("token")       String  token,
            @JsonProperty("notes")       String  notes,
            @JsonProperty("expiresAt")   Instant expiresAt,
            @JsonProperty("createdAt")   Instant createdAt,
            @JsonProperty("updatedAt")   Instant updatedAt) {

        this.id          = (id        != null) ? id        : UUID.randomUUID();
        this.label       = label;
        this.serviceName = serviceName;
        this.token       = token;
        this.notes       = notes;
        this.expiresAt   = expiresAt;
        this.createdAt   = (createdAt != null) ? createdAt : Instant.now();
        this.updatedAt   = (updatedAt != null) ? updatedAt : this.createdAt;
    }

    /** Convenience constructor for new credentials. */
    public ApiTokenCredential(String label, String serviceName, String token,
                              String notes, Instant expiresAt) {
        this(null, label, serviceName, token, notes, expiresAt, null, null);
    }

    // ---- VaultItem contract ----

    @Override public UUID   getId()       { return id; }
    @Override public String getLabel()    { return label; }
    @Override public CredentialType getType() { return CredentialType.API_TOKEN; }
    @Override public Instant getCreatedAt() { return createdAt; }
    @Override public Instant getUpdatedAt() { return updatedAt; }

    // ---- Getters ----

    public String  getServiceName() { return serviceName; }
    public String  getToken()       { return token; }
    public String  getNotes()       { return notes; }
    public Instant getExpiresAt()   { return expiresAt; }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    // ---- Setters ----

    public void setLabel(String label)             { this.label       = label;       touch(); }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; touch(); }
    public void setToken(String token)             { this.token       = token;       touch(); }
    public void setNotes(String notes)             { this.notes       = notes;       touch(); }
    public void setExpiresAt(Instant expiresAt)    { this.expiresAt   = expiresAt;   touch(); }

    private void touch() { this.updatedAt = Instant.now(); }

    @Override
    public String toString() {
        return "ApiTokenCredential{id=" + id + ", label='" + label
                + "', service='" + serviceName + "', expired=" + isExpired() + "}";
    }
}
