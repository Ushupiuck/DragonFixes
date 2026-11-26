package cc.unilock.dragonfixes.compat;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Generation and population are separate events, not necessarily one per stack frame. */
public final class WorldgenChangeBuffer<W, K, V> {
    private final AtomicLong epoch = new AtomicLong();
    private final ThreadLocal<Context<W, K, V>> local = new ThreadLocal<>();

    public void begin(W world) {
        state(world, true).depth++;
    }

    public Batch<K, V> recording(W world) {
        Batch<K, V> batch = state(world, false);
        return batch != null && batch.depth > 0 && !batch.dispatching ? batch : null;
    }

    public void finish(W world, Consumer<Map<K, V>> watcher) {
        Batch<K, V> batch = state(world, false);
        if (batch == null) return;
        if (batch.dispatching) {
            // A watcher can synchronously cause more population. Never redispatch
            // its parent batch or clear the parent's dispatch guard.
            if (batch.depth > 0) batch.depth--;
            return;
        }
        batch.dispatching = true;
        try {
            if (batch.data != null && !batch.data.isEmpty()) {
                Map<K, V> snapshot;
                if (batch.data.size() == 1) {
                    Map.Entry<K, V> entry = batch.data.entrySet().iterator().next();
                    snapshot = Collections.singletonMap(entry.getKey(), entry.getValue());
                } else {
                    // Keep the original copy and its iteration order. Some watchers
                    // consume world RNG in that order and may retain the snapshot.
                    snapshot = Collections.unmodifiableMap(new HashMap<>(batch.data));
                }
                watcher.accept(snapshot);
            }
        } finally {
            if (batch.data != null) batch.data.clear();
            if (batch.depth > 0) batch.depth--;
            batch.dispatching = false;
        }
    }

    /** Logout can run on the client thread; invalidate other threads lazily. */
    public void reset() {
        epoch.incrementAndGet();
        local.remove();
    }

    private Batch<K, V> state(W world, boolean create) {
        long generation = epoch.get();
        Context<W, K, V> context = local.get();
        if (context != null && context.epoch != generation) {
            local.remove();
            context = null;
        }
        if (context == null) {
            if (!create) return null;
            context = new Context<>(generation);
            local.set(context);
        }
        if (context.lastWorld != null && context.lastWorld.get() == world) {
            return context.lastBatch;
        }
        Batch<K, V> batch = context.worlds.get(world);
        if (batch == null && create) {
            batch = new Batch<>();
            context.worlds.put(world, batch);
        }
        if (batch != null) {
            context.lastWorld = new WeakReference<>(world);
            context.lastBatch = batch;
        }
        return batch;
    }

    private static final class Context<W, K, V> {
        final long epoch;
        // World uses identity equality; weak keys avoid retaining unloaded worlds.
        final WeakHashMap<W, Batch<K, V>> worlds = new WeakHashMap<>();
        WeakReference<W> lastWorld;
        Batch<K, V> lastBatch;

        Context(long epoch) {
            this.epoch = epoch;
        }
    }

    public static final class Batch<K, V> {
        private int depth;
        private boolean dispatching;
        private HashMap<K, V> data;

        public void put(K key, V value) {
            if (data == null) data = new HashMap<>();
            data.put(key, value);
        }
    }
}
