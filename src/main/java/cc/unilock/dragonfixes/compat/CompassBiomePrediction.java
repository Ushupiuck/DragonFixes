package cc.unilock.dragonfixes.compat;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

/** Optional generator-specific predictions, called only when live/saved data is unavailable. */
public interface CompassBiomePrediction {
    interface Rwg1 {
        BiomeGenBase dragonfixes$predictRwg1(World world, int x, int z, BiomeGenBase raw);
    }

    interface Rtg {
        BiomeGenBase dragonfixes$predictRtg(World world, int x, int z, BiomeGenBase raw);
    }
}
