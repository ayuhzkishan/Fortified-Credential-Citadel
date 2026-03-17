package com.citadel.crypto;

import com.citadel.factory.CipherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Provides RSA-4096 asymmetric encryption, decryption, signing, and verification.
 *
 * <p><b>Key uses in the Citadel vault:</b>
 * <ul>
 *   <li><b>Encryption</b>: Encrypting the AES master key under a public key for
 *       secure vault export or multi-device scenarios (future).</li>
 *   <li><b>Signing</b>: Digitally signing vault snapshots so the integrity and
 *       authorship of exported data can be verified.</li>
 * </ul>
 *
 * <p><b>Why OAEP padding?</b><br>
 * PKCS#1 v1.5 padding is vulnerable to padding oracle attacks (Bleichenbacher's attack).
 * OAEP (Optimal Asymmetric Encryption Padding) with SHA-256 is the modern,
 * secure replacement recommended by NIST and OWASP.
 *
 * @author Ayush Kishan
 */
public class RsaService {

    private static final Logger logger = LoggerFactory.getLogger(RsaService.class);

    private static final String RSA_ALGORITHM      = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /**
     * Generates a new RSA-4096 key pair using the Bouncy Castle provider.
     *
     * <p>This is an expensive operation (~1–2 seconds). Consider calling this
     * on a background thread when used in a UI context.
     *
     * @return A freshly generated {@link KeyPair} with a 4096-bit RSA key.
     * @throws CryptoException if key generation fails.
     */
    public KeyPair generateKeyPair() {
        try {
            logger.info("Generating RSA-{} key pair...", CipherFactory.RSA_KEY_SIZE_BITS);
            CryptoEngineManager.getInstance(); // ensure BC is registered

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    RSA_ALGORITHM, CryptoEngineManager.BC_PROVIDER);
            keyPairGenerator.initialize(CipherFactory.RSA_KEY_SIZE_BITS, new SecureRandom());

            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            logger.info("RSA-{} key pair generated successfully.", CipherFactory.RSA_KEY_SIZE_BITS);
            return keyPair;

        } catch (Exception e) {
            throw new CryptoException("RSA key pair generation failed.", e);
        }
    }

    /**
     * Encrypts data with an RSA public key using OAEP/SHA-256 padding.
     *
     * <p>RSA is not suitable for encrypting large amounts of data directly.
     * Use it to encrypt small payloads like AES keys (hybrid encryption model).
     *
     * @param plaintext The data to encrypt (e.g., an AES-256 key — 32 bytes).
     * @param publicKey The RSA public key of the intended recipient.
     * @return The RSA-encrypted ciphertext.
     * @throws CryptoException if encryption fails.
     */
    public byte[] encrypt(byte[] plaintext, PublicKey publicKey) {
        try {
            Cipher cipher = CipherFactory.forRsa();
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);
            byte[] ciphertext = cipher.doFinal(plaintext);
            logger.debug("RSA encryption succeeded. Output: {} bytes", ciphertext.length);
            return ciphertext;

        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("RSA encryption failed.", e);
        }
    }

    /**
     * Decrypts RSA-encrypted data with the corresponding private key.
     *
     * @param ciphertext The RSA-encrypted bytes (from {@link #encrypt}).
     * @param privateKey The RSA private key matching the public key used to encrypt.
     * @return The original plaintext bytes.
     * @throws CryptoException if decryption fails or the key is incorrect.
     */
    public byte[] decrypt(byte[] ciphertext, PrivateKey privateKey) {
        try {
            Cipher cipher = CipherFactory.forRsa();
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
            byte[] plaintext = cipher.doFinal(ciphertext);
            logger.debug("RSA decryption succeeded. Plaintext: {} bytes", plaintext.length);
            return plaintext;

        } catch (CryptoException e) {
            throw e;
        } catch (Exception e) {
            throw new CryptoException("RSA decryption failed — wrong key or corrupted ciphertext.", e);
        }
    }

    /**
     * Signs data using a private key, producing a SHA-256 with RSA digital signature.
     *
     * <p>The signature can be verified later with {@link #verify} to confirm both
     * the integrity of the data and that it was signed by the holder of the private key.
     *
     * @param data       The data to sign (e.g., a vault export payload).
     * @param privateKey The RSA private key to sign with.
     * @return The digital signature bytes.
     * @throws CryptoException if signing fails.
     */
    public byte[] sign(byte[] data, PrivateKey privateKey) {
        try {
            Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM, CryptoEngineManager.BC_PROVIDER);
            signer.initSign(privateKey, new SecureRandom());
            signer.update(data);
            byte[] signature = signer.sign();
            logger.debug("RSA signature generated. Length: {} bytes", signature.length);
            return signature;

        } catch (Exception e) {
            throw new CryptoException("RSA signing failed.", e);
        }
    }

    /**
     * Verifies a digital signature against data using the corresponding public key.
     *
     * @param data      The original data that was signed.
     * @param signature The signature bytes from {@link #sign}.
     * @param publicKey The RSA public key matching the private key that signed.
     * @return {@code true} if the signature is valid; {@code false} if it does not match.
     * @throws CryptoException if verification cannot be performed (e.g., corrupt inputs).
     */
    public boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
        try {
            Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM, CryptoEngineManager.BC_PROVIDER);
            verifier.initVerify(publicKey);
            verifier.update(data);
            boolean valid = verifier.verify(signature);
            logger.debug("RSA signature verification result: {}", valid ? "VALID" : "INVALID");
            return valid;

        } catch (Exception e) {
            throw new CryptoException("RSA signature verification failed.", e);
        }
    }
}
