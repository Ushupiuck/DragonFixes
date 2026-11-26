package cc.unilock.dragonfixes.mixin.late.rtg;

import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.compat.RtgMapGenIds;
import cc.unilock.dragonfixes.compat.RtgMapGenScope;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import rtg.util.CellNoise;
import rtg.util.OpenSimplexNoise;
import rtg.world.biome.RTGBiomeProvider;
import rtg.world.biome.realistic.RealisticBiomeBase;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(value = RealisticBiomeBase.class, remap = false)
public abstract class EndlessIdsMapGenBiomeMixin {
    @org.spongepowered.asm.mixin.Unique
    private static final AtomicBoolean dragonfixes$reportedMapGenCorrection = new AtomicBoolean();

    @WrapMethod(method = "generateMapGen")
    private void dragonfixes$deduplicateEndlessIdsMapGen(Block[] blocks, byte[] metadata, Long seed,
            World world, RTGBiomeProvider cmr, Random mapRand, int x, int z, OpenSimplexNoise simplex,
            CellNoise cell, float[] noise, Operation<Void> original) {
        RtgMapGenScope scope = RtgMapGenScope.current();
        RealisticBiomeBase self = (RealisticBiomeBase)(Object)this;
        if (scope != null && !scope.dispatching && scope.landscape != null
                && scope.x == x && scope.z == z && scope.landscape.noise == noise) {
            if (scope.remaining > 0 && scope.blocks == blocks && scope.metadata == metadata) {
                int expected = scope.landscape.biome[256 - scope.remaining].baseBiome.biomeID;
                if (self == RealisticBiomeBase.getBiome(expected)) {
                    --scope.remaining;
                    return;
                }
                // Unexpected caller: do not suppress further invocations.
                scope.remaining = 0;
            }
            if (!scope.examined) {
                scope.examined = true;
                if (scope.landscape.biome.length == 256 && RtgMapGenScope.calledFromEndlessIds()
                        && self == RealisticBiomeBase.getBiome(scope.landscape.biome[0].baseBiome.biomeID)) {
                    int[] ids = new int[256];
                    for (int i = 0; i < ids.length; i++) ids[i] = scope.landscape.biome[i].baseBiome.biomeID;
                    int[] distinct = RtgMapGenIds.uniqueSorted(ids);
                    scope.blocks = blocks;
                    scope.metadata = metadata;
                    scope.dispatching = true;
                    try {
                        for (int id : distinct) {
                            RealisticBiomeBase biome = RealisticBiomeBase.getBiome(id);
                            if (biome == self) {
                                original.call(blocks, metadata, seed, world, cmr, mapRand, x, z, simplex, cell, noise);
                            } else {
                                biome.generateMapGen(blocks, metadata, seed, world, cmr, mapRand, x, z, simplex, cell, noise);
                            }
                        }
                    } finally {
                        scope.dispatching = false;
                    }
                    scope.remaining = 255;
                    if (dragonfixes$reportedMapGenCorrection.compareAndSet(false, true)) {
                        DragonFixes.LOGGER.info("RTG/EndlessIDs: restored distinct-biome map generation (256 -> {} calls in first corrected chunk)", distinct.length);
                    }
                    return;
                }
            }
        }
        original.call(blocks, metadata, seed, world, cmr, mapRand, x, z, simplex, cell, noise);
    }
}
