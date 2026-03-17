package com.citadel.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a username/password credential stored in the vault.
 *
 * <p>Typical use: website logins, application accounts, SSH credentials.
 *
 * @author Ayush Kishan
 */
public class PasswordCredential implements VaultItem {

    private final UUID id;
    private String label;
    private String username;
    private String password;
    private String url;
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * Constructs a new {@code PasswordCredential}.
     * The {@link UUID} and {@code createdAt} timestamp are assigned automatically.
     *
     * @param label    Human-readable name for this entry (e.g., "GitHub").
     * @param username The account username or email.
     * @param password The account password (stored encrypted in the vault file).
     * @param url      The associated website URL (may be null).
     * @param notes    Optional freeform notes (may be null).
     */
    public PasswordCredential(String label, String username, String password,
                              String url, String notes) {
        this.id        = UUID.randomUUID();
        this.label     = label;
        this.username  = username;
        this.password  = password;
        this.url       = url;
        this.notes     = notes;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // ---- VaultItem contract ----

    @Override public UUID getId()          { return id; }
    @Override public String getLabel()     { return label; }
    @Override public CredentialType getType() { return CredentialType.PASSWORD; }
    @Override public Instant getCreatedAt() { return createdAt; }
    @Override public Instant getUpdatedAt() { return updatedAt; }

    // ---- Getters ----

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getUrl()      { return url; }
    public String getNotes()    { return notes; }

    // ---- Setters (update timestamp on every mutation) ----

    public void setLabel(String label)       { this.label = label; touch(); }
    public void setUsername(String username) { this.username = username; touch(); }
    public void setPassword(String password) { this.password = password; touch(); }
    public void setUrl(String url)           { this.url = url; touch(); }
    public void setNotes(String notes)       { this.notes = notes; touch(); }

    /** Updates the {@code updatedAt} timestamp to now. */
    private void touch() { this.updatedAt = Instant.now(); }

    @Override
    public String toString() {
        return "PasswordCredential{id=" + id + ", label='" + label + "', url='" + url + "'}";
    }
}
