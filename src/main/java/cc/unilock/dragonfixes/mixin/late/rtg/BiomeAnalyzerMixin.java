package cc.unilock.dragonfixes.mixin.late.rtg;

import Reika.ChromatiCraft.ChromatiCraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.biome.BiomeGenBase;
import rtg.world.biome.BiomeAnalyzer;
import rtg.world.biome.realistic.RealisticBiomeBase;

@Mixin(value = BiomeAnalyzer.class, remap = false)
public abstract class BiomeAnalyzerMixin {
    @Shadow private boolean[] riverBiome;
    @Shadow private boolean[] beachBiome;
    @Shadow private int[] preferredBeach;
    @Shadow private int[] xyinverted;

    // The in-game name is Rainbow Stream, not Rainbow River.
    @Inject(method = "determineRiverBiomes", at = @At("RETURN"), require = 1)
    private void dragonfixes$classifyRainbowStream(CallbackInfo ci) {
        if (dragonfixes$hasWrapper(ChromatiCraft.rainbowRiver)) {
            riverBiome[ChromatiCraft.rainbowRiver.biomeID] = true;
        }
    }

    // RTG's name heuristic only recognizes "beach" and "mangrove".
    // This runs before determineLandBiomes, so Shores are not classified as inland.
    @Inject(method = "determineBeachBiomes", at = @At("RETURN"), require = 1)
    private void dragonfixes$classifyCliffShores(CallbackInfo ci) {
        if (dragonfixes$hasWrapper(ChromatiCraft.glowingcliffsEdge)) {
            beachBiome[ChromatiCraft.glowingcliffsEdge.biomeID] = true;
        }
    }

    @Inject(method = "determinePreferredBeaches", at = @At("RETURN"), require = 1)
    private void dragonfixes$preferCliffShores(CallbackInfo ci) {
        if (dragonfixes$hasWrapper(ChromatiCraft.glowingcliffs)
                && dragonfixes$hasWrapper(ChromatiCraft.glowingcliffsEdge)) {
            preferredBeach[ChromatiCraft.glowingcliffs.biomeID] = ChromatiCraft.glowingcliffsEdge.biomeID;
            preferredBeach[ChromatiCraft.glowingcliffsEdge.biomeID] = ChromatiCraft.glowingcliffsEdge.biomeID;
        }
    }

    // The native GenLayer already selects the parent-dependent Shores. Reika's terrain shaper
    // reads that same manager result, and distinguishes the main cliffs from their shores.
    // Do not let RTG's river/beach/ocean/lake repair turn either into another biome or invent
    // cliff-family columns outside that original footprint. Keep RTG's blended heights intact.
    @Inject(method = "newRepair", at = @At("RETURN"), require = 1)
    private void dragonfixes$preserveChromaBiomes(int[] genLayerBiomes, RealisticBiomeBase[] jitteredBiomes,
            int[] biomeNeighborhood, int neighborhoodSize, float[] noise, float[] riverStrength, CallbackInfo ci) {
        if (!dragonfixes$hasWrapper(ChromatiCraft.glowingcliffs)
                || !dragonfixes$hasWrapper(ChromatiCraft.glowingcliffsEdge)
                || !dragonfixes$hasWrapper(ChromatiCraft.rainbowRiver)) return;

        RealisticBiomeBase stream = RealisticBiomeBase.getBiome(ChromatiCraft.rainbowRiver.biomeID);
        for (int i = 0; i < jitteredBiomes.length; i++) {
            int id = genLayerBiomes[xyinverted[i]];
            RealisticBiomeBase original = RealisticBiomeBase.getBiome(id);
            RealisticBiomeBase repaired = jitteredBiomes[i];
            if (original == null || repaired == null || original == repaired) continue;

            if (dragonfixes$isCliff(original.baseBiome) || dragonfixes$isCliff(repaired.baseBiome)) {
                jitteredBiomes[i] = original;
            }

            // Preserve native streams, and RTG-generated rivers within Rainbow Forest, even
            // if later beach or ocean repair would have overwritten the river assignment.
            if (original.baseBiome == ChromatiCraft.rainbowRiver
                    || (original.baseBiome == ChromatiCraft.rainbowforest
                        && noise[i] <= 61.5f && riverStrength[i] > 0.7f)) {
                jitteredBiomes[i] = stream;
            }
        }
    }

    @Unique
    private static boolean dragonfixes$isCliff(BiomeGenBase biome) {
        return biome == ChromatiCraft.glowingcliffs || biome == ChromatiCraft.glowingcliffsEdge;
    }

    @Unique
    private static boolean dragonfixes$hasWrapper(BiomeGenBase biome) {
        return biome != null && RealisticBiomeBase.getBiome(biome.biomeID) != null;
    }
}
