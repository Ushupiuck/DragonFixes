package cc.unilock.dragonfixes.compat;

/** One lazy 16x16 cache per carving pass. No global biome state. */
public final class CarverBiomeColumns<T> {
    private final int chunkX, chunkZ;
    private final Object[] values = new Object[256];
    private final boolean[] known = new boolean[256];

    public CarverBiomeColumns(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int index(int x, int z) {
        if ((x >> 4) != chunkX || (z >> 4) != chunkZ) return -1;
        return ((x & 15) << 4) | (z & 15);
    }

    public boolean isKnown(int index) { return known[index]; }

    @SuppressWarnings("unchecked")
    public T get(int index) { return (T) values[index]; }

    public void remember(int index, T biome) {
        values[index] = biome;
        known[index] = true;
    }
}
