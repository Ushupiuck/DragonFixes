package cc.unilock.dragonfixes.mixin.late.climatecontrol;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import climateControl.customGenLayer.GenLayerBiomeByClimate;
import climateControl.customGenLayer.GenLayerBiomeByTaggedClimate;
import climateControl.customGenLayer.GenLayerRandomBiomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GenLayerBiomeByClimate.class, GenLayerBiomeByTaggedClimate.class, GenLayerRandomBiomes.class})
public abstract class GenLayerBiomeSelectionMixin {
    @Unique private boolean dragonfixes$loggedCliffs;

    // Check the actual output of the primary biome selector, before zooms,
    // shores, locking, Voronoi and RTG's terrain repair.
    @Inject(method = "getInts", at = @At("RETURN"), require = 1)
    private void dragonfixes$reportCliffSelection(int x, int z, int width, int height,
            CallbackInfoReturnable<int[]> cir) {
        if (dragonfixes$loggedCliffs || ChromatiCraft.glowingcliffs == null) return;
        int id = ChromatiCraft.glowingcliffs.biomeID;
        int[] result = cir.getReturnValue();
        for (int i = 0; i < width * height; i++) {
            if (result[i] == id) {
                dragonfixes$loggedCliffs = true;
                DragonFixes.LOGGER.info("Climate Control: selected Luminous Cliffs (ID {}) at biome-layer cell {}, {}",
                        id, x + i % width, z + i / width);
                return;
            }
        }
    }
}
