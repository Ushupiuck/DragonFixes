package cc.unilock.dragonfixes.mixin.late.chromaticraft.client;

import Reika.ChromatiCraft.ChromaClientEventController;
import cc.unilock.dragonfixes.DragonFixes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = ChromaClientEventController.class, remap = false)
public class ChromaClientEventControllerMixin {
    @Unique private boolean dragonfixes$reportedGlowCliffFailure;
    @Unique private long dragonfixes$lastGlowCliffWarning;
    /**
     * @author thegamemaster1234
     * @reason catch Exception
     */
    @WrapMethod(method = "updateGlowCliffRendering")
    private void updateGlowCliffRendering(TickEvent.ClientTickEvent evt, Operation<Void> original) {
        try {
            original.call(evt);
        } catch (Exception ex) {
            // Keep the existing crash fallback, but report the actual cause and
            // avoid writing an exception to the log on every client tick.
            long now = System.nanoTime();
            if (!dragonfixes$reportedGlowCliffFailure || now - dragonfixes$lastGlowCliffWarning >= 60_000_000_000L) {
                dragonfixes$reportedGlowCliffFailure = true;
                dragonfixes$lastGlowCliffWarning = now;
                DragonFixes.LOGGER.warn("Exception caught in ChromaClientEventController#updateGlowCliffRendering (repeats limited to once per minute)", ex);
            }
        }
    }
}
