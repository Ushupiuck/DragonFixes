package cc.unilock.dragonfixes.mixin.late.am2.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets = "am2.guis.AMIngameGUI", remap = false)
public abstract class PotionHudMixin {
    // The original AM2 used 198 + (index / 8) * 18, with integer division.
    // Legacy casts index to float before division, selecting a fractional row.
    // Snap only the source Y argument of the two buff icon draws to its row.
    // This also leaves older builds' already-correct coordinates unchanged.
    @ModifyArg(method = "RenderBuffs(II)V",
            at = @At(value = "INVOKE",
                    target = "Lam2/guis/AMIngameGUI;drawTexturedModalRect_Classic(FFFFFFFF)V"),
            index = 3, require = 2, allow = 2)
    private float dragonfixes$alignPotionIconRow(float sourceY) {
        return 198.0F + (int) ((sourceY - 198.0F) / 18.0F) * 18.0F;
    }
}
