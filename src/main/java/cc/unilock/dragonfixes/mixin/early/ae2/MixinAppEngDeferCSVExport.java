package cc.unilock.dragonfixes.mixin.early.ae2;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cc.unilock.dragonfixes.compat.AE2CSVExportBridge;

/**
 * Only change AE2's optional startup CSV export service; all other AE2
 * workers and mod lifecycle behavior remain as implemented by GTNH.
 */
@Mixin(targets = "appeng.core.AppEng", remap = false)
public abstract class MixinAppEngDeferCSVExport {
    @Inject(
            method = "startService(Ljava/lang/String;Ljava/lang/Thread;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false)
    private void dragonfixes$deferCSVExport(String serviceName, Thread thread, CallbackInfo ci) {
        if ("AE2 CSV Export".equals(serviceName)
                && AE2CSVExportBridge.defer(thread, serviceName)) {
            ci.cancel();
        }
    }
}
