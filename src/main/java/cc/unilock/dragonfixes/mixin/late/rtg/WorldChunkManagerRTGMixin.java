package cc.unilock.dragonfixes.mixin.late.rtg;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtg.world.biome.realistic.RealisticBiomeBase;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import rtg.world.biome.WorldChunkManagerRTG;

@Mixin(value = WorldChunkManagerRTG.class, remap = false)
public abstract class WorldChunkManagerRTGMixin {
    @Shadow private float[] borderNoise;
    @Unique private boolean dragonfixes$loggedRawCliffs;

    @Inject(method = "getBiomeDataAt", at = @At("RETURN"), require = 1)
    private void dragonfixes$reportRawCliffs(int x, int z, CallbackInfoReturnable<RealisticBiomeBase> cir) {
        if (dragonfixes$loggedRawCliffs || ChromatiCraft.glowingcliffs == null) return;
        RealisticBiomeBase biome = cir.getReturnValue();
        if (biome != null && biome.baseBiome == ChromatiCraft.glowingcliffs) {
            dragonfixes$loggedRawCliffs = true;
            DragonFixes.LOGGER.info("RTG: biome manager returned Luminous Cliffs at {}, {}", x, z);
        }
    }

    // EndlessIDs expands this array, but does not extend the reset/check loop.
    @ModifyConstant(method = "isBorderlessAt", constant = @Constant(intValue = 256), require = 1)
    private int dragonfixes$scanEntireBorderArray(int original) {
        return borderNoise.length;
    }
}
