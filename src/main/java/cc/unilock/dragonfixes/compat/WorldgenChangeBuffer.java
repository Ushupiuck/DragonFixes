package cc.unilock.dragonfixes.compat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Owned by one World, including when terrain and population run on different threads. */
public final class WorldgenChangeBuffer<K, V> {
    private volatile int pending;
    private final ThreadLocal<Boolean> dispatching = new ThreadLocal<>();
    private HashMap<K, V> data;

    public synchronized void begin() {
        pending++;
    }

    public boolean isRecording() {
        return pending > 0 && dispatching.get() == null;
    }

    public synchronized void put(K key, V value) {
        // Recheck after acquiring the lock: another thread may have finished.
        if (!isRecording()) return;
        if (data == null) data = new HashMap<>();
        data.put(key, value);
    }

    private synchronized Map<K, V> drain(boolean nested) {
        if (pending > 0) pending--;
        if (nested || data == null || data.isEmpty()) return Collections.emptyMap();

        Map<K, V> snapshot;
        if (data.size() == 1) {
            Map.Entry<K, V> entry = data.entrySet().iterator().next();
            snapshot = Collections.singletonMap(entry.getKey(), entry.getValue());
        } else {
            // Preserve the original copy ordering for a sequential per-world
            // batch, since some watchers consume world RNG in iteration order.
            snapshot = Collections.unmodifiableMap(new HashMap<>(data));
        }
        // Clear under the same lock, BEFORE the callbacks. Later writes belong
        // to a subsequent snapshot and must not be erased by this dispatch.
        data.clear();
        return snapshot;
    }

    public void finish(Consumer<Map<K, V>> watcher) {
        Map<K, V> snapshot = drain(dispatching.get() != null);
        if (snapshot.isEmpty()) return;
        dispatching.set(Boolean.TRUE);
        try {
            watcher.accept(snapshot);
        } finally {
            dispatching.remove();
        }
    }
}
