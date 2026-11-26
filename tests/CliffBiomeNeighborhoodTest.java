import cc.unilock.dragonfixes.compat.CliffBiomeNeighborhood;

public final class CliffBiomeNeighborhoodTest {
    private static void check(boolean ok) {
        if (!ok) throw new AssertionError();
    }

    private static long firstHit(int x, int z, CliffBiomeNeighborhood.Lookup<Integer> lookup) {
        for (int d = 1; d <= 16; d += 4) {
            for (double a = 0; a < 360; a += 7.5) {
                int dx = (int)Math.floor(x + d * Math.cos(Math.toRadians(a)));
                int dz = (int)Math.floor(z + d * Math.sin(Math.toRadians(a)));
                Integer b = lookup.get(dx, dz);
                if (b != null && b == 1) return ((long)dx << 32) ^ (dz & 0xffffffffL);
            }
        }
        return Long.MIN_VALUE;
    }

    public static void main(String[] args) {
        int total = 0;
        for (int cx : new int[]{-1874999, -3, -1, 0, 2, 1874999}) {
            for (int cz : new int[]{-2, 0, 3}) {
                for (int scenario = 0; scenario < 4; scenario++) {
                    final int mode = scenario, ox = cx * 16, oz = cz * 16;
                    CliffBiomeNeighborhood<Integer> cache = new CliffBiomeNeighborhood<>(cx, cz);
                    int[] calls = {0};
                    CliffBiomeNeighborhood.Lookup<Integer> raw = (x,z) -> {
                        if (mode == 0) return null;
                        if (mode == 1) return 0;
                        if (mode == 2) return x == ox - 1 && z == oz ? 1 : 0;
                        return (x - ox + 2 * (z - oz)) % 19 == 0 ? 1 : 0;
                    };
                    CliffBiomeNeighborhood.Lookup<Integer> cached = (x,z) -> {
                        int i = cache.index(x,z);
                        if (cache.isKnown(i)) return cache.get(i);
                        calls[0]++;
                        return cache.remember(i, raw.get(x,z));
                    };
                    boolean may = cache.mayContainReachableCliff(cached, null, b -> b != null && b == 1);
                    for (int x = ox; x < ox + 16; x++) {
                        for (int z = oz; z < oz + 16; z++) {
                            check(cache.isChunkColumn(x,z));
                            for (int d = 1; d <= 16; d += 4) {
                                for (double a = 0; a < 360; a += 7.5) {
                                    int dx = (int)Math.floor(x + d*Math.cos(Math.toRadians(a)));
                                    int dz = (int)Math.floor(z + d*Math.sin(Math.toRadians(a)));
                                    check(cache.index(dx,dz) >= 0);
                                }
                            }
                            long expected = firstHit(x,z,raw);
                            long actual = may ? firstHit(x,z,cached) : Long.MIN_VALUE;
                            check(expected == actual);
                            total++;
                        }
                    }
                    int before = calls[0];
                    check(cache.mayContainReachableCliff(cached, null,b -> b != null && b == 1) == may);
                    check(calls[0] == before && calls[0] <= 48*48);
                    check(!cache.isChunkColumn(ox-1,oz) && cache.index(ox-17,oz) == -1);
                    if (mode < 2) check(!may && calls[0] == 42*42);
                }
            }
        }
        System.out.println("Cliff search parity: " + total + " columns");
    }
}
