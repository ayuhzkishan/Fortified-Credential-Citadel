package com.citadel.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;

/**
 * Singleton manager for the cryptographic engine.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Registers the Bouncy Castle JCE provider exactly once at startup.</li>
 *   <li>Acts as a single, shared access point for the crypto subsystem.</li>
 *   <li>Prevents redundant or conflicting provider registrations across the app.</li>
 * </ul>
 *
 * <p>Thread-safe via double-checked locking with a {@code volatile} instance field,
 * ensuring correct behaviour even under Java's memory model in a concurrent environment.
 *
 * <p>Usage:
 * <pre>{@code
 *   CryptoEngineManager manager = CryptoEngineManager.getInstance();
 *   // BouncyCastle provider is now guaranteed to be registered
 * }</pre>
 *
 * @author Ayush Kishan
 */
public class CryptoEngineManager {

    private static final Logger logger = LoggerFactory.getLogger(CryptoEngineManager.class);

    /**
     * The name of the Bouncy Castle JCE provider.
     * Referenced by CipherFactory and all crypto services when requesting a provider.
     */
    public static final String BC_PROVIDER = BouncyCastleProvider.PROVIDER_NAME;

    // volatile ensures the instance write is visible to all threads immediately
    private static volatile CryptoEngineManager instance;

    /** Private constructor — use {@link #getInstance()} */
    private CryptoEngineManager() {
        registerBouncyCastle();
    }

    /**
     * Returns the single instance of {@code CryptoEngineManager}.
     *
     * <p>Uses double-checked locking for thread-safe lazy initialization
     * without the cost of synchronizing on every call.
     *
     * @return The singleton instance.
     */
    public static CryptoEngineManager getInstance() {
        if (instance == null) {
            synchronized (CryptoEngineManager.class) {
                if (instance == null) {
                    instance = new CryptoEngineManager();
                }
            }
        }
        return instance;
    }

    /**
     * Registers the Bouncy Castle provider if not already registered.
     * Idempotent — safe to call multiple times (only registers once).
     */
    private void registerBouncyCastle() {
        if (Security.getProvider(BC_PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
            logger.info("Bouncy Castle provider registered: {}", BC_PROVIDER);
        } else {
            logger.debug("Bouncy Castle provider already registered — skipping.");
        }
    }

    /**
     * Returns {@code true} if the Bouncy Castle provider is currently registered
     * with the JVM's security framework.
     *
     * @return {@code true} if BouncyCastle is active.
     */
    public boolean isProviderRegistered() {
        return Security.getProvider(BC_PROVIDER) != null;
    }
}
