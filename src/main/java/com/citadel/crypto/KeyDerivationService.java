package com.citadel.crypto;

import java.util.concurrent.CompletableFuture;

/**
 * Contract for key derivation services.
 *
 * <p>A Key Derivation Function (KDF) transforms a human-memorable password
 * into a fixed-length cryptographic key suitable for AES-256.
 *
 * <p>Implementations must be:
 * <ul>
 *   <li><b>Slow by design</b> — to resist brute-force and dictionary attacks.</li>
 *   <li><b>Deterministic</b> — same password + salt always produces the same key.</li>
 *   <li><b>Salt-dependent</b> — different salts produce completely different keys.</li>
 * </ul>
 *
 * @author Ayush Kishan
 */
public interface KeyDerivationService {

    /**
     * Derives a cryptographic key from a password and a salt (blocking).
     *
     * <p>The caller is responsible for:
     * <ul>
     *   <li>Zeroing out {@code password} after this call (security hygiene).</li>
     *   <li>Storing the {@code salt} alongside the encrypted vault so it can be
     *       reproduced at unlock time.</li>
     * </ul>
     *
     * @param password  The user's master password as a char array (not String,
     *                  to allow zeroing from memory).
     * @param salt      A randomly generated salt (at least 16 bytes recommended).
     * @param keyLength The desired output key length in bytes (e.g., 32 for AES-256).
     * @return The derived key bytes of length {@code keyLength}.
     * @throws CryptoException if key derivation fails.
     */
    byte[] deriveKey(char[] password, byte[] salt, int keyLength);

    /**
     * Derives a cryptographic key asynchronously (non-blocking).
     *
     * <p>Offloads the expensive KDF computation to a background thread via
     * {@link CompletableFuture}, keeping the calling thread (e.g., UI thread) free.
     *
     * @param password  The user's master password as a char array.
     * @param salt      A randomly generated salt.
     * @param keyLength Desired output key length in bytes.
     * @return A {@link CompletableFuture} that resolves to the derived key bytes.
     */
    CompletableFuture<byte[]> deriveKeyAsync(char[] password, byte[] salt, int keyLength);
}
