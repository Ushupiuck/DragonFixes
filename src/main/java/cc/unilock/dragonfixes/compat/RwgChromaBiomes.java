package cc.unilock.dragonfixes.compat;

import rwg2.biomes.realistic.RealisticBiomeBase;

/** Wrappers created by RWG2's own ChromatiCraft support, without allocating extra realistic biome IDs. */
public final class RwgChromaBiomes {
    public static RealisticBiomeBase cliffs;
    public static RealisticBiomeBase shores;

    private RwgChromaBiomes() {}

    public static boolean isCliff(RealisticBiomeBase biome) {
        return biome != null && (biome == cliffs || biome == shores);
    }
}
