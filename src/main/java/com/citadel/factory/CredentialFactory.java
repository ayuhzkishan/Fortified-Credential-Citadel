package com.citadel.factory;

import com.citadel.model.*;

import java.time.Instant;

/**
 * Factory for creating {@link VaultItem} instances.
 *
 * <p>Centralises the creation of all credential types. Instead of calling
 * constructors directly, callers use the typed factory methods here,
 * which also enforces validation and consistent defaults.
 *
 * @author Ayush Kishan
 */
public class CredentialFactory {

    /** Utility class — no instantiation. */
    private CredentialFactory() {}

    /**
     * Creates a new {@link PasswordCredential}.
     *
     * @param label    Entry name (e.g., "GitHub").
     * @param username Account username or email.
     * @param password Account password.
     * @param url      Associated URL (nullable).
     * @param notes    Optional notes (nullable).
     * @return A new {@link PasswordCredential} instance.
     * @throws IllegalArgumentException if label, username, or password is blank.
     */
    public static PasswordCredential createPassword(String label, String username,
                                                    String password, String url, String notes) {
        requireNonBlank(label, "label");
        requireNonBlank(username, "username");
        requireNonBlank(password, "password");
        return new PasswordCredential(label, username, password, url, notes);
    }

    /**
     * Creates a new {@link ApiTokenCredential}.
     *
     * @param label       Entry name (e.g., "GitHub CI Token").
     * @param serviceName Service the token belongs to (e.g., "GitHub").
     * @param token       The actual API token string.
     * @param notes       Optional notes (nullable).
     * @param expiresAt   Optional expiry timestamp (nullable if token doesn't expire).
     * @return A new {@link ApiTokenCredential} instance.
     * @throws IllegalArgumentException if label, serviceName, or token is blank.
     */
    public static ApiTokenCredential createApiToken(String label, String serviceName,
                                                    String token, String notes, Instant expiresAt) {
        requireNonBlank(label, "label");
        requireNonBlank(serviceName, "serviceName");
        requireNonBlank(token, "token");
        return new ApiTokenCredential(label, serviceName, token, notes, expiresAt);
    }

    /**
     * Creates a new {@link SecureNoteCredential}.
     *
     * @param label   Entry name (e.g., "AWS Recovery Codes").
     * @param content The sensitive text to store.
     * @return A new {@link SecureNoteCredential} instance.
     * @throws IllegalArgumentException if label or content is blank.
     */
    public static SecureNoteCredential createSecureNote(String label, String content) {
        requireNonBlank(label, "label");
        requireNonBlank(content, "content");
        return new SecureNoteCredential(label, content);
    }

    /**
     * Validates that a string field is non-null and non-blank.
     *
     * @param value     The value to check.
     * @param fieldName Name of the field (for the error message).
     * @throws IllegalArgumentException if {@code value} is null or blank.
     */
    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' must not be null or blank.");
        }
    }
}
