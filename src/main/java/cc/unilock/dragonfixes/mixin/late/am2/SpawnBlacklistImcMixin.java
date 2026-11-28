package cc.unilock.dragonfixes.mixin.late.am2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** AM2 2.0.2 uses a regex alternation instead of a literal pipe in dsb and bsb. */
@Pseudo
@Mixin(targets = "am2.AMCore", remap = false)
public abstract class SpawnBlacklistImcMixin {
    @ModifyArg(method = "onIMCReceived",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;split(Ljava/lang/String;)[Ljava/lang/String;"),
            index = 0, require = 0, remap = false)
    private String dragonfixes$literalBlacklistSeparator(String regex) {
        // Later AM2 releases already fix the parser; leave their regex intact.
        return "|".equals(regex) ? "\\|" : regex;
    }
}
