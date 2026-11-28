package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import net.minecraft.launchwrapper.Launch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blood Magic's old reflective API was removed in ChromatiCraft V33.
 * Leave its Torcherino registrations intact; only defer the obsolete
 * ChromatiCraft branch until both mods have completed initialization.
 */
@Pseudo
@Mixin(targets = "WayofTime.alchemicalWizardry.AlchemicalWizardry", remap = false)
public abstract class BloodMagicAcceleratorMixin {
    @Inject(method = "blacklistAccelerators", at = @At(value = "INVOKE",
            target = "Ljava/lang/Class;forName(Ljava/lang/String;)Ljava/lang/Class;",
            ordinal = 0), cancellable = true, require = 0, remap = false)
    private static void dragonfixes$deferLegacyBlacklist(CallbackInfo ci) {
        if (Launch.classLoader.getResource("Reika/ChromatiCraft/API/AcceleratorBlacklist.class") == null
                && Launch.classLoader.getResource("Reika/ChromatiCraft/API/AdjacencyUpgradeAPI.class") != null) {
            ci.cancel();
        }
    }
}
