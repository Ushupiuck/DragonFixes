package cc.unilock.dragonfixes.mixin.late.climatecontrol;

import cc.unilock.dragonfixes.compat.ChromaBiomeEvents;
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
    private void dragonfixes$forwardChromaRivers(int x, int z, int width, int height,
            CallbackInfoReturnable<int[]> cir, @Local(ordinal = 0) int[] parentBiomes) {
        int[] result = cir.getReturnValue();
        for (int i = 0; i < width * height; i++) {
            // Do not re-enable rivers suppressed by Climate Control, including RTG worlds.
            int planned = result[i];
            if (planned != BiomeGenBase.river.biomeID && planned != BiomeGenBase.frozenRiver.biomeID) continue;
            BiomeGenBase parent = BiomeGenBase.getBiome(parentBiomes[i]);
            result[i] = ChromaBiomeEvents.river(parent, BiomeGenBase.getBiome(planned)).biomeID;
        }
    }
}
