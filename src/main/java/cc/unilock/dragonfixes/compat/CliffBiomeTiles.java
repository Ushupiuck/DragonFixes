package cc.unilock.dragonfixes.compat;

import cc.unilock.dragonfixes.mixin.early.world.WorldChunkManagerBiomeCacheAccessor;
import cc.unilock.dragonfixes.mixin.late.rtg.RtgBiomeCacheAccessor;
import net.minecraft.world.biome.BiomeCache;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

/** Only exact audited manager implementations; never substitute bulk
 * generation arrays for the scalar prediction paths of RWG or other mods. */
public final class CliffBiomeTiles {
    private CliffBiomeTiles() {}

    public static BiomeGenBase[] getTile(WorldChunkManager manager, int x, int z) {
        BiomeCache cache = null;
        if (manager.getClass() == WorldChunkManager.class
                && manager instanceof WorldChunkManagerBiomeCacheAccessor) {
            cache = ((WorldChunkManagerBiomeCacheAccessor) manager).dragonfixes$getVanillaBiomeCache();
        } else if (manager.getClass().getName().equals("rtg.world.biome.WorldChunkManagerRTG")
                && manager instanceof RtgBiomeCacheAccessor) {
            cache = ((RtgBiomeCacheAccessor) manager).dragonfixes$getRtgBiomeCache();
        }
        // Unlike the manager's bulk API this does not unconditionally reset
        // IntCache; it is the cache used by the original scalar lookup.
        return cache == null ? null : cache.getCachedBiomes(x, z);
    }
}
