package com.citadel.event;

/**
 * Defines all events that the vault core can emit.
 *
 * <p>Used by {@link VaultEventBus} to notify subscribers of lifecycle changes.
 *
 * @author Ayush Kishan
 */
public enum VaultEvent {

    /** The vault has been successfully unlocked with the master password. */
    VAULT_UNLOCKED,

    /** The vault has been locked — session key wiped from memory. */
    VAULT_LOCKED,

    /** One or more vault items were added, updated, or deleted. */
    VAULT_MODIFIED,

    /** Argon2id key derivation has started (may take 1–3 seconds). */
    KEY_DERIVATION_STARTED,

    /** Argon2id key derivation completed successfully. */
    KEY_DERIVATION_COMPLETED,

    /** The vault was saved (re-encrypted and written to disk) successfully. */
    VAULT_SAVED,

    /**
     * Auto-lock countdown warning.
     * Emitted shortly before the vault auto-locks due to inactivity.
     */
    AUTO_LOCK_WARNING,

    /** An error occurred during a vault operation. */
    VAULT_ERROR
}
