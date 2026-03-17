package com.citadel.crypto;

import com.citadel.factory.CipherFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmServiceTest {

    private AesGcmService aesService;
    private byte[] validKey;

    @BeforeEach
    void setUp() {
        // CryptoEngineManager is invoked implicitly inside AesGcmService -> CipherFactory
        aesService = new AesGcmService();
        validKey = new byte[32]; // 256 bits (all zeros for test determinism)
    }

    @Test
    void testEncryptDecryptSuccess() {
        String originalText = "SuperSecretPassword123!";
        byte[] plaintext = originalText.getBytes(StandardCharsets.UTF_8);

        // Encrypt
        byte[] ciphertext = aesService.encrypt(plaintext, validKey);
        
        // Assert output structure is larger than just IV + Auth Tag
        assertTrue(ciphertext.length > CipherFactory.AES_GCM_IV_BYTES + (CipherFactory.AES_GCM_TAG_BITS / 8));

        // Decrypt
        byte[] decryptedBytes = aesService.decrypt(ciphertext, validKey);
        String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);

        assertEquals(originalText, decryptedText);
    }

    @Test
    void testTamperedCiphertextThrowsException() {
        byte[] plaintext = "Data to tinker with".getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = aesService.encrypt(plaintext, validKey);

        // Tamper with the last byte (part of the GCM Auth Tag usually)
        ciphertext[ciphertext.length - 1] ^= 0x01;

        CryptoException exception = assertThrows(CryptoException.class, () ->
            aesService.decrypt(ciphertext, validKey)
        );
        assertTrue(exception.getMessage().contains("tampered"), "Message should mention tampering");
    }

    @Test
    void testInvalidKeyLengthThrowsIllegalArgument() {
        byte[] shortKey = new byte[16]; // 128 bit key

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            aesService.encrypt("test".getBytes(), shortKey)
        );
        assertTrue(exception.getMessage().contains("32-byte"));
    }
}
