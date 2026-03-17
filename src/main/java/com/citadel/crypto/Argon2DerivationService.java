package com.citadel.crypto;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Argon2id implementation of {@link KeyDerivationService}.
 *
 * <p><b>Why Argon2id?</b><br>
 * Argon2id is the winner of the Password Hashing Competition (2015) and the
 * current OWASP recommendation. It is <em>intentionally slow and memory-hard</em>:
 * <ul>
 *   <li>Requires large amounts of RAM per derivation → GPU parallelism attacks are
 *       enormously expensive.</li>
 *   <li>Sequential memory accesses → ASIC/FPGA resistance.</li>
 *   <li>Configurable: iterations, memory, and parallelism can be tuned to hardware.</li>
 * </ul>
 *
 * <p><b>OWASP-recommended parameters used:</b>
 * <ul>
 *   <li>Variant: {@code Argon2id} (hybrid of Argon2i and Argon2d)</li>
 *   <li>Iterations: {@value #ITERATIONS}</li>
 *   <li>Memory: {@value #MEMORY_KB} KB ({@value #MEMORY_KB_READABLE} MB)</li>
 *   <li>Parallelism: {@value #PARALLELISM} threads</li>
 * </ul>
 *
 * @author Ayush Kishan
 */
public class Argon2DerivationService implements KeyDerivationService {

    private static final Logger logger = LoggerFactory.getLogger(Argon2DerivationService.class);

    // OWASP recommended Argon2id parameters (2023)
    private static final int ITERATIONS      = 3;
    private static final int MEMORY_KB       = 65536; // 64 MB
    private static final int PARALLELISM     = 4;
    private static final String MEMORY_KB_READABLE = "64";

    /** Recommended salt length in bytes. */
    public static final int SALT_LENGTH_BYTES = 16;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Derives a key from the given password and salt using Argon2id (blocking).
     *
     * <p>The password char array is encoded to UTF-8 bytes internally.
     * The resulting byte array is zeroed from memory immediately after use.
     *
     * @param password  The master password chars (zeroed by caller after this returns).
     * @param salt      At least {@value #SALT_LENGTH_BYTES} bytes of random salt.
     * @param keyLength Desired key length in bytes (32 for AES-256).
     * @return The derived key of {@code keyLength} bytes.
     * @throws CryptoException if Argon2 derivation fails.
     */
    @Override
    public byte[] deriveKey(char[] password, byte[] salt, int keyLength) {
        logger.debug("Starting Argon2id key derivation (iterations={}, memory={}MB, parallelism={})...",
                ITERATIONS, MEMORY_KB_READABLE, PARALLELISM);

        byte[] passwordBytes = null;
        try {
            Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withIterations(ITERATIONS)
                    .withMemoryAsKB(MEMORY_KB)
                    .withParallelism(PARALLELISM)
                    .withSalt(salt)
                    .build();

            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(params);

            passwordBytes = charArrayToUtf8Bytes(password);
            byte[] derivedKey = new byte[keyLength];
            generator.generateBytes(passwordBytes, derivedKey, 0, derivedKey.length);

            logger.debug("Argon2id key derivation complete. Key length: {} bytes", keyLength);
            return derivedKey;

        } catch (Exception e) {
            throw new CryptoException("Argon2id key derivation failed.", e);
        } finally {
            // Zero the password bytes from memory regardless of success or failure
            if (passwordBytes != null) {
                Arrays.fill(passwordBytes, (byte) 0);
            }
        }
    }

    /**
     * Derives a key asynchronously using Argon2id.
     *
     * <p>Submits the blocking {@link #deriveKey} call to the common ForkJoin pool,
     * keeping the calling thread (e.g., JavaFX UI thread) free to update progress
     * indicators while derivation runs in the background.
     *
     * @param password  The master password chars.
     * @param salt      Random salt bytes.
     * @param keyLength Desired key length in bytes.
     * @return A {@link CompletableFuture} resolving to the derived key.
     */
    @Override
    public CompletableFuture<byte[]> deriveKeyAsync(char[] password, byte[] salt, int keyLength) {
        logger.debug("Submitting Argon2id key derivation to background thread...");
        return CompletableFuture.supplyAsync(() -> deriveKey(password, salt, keyLength));
    }

    /**
     * Generates a cryptographically random salt of the recommended length.
     *
     * @return A {@value #SALT_LENGTH_BYTES}-byte random salt.
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Converts a char array to a UTF-8 byte array without going through a String
     * (which would create an interned, un-zeroable object on the heap).
     */
    private byte[] charArrayToUtf8Bytes(char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        // Zero the ByteBuffer backing array
        Arrays.fill(byteBuffer.array(), (byte) 0);
        return bytes;
    }
}
