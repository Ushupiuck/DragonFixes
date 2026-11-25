package cc.unilock.dragonfixes.mixin.late.rwg1;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.compat.ChromaBiomeEvents;
import cc.unilock.dragonfixes.compat.Rwg1ChromaBiomes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.biome.BiomeGenBase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rwg.biomes.realistic.RealisticBiomeBase;
import rwg.world.ChunkGeneratorRealistic;
import rwg.world.ChunkManagerRealistic;

@Mixin(value = ChunkGeneratorRealistic.class, remap = false)
public abstract class ChunkGeneratorRealisticMixin {
    @Unique private boolean dragonfixes$loggedCliffs;
    @Unique private boolean dragonfixes$loggedShores;
    @Unique private boolean dragonfixes$loggedStream;

    // The native Chroma cliff shaper consults the manager, not RWG1's blended selection.
    // Keep the biome IDs consistent with that shaper without altering blended heights.
    @Inject(method = "getNewNoise", at = @At("RETURN"), require = 1)
    private void dragonfixes$alignCliffBiomes(ChunkManagerRealistic manager, int x, int z,
            RealisticBiomeBase[] biomes, CallbackInfoReturnable<float[]> cir) {
        if (Rwg1ChromaBiomes.cliffs == null || Rwg1ChromaBiomes.shores == null) return;
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int index = localZ * 16 + localX;
                RealisticBiomeBase predicted = manager.getBiomeDataAt(x + localX, z + localZ);
                if (Rwg1ChromaBiomes.isCliff(predicted) || Rwg1ChromaBiomes.isCliff(biomes[index])) {
                    if (predicted != null) biomes[index] = predicted;
                }
            }
        }
    }

    @WrapOperation(method = "replaceBlocksForBiome",
            at = @At(value = "FIELD", target = "Lrwg/biomes/realistic/RealisticBiomeBase;riverBiome:Lnet/minecraft/world/biome/BiomeGenBase;",
                    opcode = Opcodes.GETFIELD), require = 1)
    private BiomeGenBase dragonfixes$forwardRiverEvent(RealisticBiomeBase biome, Operation<BiomeGenBase> original) {
        return ChromaBiomeEvents.river(biome.baseBiome, original.call(biome));
    }

    @Inject(method = "replaceBlocksForBiome", at = @At("RETURN"), require = 1)
    private void dragonfixes$reportFinalBiomes(int cx, int cz, net.minecraft.block.Block[] blocks, byte[] metadata,
            RealisticBiomeBase[] biomes, BiomeGenBase[] base, float[] heights, CallbackInfo ci) {
        if (dragonfixes$loggedCliffs && dragonfixes$loggedShores && dragonfixes$loggedStream) return;
        for (BiomeGenBase biome : base) {
            if (biome == null) continue;
            if (!dragonfixes$loggedCliffs && biome == ChromatiCraft.glowingcliffs) {
                dragonfixes$loggedCliffs = true;
                DragonFixes.LOGGER.info("RWG1: generating Luminous Cliffs at chunk {}, {}", cx, cz);
            }
            if (!dragonfixes$loggedShores && biome == ChromatiCraft.glowingcliffsEdge) {
                dragonfixes$loggedShores = true;
                DragonFixes.LOGGER.info("RWG1: generating Luminous Cliffs Shores at chunk {}, {}", cx, cz);
            }
            if (!dragonfixes$loggedStream && biome == ChromatiCraft.rainbowRiver) {
                dragonfixes$loggedStream = true;
                DragonFixes.LOGGER.info("RWG1: generating Rainbow Stream at chunk {}, {}", cx, cz);
            }
        }
    }
}
