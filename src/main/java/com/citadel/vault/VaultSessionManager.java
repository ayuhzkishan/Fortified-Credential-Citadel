package com.citadel.vault;

import com.citadel.crypto.CryptoException;
import com.citadel.event.VaultEvent;
import com.citadel.event.VaultEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Singleton managing the lifecycle of an active vault session.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Hold the derived master key in memory while the vault is unlocked.</li>
 *   <li>Wipe the master key from memory when the vault is locked.</li>
 *   <li>Manage the auto-lock timer to protect unattended vaults.</li>
 *   <li>Provide global lock state to other components.</li>
 * </ul>
 *
 * @author Ayush Kishan
 */
public class VaultSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(VaultSessionManager.class);

    private static volatile VaultSessionManager instance;

    private byte[] sessionKey;
    private boolean isLocked = true;

    // Auto-lock configuration
    private Timer autoLockTimer;
    private long autoLockDelayMs = 5 * 60 * 1000; // Default: 5 minutes

    private VaultSessionManager() {}

    /**
     * Returns the single instance of {@code VaultSessionManager}.
     */
    public static VaultSessionManager getInstance() {
        if (instance == null) {
            synchronized (VaultSessionManager.class) {
                if (instance == null) {
                    instance = new VaultSessionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Called when the vault is successfully unlocked.
     * Sets the session key, updates state, and starts the auto-lock timer.
     *
     * @param masterKey The derived 256-bit AES master key.
     */
    public synchronized void onUnlocked(byte[] masterKey) {
        if (masterKey == null || masterKey.length != 32) {
            throw new IllegalArgumentException("Invalid master key length for session.");
        }
        
        // Wipe old key if it exists (defensive programming)
        if (this.sessionKey != null) {
            Arrays.fill(this.sessionKey, (byte) 0);
        }

        // Copy key so caller can zero their array
        this.sessionKey = Arrays.copyOf(masterKey, masterKey.length);
        this.isLocked = false;
        
        resetAutoLockTimer();
        VaultEventBus.publish(VaultEvent.VAULT_UNLOCKED);
        logger.info("Vault session unlocked.");
    }

    /**
     * Locks the vault by immediately zeroing the session key from memory
     * and stopping the auto-lock timer.
     */
    public synchronized void lock() {
        if (isLocked) {
            return; // Already locked
        }

        if (sessionKey != null) {
            Arrays.fill(sessionKey, (byte) 0);
            sessionKey = null;
        }

        if (autoLockTimer != null) {
            autoLockTimer.cancel();
            autoLockTimer = null;
        }

        isLocked = true;
        VaultEventBus.publish(VaultEvent.VAULT_LOCKED);
        logger.info("Vault session locked. Master key erased from memory.");
    }

    /**
     * Retrieves the current session key.
     *
     * @return The 32-byte AES key.
     * @throws CryptoException if the vault is currently locked.
     */
    public synchronized byte[] getSessionKey() {
        if (isLocked || sessionKey == null) {
            throw new CryptoException("Cannot retrieve session key: Vault is locked.");
        }
        return sessionKey; // Note: normally we'd return a copy, but we want to avoid key material spread in heap
    }

    /**
     * Returns whether the vault is currently locked.
     */
    public synchronized boolean isLocked() {
        return isLocked;
    }

    /**
     * Touches the session to keep it alive, resetting the auto-lock countdown.
     * Call this on user interaction (mouse move, key press in UI).
     */
    public synchronized void keepAlive() {
        if (!isLocked) {
            resetAutoLockTimer();
        }
    }

    /**
     * Sets a custom auto-lock delay.
     *
     * @param minutes The delay in minutes (use 0 to disable auto-lock).
     */
    public synchronized void setAutoLockDelayMinutes(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("Delay cannot be negative.");
        this.autoLockDelayMs = (long) minutes * 60 * 1000;
        if (!isLocked) {
            resetAutoLockTimer();
        }
    }

    /**
     * Cancels any existing timer and schedules a new lock + warning timer.
     */
    private void resetAutoLockTimer() {
        if (autoLockTimer != null) {
            autoLockTimer.cancel();
        }

        if (autoLockDelayMs <= 0) {
            return; // Auto-lock disabled
        }

        autoLockTimer = new Timer("Vault-AutoLock-Timer", true); // Daemon thread

        long warningDelayMs = Math.max(0, autoLockDelayMs - 60000); // Warn 60s before lock

        // Schedule warning (only if delay > 1 minute)
        if (warningDelayMs > 0) {
            autoLockTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    VaultEventBus.publish(VaultEvent.AUTO_LOCK_WARNING);
                }
            }, warningDelayMs);
        }

        // Schedule actual lock
        autoLockTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("Auto-lock timeout reached.");
                lock();
            }
        }, autoLockDelayMs);
    }
}
