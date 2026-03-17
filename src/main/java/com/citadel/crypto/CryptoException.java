package com.citadel.crypto;

/**
 * Custom unchecked exception for all cryptographic operation failures.
 *
 * <p>Wraps low-level {@link java.security.GeneralSecurityException} and
 * other crypto-related checked exceptions into a consistent, unchecked
 * exception that callers can handle at the appropriate boundary.
 *
 * <p>Usage examples:
 * <ul>
 *   <li>AES encryption/decryption failures</li>
 *   <li>RSA key generation or signing errors</li>
 *   <li>Argon2 / PBKDF2 key derivation failures</li>
 *   <li>Cipher initialization errors</li>
 *   <li>Authentication tag mismatch (tampered ciphertext)</li>
 * </ul>
 *
 * @author Ayush Kishan
 */
public class CryptoException extends RuntimeException {

    /**
     * Constructs a new CryptoException with a descriptive message.
     *
     * @param message A clear, context-rich description of what failed.
     */
    public CryptoException(String message) {
        super(message);
    }

    /**
     * Constructs a new CryptoException wrapping a root cause.
     *
     * @param message A clear, context-rich description of what failed.
     * @param cause   The underlying exception that caused the failure.
     */
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
