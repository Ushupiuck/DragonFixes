package cc.unilock.dragonfixes.mixin.late.climatecontrol;

import Reika.ChromatiCraft.ChromatiCraft;
import Reika.DragonAPI.Instantiable.Event.GenLayerBeachEvent;
import cc.unilock.dragonfixes.compat.ClimateControlChromaBiomes;
import climateControl.customGenLayer.GenLayerPrettyShore;
import climateControl.customGenLayer.GenLayerShoreCC;
import climateControl.genLayerPack.GenLayerShore;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// getInts overrides the Minecraft GenLayer method and must be remapped.
@Mixin({GenLayerPrettyShore.class, GenLayerShoreCC.class, GenLayerShore.class})
public abstract class GenLayerShoreMixin {
    @Inject(method = "getInts", at = @At("RETURN"), require = 1)
    private void dragonfixes$forwardCliffBeaches(int x, int z, int width, int height,
            CallbackInfoReturnable<int[]> cir, @Local(ordinal = 0) int[] parentBiomes) {
        if (ChromatiCraft.glowingcliffs == null || ChromatiCraft.glowingcliffsEdge == null) return;
        int cliffs = ChromatiCraft.glowingcliffs.biomeID;
        int[] result = cir.getReturnValue();
        int parentWidth = width + 2;

        // Preserve Climate Control's coastline placement. Forward only actual
        // beach replacements of Luminous Cliffs to Reika's native event handler.
        for (int dz = 0; dz < height; dz++) {
            for (int dx = 0; dx < width; dx++) {
                int i = dx + dz * width;
                if (parentBiomes[dx + 1 + (dz + 1) * parentWidth] == cliffs
                        && ClimateControlChromaBiomes.isVanillaBeach(result[i])) {
                    result[i] = GenLayerBeachEvent.fire(ChromatiCraft.glowingcliffs, result[i]);
                }
            }
        }
    }
}
