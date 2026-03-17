package com.citadel.crypto;

import com.citadel.factory.CipherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Provides AES-256-GCM authenticated encryption and decryption.
 *
 * <p><b>Why AES-256-GCM?</b><br>
 * GCM (Galois/Counter Mode) is an authenticated encryption mode — it both
 * encrypts the data AND produces a 128-bit authentication tag. If even one
 * byte of the ciphertext is modified, decryption throws an exception.
 * This means no separate HMAC is needed. GCM is also parallelisable and
 * extremely fast on modern hardware.
 *
 * <p><b>Output format of {@link #encrypt}:</b>
 * <pre>
 *   [ 12 bytes IV ] [ N bytes ciphertext + 16 bytes GCM auth tag ]
 * </pre>
 * The IV is prepended to the ciphertext so {@link #decrypt} can extract
 * it without needing to store it separately.
 *
 * @author Ayush Kishan
 */
public class AesGcmService {

    private static final Logger logger = LoggerFactory.getLogger(AesGcmService.class);

    private static final String ALGORITHM = "AES";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Encrypts {@code plaintext} using AES-256-GCM with the supplied key.
     *
     * <p>A fresh 12-byte IV is generated per call using {@link SecureRandom},
     * ensuring identical plaintexts always produce different ciphertexts.
     *
     * @param plaintext The raw bytes to encrypt (must not be null or empty).
     * @param keyBytes  The 32-byte (256-bit) AES key.
     * @return A byte array in the format: {@code [IV (12 bytes)] + [ciphertext + auth tag]}.
     * @throws CryptoException   if encryption fails for any reason.
     * @throws IllegalArgumentException if key is not exactly 32 bytes.
     */
    public byte[] encrypt(byte[] plaintext, byte[] keyBytes) {
        validateKey(keyBytes);
        if (plaintext == null || plaintext.length == 0) {
            throw new CryptoException("Plaintext must not be null or empty.");
        }

        try {
            // Generate a fresh random IV for every encryption call
            byte[] iv = new byte[CipherFactory.AES_GCM_IV_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = CipherFactory.forAesGcm();
            GCMParameterSpec parameterSpec = new GCMParameterSpec(CipherFactory.AES_GCM_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, buildSecretKey(keyBytes), parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext);

            // Prepend IV so decrypt can extract it: [IV | ciphertext+tag]
            byte[] output = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, output, 0, iv.length);
            System.arraycopy(ciphertext, 0, output, iv.length, ciphertext.length);

            logger.debug("AES-256-GCM encryption succeeded. Output size: {} bytes", output.length);
            return output;

        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("AES-256-GCM encryption failed.", e);
        }
    }

    /**
     * Decrypts an AES-256-GCM encrypted byte array produced by {@link #encrypt}.
     *
     * <p>Extracts the IV from the first 12 bytes, then decrypts and verifies
     * the authentication tag. If the ciphertext has been tampered with,
     * a {@link CryptoException} is thrown.
     *
     * @param encryptedData The full encrypted payload: {@code [IV (12 bytes)] + [ciphertext + auth tag]}.
     * @param keyBytes      The 32-byte (256-bit) AES key, must match what was used in {@link #encrypt}.
     * @return The original plaintext bytes.
     * @throws CryptoException   if decryption or authentication tag verification fails.
     * @throws IllegalArgumentException if key is not exactly 32 bytes.
     */
    public byte[] decrypt(byte[] encryptedData, byte[] keyBytes) {
        validateKey(keyBytes);
        if (encryptedData == null || encryptedData.length <= CipherFactory.AES_GCM_IV_BYTES) {
            throw new CryptoException("Encrypted data is null or too short to contain a valid IV.");
        }

        try {
            // Extract the IV from the first 12 bytes
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, CipherFactory.AES_GCM_IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(encryptedData, CipherFactory.AES_GCM_IV_BYTES, encryptedData.length);

            Cipher cipher = CipherFactory.forAesGcm();
            GCMParameterSpec parameterSpec = new GCMParameterSpec(CipherFactory.AES_GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, buildSecretKey(keyBytes), parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);

            logger.debug("AES-256-GCM decryption succeeded. Plaintext size: {} bytes", plaintext.length);
            return plaintext;

        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException(
                    "AES-256-GCM decryption failed — ciphertext may be corrupted or tampered.", e);
        }
    }

    /**
     * Builds a {@link SecretKey} from a raw 32-byte key material array.
     */
    private SecretKey buildSecretKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Validates that the supplied key is exactly 32 bytes (256 bits).
     *
     * @throws IllegalArgumentException if key length is not 32 bytes.
     */
    private void validateKey(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "AES-256 requires a 32-byte key. Provided: "
                    + (keyBytes == null ? "null" : keyBytes.length + " bytes"));
        }
    }
}
