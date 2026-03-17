package com.citadel.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A thread-safe event bus implementing the Observer pattern for vault events.
 *
 * <p>Decouples the vault core from any UI layer. The core fires events;
 * UI components subscribe to the events they care about and react accordingly —
 * without the core ever knowing anything about the UI.
 *
 * <p><b>Thread safety:</b>
 * <ul>
 *   <li>{@link ConcurrentHashMap} for the subscriber registry — safe for concurrent
 *       subscribe/unsubscribe from multiple threads.</li>
 *   <li>{@link CopyOnWriteArrayList} per event — safe for iteration even while
 *       new subscribers are being added.</li>
 * </ul>
 *
 * <p><b>Usage example:</b>
 * <pre>{@code
 *   // Subscribe
 *   VaultEventBus.subscribe(VaultEvent.VAULT_UNLOCKED, e -> updateUIToUnlockedState());
 *   VaultEventBus.subscribe(VaultEvent.AUTO_LOCK_WARNING, e -> showCountdownBanner());
 *
 *   // Publish (called by VaultManager internally)
 *   VaultEventBus.publish(VaultEvent.VAULT_UNLOCKED);
 * }</pre>
 *
 * @author Ayush Kishan
 */
public class VaultEventBus {

    private static final Logger logger = LoggerFactory.getLogger(VaultEventBus.class);

    /**
     * Registry: each VaultEvent maps to a list of subscribed listeners.
     * ConcurrentHashMap + CopyOnWriteArrayList = fully thread-safe without locks.
     */
    private static final Map<VaultEvent, List<Consumer<VaultEvent>>> subscribers =
            new ConcurrentHashMap<>();

    /** Utility class — no instantiation. */
    private VaultEventBus() {}

    /**
     * Subscribes a listener to a specific vault event.
     *
     * <p>The listener will be called every time the given event is published.
     * Multiple listeners can be subscribed to the same event.
     *
     * @param event    The {@link VaultEvent} to subscribe to.
     * @param listener A {@link Consumer} that handles the event when it fires.
     */
    public static void subscribe(VaultEvent event, Consumer<VaultEvent> listener) {
        subscribers
                .computeIfAbsent(event, e -> new CopyOnWriteArrayList<>())
                .add(listener);
        logger.debug("Subscribed listener to event: {}", event);
    }

    /**
     * Unsubscribes a previously registered listener from an event.
     *
     * @param event    The event to unsubscribe from.
     * @param listener The exact listener instance to remove.
     */
    public static void unsubscribe(VaultEvent event, Consumer<VaultEvent> listener) {
        List<Consumer<VaultEvent>> listeners = subscribers.get(event);
        if (listeners != null) {
            listeners.remove(listener);
            logger.debug("Unsubscribed listener from event: {}", event);
        }
    }

    /**
     * Publishes a vault event, notifying all registered subscribers synchronously.
     *
     * <p>Subscribers are notified in subscription order.
     * Exceptions thrown by a listener are caught and logged — they do not
     * interrupt delivery to remaining subscribers.
     *
     * @param event The {@link VaultEvent} to publish.
     */
    public static void publish(VaultEvent event) {
        logger.debug("Publishing event: {}", event);
        List<Consumer<VaultEvent>> listeners = subscribers.getOrDefault(event, List.of());
        for (Consumer<VaultEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.error("Exception in event listener for {}: {}", event, e.getMessage(), e);
            }
        }
    }

    /**
     * Removes all subscribers for all events.
     * Useful for clean-up in tests or application shutdown.
     */
    public static void clearAll() {
        subscribers.clear();
        logger.debug("All VaultEventBus subscribers cleared.");
    }
}
