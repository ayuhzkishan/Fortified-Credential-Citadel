package com.citadel.vault;

import com.citadel.crypto.AesGcmService;
import com.citadel.crypto.CryptoException;
import com.citadel.crypto.KeyDerivationService;
import com.citadel.event.VaultEvent;
import com.citadel.event.VaultEventBus;
import com.citadel.model.VaultItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

/**
 * The primary public API for the Fortified Credential Citadel.
 *
 * <p>Coordinates the crypto services, serializer, and session manager to provide
 * a simple interface for the UI to unlock, lock, modify, and save the vault.
 *
 * <p>In-memory credentials are held in a {@link ConcurrentHashMap} for thread-safe
 * fast lookups while the vault is unlocked. They are cleared when locked.
 *
 * @author Ayush Kishan
 */
public class VaultManager {

    private static final Logger logger = LoggerFactory.getLogger(VaultManager.class);

    private final Path vaultFilePath;
    private final KeyDerivationService kdfService;
    private final VaultSessionManager sessionManager;
    private final SqliteVaultRepository repository;
    private final VaultStore store;

    // We must store the Argon2 salt in plaintext so we can recreate the key later
    private byte[] currentSalt;

    /**
     * Constructs a new {@code VaultManager}.
     *
     * @param vaultFilePath The path to the {@code .citadel} encrypted vault file.
     * @param aesService    The AES-GCM service.
     * @param kdfService    The Argon2id service.
     */
    public VaultManager(Path vaultFilePath, AesGcmService aesService, KeyDerivationService kdfService) {
        this.vaultFilePath = vaultFilePath;
        this.kdfService = kdfService;
        this.sessionManager = VaultSessionManager.getInstance();
        this.repository = new SqliteVaultRepository(vaultFilePath, aesService);
        this.store = new VaultStore();
    }

    /**
     * Attempts to unlock the vault file with the provided master password.
     *
     * @param masterPassword The user's password (will be zeroed after derivation).
     * @throws Exception if decryption fails (wrong password or corrupted file).
     */
    public void unlock(char[] masterPassword) throws Exception {
        logger.info("Attempting to unlock vault at: {}", vaultFilePath);
        VaultEventBus.publish(VaultEvent.KEY_DERIVATION_STARTED);

        if (!Files.exists(vaultFilePath)) {
            throw new IOException("Vault file does not exist: " + vaultFilePath);
        }

        // Initialize SQLite repository
        repository.open();

        // 1. Load salt from Meta table
        this.currentSalt = repository.loadSalt();

        // 2. Derive key (slow operation)
        byte[] masterKey = null;
        try {
            masterKey = kdfService.deriveKey(masterPassword, currentSalt, 32);
            VaultEventBus.publish(VaultEvent.KEY_DERIVATION_COMPLETED);

            // 3. Decrypt and load all items into store
            repository.setMasterKey(masterKey);
            List<VaultItem> items = repository.loadAll();
            store.loadAll(items);

            // 4. Start session (stores master key in secure memory singleton)
            sessionManager.onUnlocked(masterKey);

        } catch (CryptoException e) {
            VaultEventBus.publish(VaultEvent.VAULT_ERROR);
            logger.warn("Unlock failed. Incorrect password or tampered file.");
            throw e;
        } finally {
            if (masterKey != null) {
                Arrays.fill(masterKey, (byte) 0);
            }
        }
    }

    /**
     * Initializes a brand-new, empty vault file with a new password.
     *
     * @param newPassword The new master password.
     * @throws Exception if the file cannot be written.
     */
    public void initializeNewVault(char[] newPassword) throws Exception {
        logger.info("Initializing new vault at: {}", vaultFilePath);
        
        // Ensure parent directory exists!
        Path parent = vaultFilePath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
            logger.debug("Created parent directory: {}", parent);
        }

        if (Files.exists(vaultFilePath)) {
            throw new IOException("Vault file already exists. Cannot overwrite with new initialization.");
        }

        // Initialize repository
        repository.open();

        // Generate new salt
        this.currentSalt = com.citadel.crypto.Argon2DerivationService.generateSalt();
        repository.saveSalt(currentSalt);

        // Derive key and start session immediately
        VaultEventBus.publish(VaultEvent.KEY_DERIVATION_STARTED);
        byte[] masterKey = kdfService.deriveKey(newPassword, currentSalt, 32);
        VaultEventBus.publish(VaultEvent.KEY_DERIVATION_COMPLETED);

        repository.setMasterKey(masterKey);
        sessionManager.onUnlocked(masterKey);
        Arrays.fill(masterKey, (byte) 0); 

        // Empty vault is now ready
        store.clear();
        logger.info("New vault initialized successfully.");
    }

    /**
     * Saves all in-memory changes back to the encrypted database.
     *
     * @throws Exception if the write fails.
     */
    public void save() throws Exception {
        requireUnlocked();
        logger.info("Synchronizing vault memory with disk repository...");
        sessionManager.keepAlive();

        repository.setMasterKey(sessionManager.getSessionKey());

        // For this Phase 3 version, we simply push all current in-memory items to SQLite.
        // In a more advanced version, we could track dirty flags to only update what changed.
        for (VaultItem item : store.findAll()) {
            repository.upsertItem(item);
        }

        VaultEventBus.publish(VaultEvent.VAULT_SAVED);
        logger.info("Vault sync complete. {} items secured.", store.size());
    }

    /** Locks the vault, sweeping all credentials and keys from memory. */
    public void lock() {
        sessionManager.lock();
        store.clear();
        repository.setMasterKey(null);
        if (currentSalt != null) {
            Arrays.fill(currentSalt, (byte) 0);
            currentSalt = null;
        }
        logger.info("Vault Manager cleared all in-memory data.");
    }

    // --- CRUD Operations (Delegated to Store) ---

    public void addItem(VaultItem item) {
        requireUnlocked();
        store.put(item);
        sessionManager.keepAlive();
        VaultEventBus.publish(VaultEvent.VAULT_MODIFIED);
    }

    public Optional<VaultItem> getItem(UUID id) {
        requireUnlocked();
        sessionManager.keepAlive();
        return store.findById(id);
    }

    public void updateItem(VaultItem item) {
        requireUnlocked();
        store.put(item); // Store handles upsert
        sessionManager.keepAlive();
        VaultEventBus.publish(VaultEvent.VAULT_MODIFIED);
    }

    public void deleteItem(UUID id) {
        requireUnlocked();
        if (store.remove(id)) {
            sessionManager.keepAlive();
            VaultEventBus.publish(VaultEvent.VAULT_MODIFIED);
            try {
                repository.deleteItem(id.toString());
            } catch (SQLException e) {
                logger.error("Failed to delete item from repository: {}", id, e);
            }
        }
    }

    public List<VaultItem> getAllItems() {
        requireUnlocked();
        sessionManager.keepAlive();
        return store.findAll();
    }

    /**
     * Returns the path to this vault's encrypted file on disk.
     */
    public Path getVaultPath() {
        return vaultFilePath;
    }

    /**
     * Alias for {@link #deleteItem(UUID)} used by UI layers.
     */
    public void removeItem(UUID id) {
        deleteItem(id);
    }

    private void requireUnlocked() {
        if (sessionManager.isLocked()) {
            throw new IllegalStateException("Vault is locked! Cannot perform operation.");
        }
    }
}
