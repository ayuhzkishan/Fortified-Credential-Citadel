package com.citadel.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a username/password credential stored in the vault.
 */
public class PasswordCredential implements VaultItem {

    private final UUID    id;
    private       String  label;
    private       String  username;
    private       String  password;
    private       String  url;
    private       String  notes;
    private final Instant createdAt;
    private       Instant updatedAt;

    /**
     * Jackson deserialization constructor. Also used when loading from the vault file.
     */
    @JsonCreator
    public PasswordCredential(
            @JsonProperty("id")        UUID    id,
            @JsonProperty("label")     String  label,
            @JsonProperty("username")  String  username,
            @JsonProperty("password")  String  password,
            @JsonProperty("url")       String  url,
            @JsonProperty("notes")     String  notes,
            @JsonProperty("createdAt") Instant createdAt,
            @JsonProperty("updatedAt") Instant updatedAt) {

        this.id        = (id        != null) ? id        : UUID.randomUUID();
        this.label     = label;
        this.username  = username;
        this.password  = password;
        this.url       = url;
        this.notes     = notes;
        this.createdAt = (createdAt != null) ? createdAt : Instant.now();
        this.updatedAt = (updatedAt != null) ? updatedAt : this.createdAt;
    }

    /** Convenience constructor for new (non-deserialized) credentials. */
    public PasswordCredential(String label, String username, String password,
                              String url, String notes) {
        this(null, label, username, password, url, notes, null, null);
    }

    // ---- VaultItem contract ----

    @Override public UUID   getId()        { return id; }
    @Override public String getLabel()     { return label; }
    @Override public CredentialType getType()  { return CredentialType.PASSWORD; }
    @Override public Instant getCreatedAt() { return createdAt; }
    @Override public Instant getUpdatedAt() { return updatedAt; }

    // ---- Getters ----

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getUrl()      { return url; }
    public String getNotes()    { return notes; }

    // ---- Setters ----

    public void setLabel(String label)       { this.label    = label;    touch(); }
    public void setUsername(String username) { this.username = username; touch(); }
    public void setPassword(String password) { this.password = password; touch(); }
    public void setUrl(String url)           { this.url      = url;      touch(); }
    public void setNotes(String notes)       { this.notes    = notes;    touch(); }

    private void touch() { this.updatedAt = Instant.now(); }

    @Override
    public String toString() {
        return "PasswordCredential{id=" + id + ", label='" + label + "', url='" + url + "'}";
    }
}
