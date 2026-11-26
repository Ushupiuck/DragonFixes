package cc.unilock.dragonfixes.mixin.early.world;

import net.minecraft.world.biome.BiomeCache;
import net.minecraft.world.biome.WorldChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldChunkManager.class)
public interface WorldChunkManagerBiomeCacheAccessor {
    @Accessor("biomeCache")
    BiomeCache dragonfixes$getVanillaBiomeCache();
}
