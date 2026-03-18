package com.citadel.ui;

import com.citadel.vault.VaultManager;

/**
 * Application-wide singleton context holding shared state (VaultManager).
 *
 * <p>All JavaFX controllers obtain the {@link VaultManager} from here
 * instead of constructing their own instance.
 */
public class AppContext {

    private static VaultManager vaultManager;

    private AppContext() {}

    public static void init(VaultManager manager) {
        vaultManager = manager;
    }

    public static VaultManager getVaultManager() {
        if (vaultManager == null) throw new IllegalStateException("AppContext not initialised!");
        return vaultManager;
    }
}
