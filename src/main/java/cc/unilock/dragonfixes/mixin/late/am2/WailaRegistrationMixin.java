package cc.unilock.dragonfixes.mixin.late.am2;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Retarget the orphaned AM2 Waila callback, preserving a native one if present. */
@Mixin(targets = "am2.AMCore", remap = false)
public abstract class WailaRegistrationMixin {
    @ModifyArg(method = "init", at = @At(value = "INVOKE",
            target = "Lcpw/mods/fml/common/event/FMLInterModComms;sendMessage(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z"),
            index = 2, require = 0, remap = false)
    private String dragonfixes$restoreWailaCallback(String callback) {
        if ("am2.interop.WailaSupport.callbackRegister".equals(callback)
                && Launch.classLoader.getResource("am2/interop/WailaSupport.class") == null) {
            return "cc.unilock.dragonfixes.compat.am2.AM2WailaCompat.callbackRegister";
        }
        return callback;
    }
}
