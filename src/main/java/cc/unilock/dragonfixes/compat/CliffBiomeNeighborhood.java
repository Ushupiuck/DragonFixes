package cc.unilock.dragonfixes.compat;

import java.util.function.Predicate;

/** Bounded, pass-local memoization. Never caches results across chunks or worlds. */
public final class CliffBiomeNeighborhood<T> {
    public static final int MAX_SEARCH = 16;
    private final int halo, side, tileSide;
    private static final Object NULL_BIOME = new Object();
    private final int minX, minZ;
    private final Object[][] rows;
    private Boolean anyReachableCliff;
    private final boolean[] attemptedTiles;

    public CliffBiomeNeighborhood(int chunkX, int chunkZ) {
        this(chunkX, chunkZ, MAX_SEARCH);
    }

    public CliffBiomeNeighborhood(int chunkX, int chunkZ, int halo) {
        if (halo < MAX_SEARCH || halo > 48 || (halo & 15) != 0) throw new IllegalArgumentException("halo");
        this.halo = halo;
        side = 16 + 2 * halo;
        tileSide = side / 16;
        rows = new Object[side][];
        attemptedTiles = new boolean[tileSide * tileSide];
        minX = (chunkX << 4) - halo;
        minZ = (chunkZ << 4) - halo;
    }

    public boolean isChunkColumn(int x, int z) {
        long dx = (long)x - minX, dz = (long)z - minZ;
        return dx >= halo && dx < halo + 16
                && dz >= halo && dz < halo + 16;
    }

    public int index(int x, int z) {
        long dx = (long)x - minX, dz = (long)z - minZ;
        if (dx < 0 || dx >= side || dz < 0 || dz >= side) return -1;
        return (int) (dz * side + dx);
    }

    public boolean isKnown(int index) { return index >= 0 && rows[index / side] != null && rows[index / side][index % side] != null; }

    @SuppressWarnings("unchecked")
    public T get(int index) { Object v = rows[index / side][index % side]; return v == NULL_BIOME ? null : (T) v; }

    public T remember(int index, T biome) {
        if (index >= 0) {
            int z = index / side;
            if (rows[z] == null) rows[z] = new Object[side];
            rows[z][index % side] = biome == null ? NULL_BIOME : biome;
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
        int tile = ((tz - minZ) >> 4) * tileSide + ((tx - minX) >> 4);
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
        final int inset = halo - 13;
        for (int dz = inset; dz < side - inset; dz++) {
            for (int dx = inset; dx < side - inset; dx++) {
                int i = dz * side + dx;
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
