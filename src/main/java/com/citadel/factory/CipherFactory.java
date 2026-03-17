package com.citadel.factory;

import com.citadel.crypto.CryptoEngineManager;
import com.citadel.crypto.CryptoException;

import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Factory for creating consistently configured {@link Cipher} instances.
 *
 * <p>Centralizes cipher construction so that algorithm names, modes, padding,
 * and provider selection are defined in exactly one place. All crypto services
 * ({@code AesGcmService}, {@code RsaService}) obtain their {@link Cipher}
 * instances through this factory — no hardcoded strings scattered across classes.
 *
 * <p><b>Supported ciphers:</b>
 * <ul>
 *   <li>{@code AES/GCM/NoPadding} — authenticated symmetric encryption</li>
 *   <li>{@code RSA/ECB/OAEPWithSHA-256AndMGF1Padding} — asymmetric encryption</li>
 * </ul>
 *
 * @author Ayush Kishan
 */
public class CipherFactory {

    // Cipher transformation strings
    public static final String AES_GCM_TRANSFORMATION  = "AES/GCM/NoPadding";
    public static final String RSA_TRANSFORMATION      = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    // AES-GCM recommended parameters
    public static final int AES_KEY_SIZE_BITS  = 256;
    public static final int AES_GCM_IV_BYTES   = 12;   // 96-bit IV (NIST recommended for GCM)
    public static final int AES_GCM_TAG_BITS   = 128;  // 128-bit authentication tag

    // RSA key size
    public static final int RSA_KEY_SIZE_BITS  = 4096;

    /** Utility class — no instantiation. */
    private CipherFactory() {}

    /**
     * Creates a new {@link Cipher} instance configured for AES-256-GCM.
     *
     * <p>Uses the Bouncy Castle provider registered by {@link CryptoEngineManager}.
     *
     * @return A fresh, uninitialised {@code Cipher} for AES/GCM/NoPadding.
     * @throws CryptoException if the algorithm or provider is unavailable.
     */
    public static Cipher forAesGcm() {
        return getCipher(AES_GCM_TRANSFORMATION);
    }

    /**
     * Creates a new {@link Cipher} instance configured for RSA-OAEP.
     *
     * <p>Uses OAEP padding with SHA-256 and MGF1 — the modern, secure alternative
     * to the deprecated PKCS#1 v1.5 padding.
     *
     * @return A fresh, uninitialised {@code Cipher} for RSA/ECB/OAEPWithSHA-256AndMGF1Padding.
     * @throws CryptoException if the algorithm or provider is unavailable.
     */
    public static Cipher forRsa() {
        return getCipher(RSA_TRANSFORMATION);
    }

    /**
     * Internal helper — obtains a {@link Cipher} from the Bouncy Castle provider.
     *
     * @param transformation The full JCE transformation string.
     * @return An uninitialised {@link Cipher} instance.
     * @throws CryptoException if the transformation or provider is not available.
     */
    private static Cipher getCipher(String transformation) {
        // Ensure CryptoEngineManager has registered Bouncy Castle
        CryptoEngineManager.getInstance();

        try {
            return Cipher.getInstance(transformation, CryptoEngineManager.BC_PROVIDER);
        } catch (NoSuchAlgorithmException | javax.crypto.NoSuchPaddingException e) {
            throw new CryptoException(
                    "Cipher algorithm or padding not available: " + transformation, e);
        } catch (NoSuchProviderException e) {
            throw new CryptoException(
                    "Bouncy Castle provider not found. Ensure CryptoEngineManager is initialised.", e);
        }
    }
}
