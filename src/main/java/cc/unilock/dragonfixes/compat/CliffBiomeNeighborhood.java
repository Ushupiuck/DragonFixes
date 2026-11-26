package cc.unilock.dragonfixes.compat;

import java.util.function.Predicate;

/** Bounded, pass-local memoization. Never caches results across chunks or worlds. */
public final class CliffBiomeNeighborhood<T> {
    public static final int MAX_SEARCH = 16;
    private static final int SIDE = 16 + 2 * MAX_SEARCH;
    private final int minX, minZ;
    private final Object[] values = new Object[SIDE * SIDE];
    private final boolean[] known = new boolean[values.length];
    private Boolean anyReachableCliff;
    private final boolean[] attemptedTiles = new boolean[9];

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
    public interface TileLookup<T> {
        T[] get(int alignedX, int alignedZ);
    }

    /**
     * Fill only previously unknown, non-null entries. Nulls must be resolved
     * by the original scalar path (RTG has a null-biome patcher).
     * Tile layout is the vanilla BiomeCache convention, x + 16*z.
     */
    private void tryTile(int x, int z, TileLookup<T> tiles) {
        if (tiles == null) return;
        int tx = (x & ~15), tz = (z & ~15);
        int tile = ((tz - minZ) >> 4) * 3 + ((tx - minX) >> 4);
        if (attemptedTiles[tile]) return;
        attemptedTiles[tile] = true;
        T[] data = tiles.get(tx, tz);
        if (data == null || data.length != 256) return;
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                int i = index(tx + dx, tz + dz);
                T b = data[dx + 16 * dz];
                if (!isKnown(i) && b != null) remember(i, b);
            }
        }
    }

    /**
     * getInvertedDistanceFactor samples radii 1,5,9,13 for search <=16.
     * floor(x + d*cos(a)) cannot exceed this symmetric 13-block halo.
     * This is a negative-only broad phase, never a replacement for first-hit search.
     */
    public boolean mayContainReachableCliff(Lookup<T> scalar, TileLookup<T> tiles, Predicate<T> cliff) {
        if (anyReachableCliff != null) return anyReachableCliff;
        final int inset = MAX_SEARCH - 13;
        for (int dz = inset; dz < SIDE - inset; dz++) {
            for (int dx = inset; dx < SIDE - inset; dx++) {
                int i = dz * SIDE + dx;
                int x = minX + dx, z = minZ + dz;
                if (!isKnown(i)) tryTile(x, z, tiles);
                T b = isKnown(i) ? get(i) : remember(i, scalar.get(x, z));
                if (cliff.test(b)) return anyReachableCliff = true;
            }
        }
        return anyReachableCliff = false;
    }

    @FunctionalInterface
    public interface Lookup<T> {
        T get(int x, int z);
    }

}
