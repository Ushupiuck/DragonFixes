package cc.unilock.dragonfixes.mixin.late.am2;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Pseudo
@Mixin(targets = "am2.ShrinkHandler", remap = false)
public abstract class ShrinkHandlerMixin {
    // ExtendedProperties.For(player) may return null for simulated or not-yet
    // initialized players. Inspect the existing local rather than doing another
    // properties lookup or excluding all fake players.
    @Inject(method = "onEntityLiving(Lnet/minecraftforge/event/entity/living/LivingEvent;)V",
            at = @At(value = "FIELD",
                    target = "Lam2/playerextensions/ExtendedProperties;originalSize:Lam2/api/math/AMVector2;",
                    opcode = Opcodes.GETFIELD, ordinal = 0),
            cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, require = 1)
    private void dragonfixes$guardMissingProperties(LivingEvent event, CallbackInfo ci,
            EntityPlayer player, @Coerce Object exProps) {
        if (exProps == null) {
            ci.cancel();
        }
    }
}
