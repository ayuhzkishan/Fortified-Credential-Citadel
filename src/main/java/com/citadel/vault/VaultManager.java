package com.citadel.vault;

import com.citadel.crypto.AesGcmService;
import com.citadel.crypto.CryptoException;
import com.citadel.crypto.KeyDerivationService;
import com.citadel.event.VaultEvent;
import com.citadel.event.VaultEventBus;
import com.citadel.model.VaultItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private final AesGcmService aesService;
    private final KeyDerivationService kdfService;
    private final VaultSerializer serializer;
    private final VaultSessionManager sessionManager;

    // In-memory credential store (only populated while vault is unlocked)
    private final Map<UUID, VaultItem> activeItems = new ConcurrentHashMap<>();

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
        this.aesService = aesService;
        this.kdfService = kdfService;
        this.serializer = new VaultSerializer();
        this.sessionManager = VaultSessionManager.getInstance();
    }

    /**
     * Attempts to unlock the vault file with the provided master password.
     *
     * @param masterPassword The user's password (will be zeroed after derivation).
     * @throws CryptoException if decryption fails (wrong password or corrupted file).
     * @throws IOException     if the vault file cannot be read.
     */
    public void unlock(char[] masterPassword) throws IOException {
        logger.info("Attempting to unlock vault at: {}", vaultFilePath);
        VaultEventBus.publish(VaultEvent.KEY_DERIVATION_STARTED);

        File file = vaultFilePath.toFile();
        if (!file.exists()) {
            throw new IOException("Vault file does not exist: " + vaultFilePath);
        }

        // The file format is: [16 bytes salt] + [12 bytes IV + ciphertext + 16 bytes tag]
        byte[] fileBytes = Files.readAllBytes(vaultFilePath);
        if (fileBytes.length < 16 + 12 + 16) {
            throw new CryptoException("Vault file is too small to be valid.");
        }

        // 1. Extract salt
        this.currentSalt = Arrays.copyOfRange(fileBytes, 0, 16);
        byte[] encryptedPayload = Arrays.copyOfRange(fileBytes, 16, fileBytes.length);

        // 2. Derive key (slow operation)
        byte[] masterKey = null;
        try {
            masterKey = kdfService.deriveKey(masterPassword, currentSalt, 32);
            VaultEventBus.publish(VaultEvent.KEY_DERIVATION_COMPLETED);

            // 3. Decrypt payload
            logger.debug("Decrypting vault payload...");
            byte[] jsonBytes = aesService.decrypt(encryptedPayload, masterKey);

            // 4. Deserialize to items
            List<VaultItem> items = serializer.deserialize(jsonBytes);

            // 5. Populate in-memory map
            activeItems.clear();
            items.forEach(item -> activeItems.put(item.getId(), item));

            // 6. Start session
            sessionManager.onUnlocked(masterKey);

            // Zero intermediate buffers
            Arrays.fill(jsonBytes, (byte) 0);

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
     * @throws IOException if the file cannot be written.
     */
    public void initializeNewVault(char[] newPassword) throws IOException {
        logger.info("Initializing new vault at: {}", vaultFilePath);
        File file = vaultFilePath.toFile();
        if (file.exists()) {
            throw new IOException("Vault file already exists. Cannot overwrite with new initialization.");
        }

        // Generate new salt
        this.currentSalt = com.citadel.crypto.Argon2DerivationService.generateSalt();
        this.activeItems.clear();

        // Derive key and start session immediately
        VaultEventBus.publish(VaultEvent.KEY_DERIVATION_STARTED);
        byte[] masterKey = kdfService.deriveKey(newPassword, currentSalt, 32);
        VaultEventBus.publish(VaultEvent.KEY_DERIVATION_COMPLETED);

        sessionManager.onUnlocked(masterKey);
        Arrays.fill(masterKey, (byte) 0); // session manager has its copy

        // Save the empty vault to disk
        save();
    }

    /**
     * Saves the current in-memory credentials back to the encrypted vault file.
     *
     * @throws IOException if the disk write fails.
     */
    public void save() throws IOException {
        requireUnlocked();
        logger.info("Saving vault to disk...");
        sessionManager.keepAlive();

        byte[] sessionKey = sessionManager.getSessionKey();
        byte[] jsonBytes = null;
        byte[] encryptedPayload = null;

        try {
            // 1. Serialize map to JSON bytes
            jsonBytes = serializer.serialize(new ArrayList<>(activeItems.values()));

            // 2. Encrypt JSON bytes
            encryptedPayload = aesService.encrypt(jsonBytes, sessionKey);

            // 3. Combine [Salt] + [EncryptedPayload]
            byte[] fileBytes = new byte[currentSalt.length + encryptedPayload.length];
            System.arraycopy(currentSalt, 0, fileBytes, 0, currentSalt.length);
            System.arraycopy(encryptedPayload, 0, fileBytes, currentSalt.length, encryptedPayload.length);

            // 4. Write to disk securely (write to temp file then atomic move is best practice, 
            // but direct write is used here for brevity; can be hardened later).
            Files.write(vaultFilePath, fileBytes);

            VaultEventBus.publish(VaultEvent.VAULT_SAVED);
            logger.info("Vault saved successfully. {} items secured.", activeItems.size());

        } finally {
            if (jsonBytes != null) Arrays.fill(jsonBytes, (byte) 0);
            if (encryptedPayload != null) Arrays.fill(encryptedPayload, (byte) 0);
        }
    }

    /** Locks the vault, sweeping all credentials and keys from memory. */
    public void lock() {
        sessionManager.lock();
        activeItems.clear();
        if (currentSalt != null) {
            Arrays.fill(currentSalt, (byte) 0);
            currentSalt = null;
        }
        logger.info("Vault Manager cleared all in-memory data.");
    }

    // --- CRUD Operations ---

    public void addItem(VaultItem item) {
        requireUnlocked();
        activeItems.put(item.getId(), item);
        sessionManager.keepAlive();
        VaultEventBus.publish(VaultEvent.VAULT_MODIFIED);
    }

    public Optional<VaultItem> getItem(UUID id) {
        requireUnlocked();
        sessionManager.keepAlive();
        return Optional.ofNullable(activeItems.get(id));
    }

    public void updateItem(VaultItem item) {
        requireUnlocked();
        if (!activeItems.containsKey(item.getId())) {
            throw new IllegalArgumentException("Item does not exist in vault: " + item.getId());
        }
        activeItems.put(item.getId(), item);
        sessionManager.keepAlive();
        VaultEventBus.publish(VaultEvent.VAULT_MODIFIED);
    }

    public void deleteItem(UUID id) {
        requireUnlocked();
        if (activeItems.remove(id) != null) {
            sessionManager.keepAlive();
            VaultEventBus.publish(VaultEvent.VAULT_MODIFIED);
        }
    }

    public List<VaultItem> getAllItems() {
        requireUnlocked();
        sessionManager.keepAlive();
        return new ArrayList<>(activeItems.values());
    }

    /**
     * Fails fast if a UI or service tries to access credentials while locked.
     */
    private void requireUnlocked() {
        if (sessionManager.isLocked()) {
            throw new IllegalStateException("Vault is locked! Cannot perform operation.");
        }
    }
}
