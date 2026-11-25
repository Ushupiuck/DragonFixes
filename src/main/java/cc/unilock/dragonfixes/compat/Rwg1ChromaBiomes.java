package cc.unilock.dragonfixes.compat;

import rwg.biomes.realistic.RealisticBiomeBase;

public final class Rwg1ChromaBiomes {
    private Rwg1ChromaBiomes() {}

    public static RealisticBiomeBase cliffs;
    public static RealisticBiomeBase shores;

    public static boolean isCliff(RealisticBiomeBase biome) {
        return biome != null && ChromaBiomeEvents.isCliff(biome.baseBiome);
    }
}
