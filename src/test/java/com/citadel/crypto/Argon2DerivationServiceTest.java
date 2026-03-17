package com.citadel.crypto;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class Argon2DerivationServiceTest {

    private final Argon2DerivationService kdfService = new Argon2DerivationService();

    @Test
    void testDeterministicDerivation() {
        char[] password = "MasterPassword!99".toCharArray();
        byte[] salt = Argon2DerivationService.generateSalt();

        byte[] key1 = kdfService.deriveKey(password, salt, 32);
        
        // password array is zeroed by the first call, we must supply it anew:
        char[] passwordAgain = "MasterPassword!99".toCharArray();
        byte[] key2 = kdfService.deriveKey(passwordAgain, salt, 32);

        assertArrayEquals(key1, key2, "Same password and salt should yield identical keys");
    }

    @Test
    void testAsyncDerivation() throws ExecutionException, InterruptedException {
        char[] password = "AsyncPassword2026".toCharArray();
        byte[] salt = Argon2DerivationService.generateSalt();

        CompletableFuture<byte[]> future = kdfService.deriveKeyAsync(password, salt, 32);
        byte[] key = future.get(); // block test thread just to retrieve result

        assertEquals(32, key.length);
        
        // Assert the password buffer was zeroed
        for (char c : password) {
            assertEquals('\0', c, "Password char array should be scrubbed of memory");
        }
    }
}
