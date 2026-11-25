package cc.unilock.dragonfixes.mixin.late.climatecontrol;

import Reika.ChromatiCraft.ChromatiCraft;
import climateControl.customGenLayer.GenLayerLowlandRiverMix;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.biome.BiomeGenBase;

@Mixin(GenLayerLowlandRiverMix.class)
public abstract class GenLayerLowlandRiverMixMixin {
    @Inject(method = "getInts", at = @At("RETURN"), require = 1)
    private void dragonfixes$replaceChromaRivers(int x, int z, int width, int height,
            CallbackInfoReturnable<int[]> cir, @Local(ordinal = 0) int[] parentBiomes) {
        if (ChromatiCraft.glowingcliffs == null || ChromatiCraft.glowingcliffsEdge == null
                || ChromatiCraft.rainbowforest == null || ChromatiCraft.rainbowRiver == null) return;
        int[] result = cir.getReturnValue();
        int cliffs = ChromatiCraft.glowingcliffs.biomeID;
        int shores = ChromatiCraft.glowingcliffsEdge.biomeID;
        int forest = ChromatiCraft.rainbowforest.biomeID;
        int stream = ChromatiCraft.rainbowRiver.biomeID;

        for (int i = 0; i < width * height; i++) {
            // Do not override Climate Control's decision to omit a river, for example in RTG.
            if (result[i] != BiomeGenBase.river.biomeID && result[i] != BiomeGenBase.frozenRiver.biomeID) continue;
            int parent = parentBiomes[i];
            if (parent == cliffs || parent == shores) {
                result[i] = parent;
            } else if (parent == forest) {
                result[i] = stream;
            }
        }
    }
}
