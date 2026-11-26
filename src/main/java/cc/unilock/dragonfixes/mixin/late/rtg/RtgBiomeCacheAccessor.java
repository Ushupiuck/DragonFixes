package cc.unilock.dragonfixes.mixin.late.rtg;

import net.minecraft.world.biome.BiomeCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "rtg.world.biome.WorldChunkManagerRTG", remap = false)
public interface RtgBiomeCacheAccessor {
    @Accessor("biomeCache")
    BiomeCache dragonfixes$getRtgBiomeCache();
}
