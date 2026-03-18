package com.citadel.vault;

import com.citadel.model.VaultItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe, in-memory data store for {@link VaultItem} objects.
 *
 * <p>Uses a {@link ConcurrentHashMap} keyed by {@link UUID} for O(1) lookups,
 * and exposes rich query methods (search by label substring, filter by type).
 *
 * <p>This is the in-memory layer — persistence is handled separately by
 * {@link VaultManager} (encryption + file I/O).
 *
 * @author Ayush Kishan
 */
public class VaultStore {

    private static final Logger logger = LoggerFactory.getLogger(VaultStore.class);

    /** Primary index: UUID → VaultItem */
    private final ConcurrentHashMap<UUID, VaultItem> items = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Mutations
    // -------------------------------------------------------------------------

    /** Adds or replaces an item. */
    public void put(VaultItem item) {
        Objects.requireNonNull(item, "VaultItem must not be null");
        items.put(item.getId(), item);
        logger.debug("Stored item: id={}, label={}", item.getId(), item.getLabel());
    }

    /** Bulk-loads a list of items (replaces current contents). */
    public void loadAll(List<VaultItem> incoming) {
        items.clear();
        if (incoming != null) {
            incoming.forEach(this::put);
        }
        logger.debug("VaultStore loaded {} items.", items.size());
    }

    /** Removes an item by its UUID. Returns {@code true} if it existed. */
    public boolean remove(UUID id) {
        boolean removed = items.remove(id) != null;
        if (removed) logger.debug("Removed item: id={}", id);
        return removed;
    }

    /** Wipes all items from memory (e.g. on vault lock). */
    public void clear() {
        items.clear();
        logger.debug("VaultStore cleared.");
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /** Returns the item with the given UUID, or {@code Optional.empty()}. */
    public Optional<VaultItem> findById(UUID id) {
        return Optional.ofNullable(items.get(id));
    }

    /** Returns an unmodifiable snapshot of all items, sorted by label. */
    public List<VaultItem> findAll() {
        return items.values().stream()
                .sorted(Comparator.comparing(VaultItem::getLabel, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Searches items whose label or notes contains the given query (case-insensitive).
     *
     * @param query The substring to search for.
     * @return Matching items sorted by label.
     */
    public List<VaultItem> search(String query) {
        if (query == null || query.isBlank()) return findAll();
        String lower = query.toLowerCase();
        return items.values().stream()
                .filter(item -> item.getLabel().toLowerCase().contains(lower))
                .sorted(Comparator.comparing(VaultItem::getLabel, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Filters items by credential type.
     *
     * @param type The type to filter by (e.g. {@code CredentialType.PASSWORD}).
     * @return Matching items sorted by label.
     */
    public List<VaultItem> filterByType(VaultItem.CredentialType type) {
        return items.values().stream()
                .filter(item -> item.getType() == type)
                .sorted(Comparator.comparing(VaultItem::getLabel, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toUnmodifiableList());
    }

    /** Returns the total number of stored items. */
    public int size() {
        return items.size();
    }

    /** Returns {@code true} if no items are stored. */
    public boolean isEmpty() {
        return items.isEmpty();
    }
}
