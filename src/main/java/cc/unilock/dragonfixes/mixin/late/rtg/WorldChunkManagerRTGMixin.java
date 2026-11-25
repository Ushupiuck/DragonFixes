package cc.unilock.dragonfixes.mixin.late.rtg;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import rtg.world.biome.WorldChunkManagerRTG;

@Mixin(value = WorldChunkManagerRTG.class, remap = false)
public abstract class WorldChunkManagerRTGMixin {
    @Shadow private float[] borderNoise;

    // EndlessIDs expands this array, but does not extend the reset/check loop.
    @ModifyConstant(method = "isBorderlessAt", constant = @Constant(intValue = 256), require = 1)
    private int dragonfixes$scanEntireBorderArray(int original) {
        return borderNoise.length;
    }
}
