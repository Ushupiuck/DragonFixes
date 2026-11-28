package cc.unilock.dragonfixes.compat;

/** Per-pass height cache, including shaper and input-biome identity. */
public final class CliffHeightMemo {
    private static final int HALO = 24, SIDE = 16 + 2 * HALO;
    private final int minX, minZ;
    private Object owner;
    private final Row[] rows = new Row[SIDE];
    private static final class Row {
        final Object[] biomes = new Object[SIDE];
        final double[] heights = new double[SIDE];
        final boolean[] known = new boolean[SIDE];
    }
    public CliffHeightMemo(int chunkX, int chunkZ) {
        minX = (chunkX << 4) - HALO;
        minZ = (chunkZ << 4) - HALO;
    }
    public int index(int x, int z) {
        long dx = (long)x - minX, dz = (long)z - minZ;
        return dx < 0 || dx >= SIDE || dz < 0 || dz >= SIDE ? -1 : (int)(dz * SIDE + dx);
    }
    public boolean contains(Object shaper, int index, Object biome) {
        if (index < 0 || owner != shaper) return false;
        Row row = rows[index / SIDE];
        int x = index % SIDE;
        return row != null && row.known[x] && row.biomes[x] == biome;
    }
    public double get(int index) { return rows[index / SIDE].heights[index % SIDE]; }
    public double remember(Object shaper, int index, Object biome, double height) {
        if (index < 0) return height;
        if (owner != shaper) {
            java.util.Arrays.fill(rows, null);
            owner = shaper;
        }
        int z = index / SIDE, x = index % SIDE;
        Row row = rows[z];
        if (row == null) rows[z] = row = new Row();
        row.biomes[x] = biome;
        row.heights[x] = height;
        row.known[x] = true;
        return height;
    }
}
