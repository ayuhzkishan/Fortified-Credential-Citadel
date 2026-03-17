package com.citadel.vault;

import com.citadel.crypto.AesGcmService;
import com.citadel.crypto.Argon2DerivationService;
import com.citadel.factory.CredentialFactory;
import com.citadel.model.PasswordCredential;
import com.citadel.model.VaultItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class VaultManagerTest {

    private Path tempVaultPath;
    private VaultManager vaultManager;

    @BeforeEach
    void setUp() throws IOException {
        tempVaultPath = Files.createTempFile("test-citadel-", ".citadel");
        Files.deleteIfExists(tempVaultPath); // Start fresh each test

        vaultManager = new VaultManager(
                tempVaultPath,
                new AesGcmService(),
                new Argon2DerivationService()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        vaultManager.lock();
        Files.deleteIfExists(tempVaultPath);
    }

    @Test
    void testInitAndUnlockCycle() throws Exception {
        char[] password = "MySuperSecretPassword!".toCharArray();

        // 1. Initialize
        vaultManager.initializeNewVault(password);
        assertTrue(Files.exists(tempVaultPath), "Vault file should be created");
        assertFalse(VaultSessionManager.getInstance().isLocked(), "Session should be unlocked after init");

        // 2. Add an item
        PasswordCredential c = CredentialFactory.createPassword("Test", "user", "pass", null, null);
        vaultManager.addItem(c);
        vaultManager.save();
        
        // 3. Lock
        vaultManager.lock();
        assertTrue(VaultSessionManager.getInstance().isLocked(), "Session must be locked");
        assertThrows(IllegalStateException.class, vaultManager::getAllItems, "Should fail when locked");

        // 4. Unlock with correct password
        char[] passwordAgain = "MySuperSecretPassword!".toCharArray();
        vaultManager.unlock(passwordAgain);

        assertFalse(VaultSessionManager.getInstance().isLocked());
        assertEquals(1, vaultManager.getAllItems().size());
        
        Optional<VaultItem> retrieved = vaultManager.getItem(c.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("user", ((PasswordCredential) retrieved.get()).getUsername());
    }

    @Test
    void testUnlockWithWrongPasswordFails() throws Exception {
        vaultManager.initializeNewVault("CorrectPassword!".toCharArray());
        vaultManager.lock();

        assertThrows(Exception.class, () -> vaultManager.unlock("WrongPassword!".toCharArray()));
        assertTrue(VaultSessionManager.getInstance().isLocked());
    }
}
