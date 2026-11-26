package cc.unilock.dragonfixes.compat;

import java.util.function.Predicate;

/** Bounded, pass-local memoization. Never caches results across chunks or worlds. */
public final class CliffBiomeNeighborhood<T> {
    public static final int MAX_SEARCH = 16;
    private static final int SIDE = 16 + 2 * MAX_SEARCH;
    private final int minX, minZ;
    private final Object[] values = new Object[SIDE * SIDE];
    private final boolean[] known = new boolean[values.length];
    private Boolean anyCliff;

    public CliffBiomeNeighborhood(int chunkX, int chunkZ) {
        minX = (chunkX << 4) - MAX_SEARCH;
        minZ = (chunkZ << 4) - MAX_SEARCH;
    }

    public boolean isChunkColumn(int x, int z) {
        long dx = (long)x - minX, dz = (long)z - minZ;
        return dx >= MAX_SEARCH && dx < MAX_SEARCH + 16
                && dz >= MAX_SEARCH && dz < MAX_SEARCH + 16;
    }

    public int index(int x, int z) {
        long dx = (long)x - minX, dz = (long)z - minZ;
        if (dx < 0 || dx >= SIDE || dz < 0 || dz >= SIDE) return -1;
        return (int) (dz * SIDE + dx);
    }

    public boolean isKnown(int index) { return index >= 0 && known[index]; }

    @SuppressWarnings("unchecked")
    public T get(int index) { return (T) values[index]; }

    public T remember(int index, T biome) {
        if (index >= 0) {
            values[index] = biome;
            known[index] = true;
        }
        return biome;
    }

    @FunctionalInterface
    public interface Lookup<T> {
        T get(int x, int z);
    }

    /**
     * A conservative broad phase: every coordinate the original radial search
     * can touch lies inside this square. A positive answer does not determine
     * the original search result; only a negative answer allows an early exit.
     * Lookup is the same memoized biome query used by the original search.
     */
    public boolean mayContainCliff(Lookup<T> lookup, Predicate<T> cliff) {
        if (anyCliff == null) {
            for (int dz = 0; dz < SIDE; dz++) {
                for (int dx = 0; dx < SIDE; dx++) {
                    if (cliff.test(lookup.get(minX + dx, minZ + dz))) {
                        anyCliff = true;
                        return true;
                    }
                }
            }
            anyCliff = false;
        }
        return anyCliff;
    }
}
