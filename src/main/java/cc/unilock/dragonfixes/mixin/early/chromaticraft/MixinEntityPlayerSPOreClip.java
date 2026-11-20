package cc.unilock.dragonfixes.mixin.early.chromaticraft;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import Reika.ChromatiCraft.Auxiliary.ChromaAux;

/**
 * ChromatiCraft's ENTITYPUSHOUT ASM assumes PlayerAPI supplies
 * localPushOutOfBlocks whenever its transformer class exists.
 *
 * Preserve OreClip/noClip behavior without depending on that method name:
 * when phasing is active, suppress the vanilla collision push-out entirely.
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSPOreClip {

    @Inject(
            method = "func_145771_j(DDD)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1)
    private void dragonfixes$skipPushoutWhenPhasing(
            double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (ChromaAux.applyNoclipPhase((EntityPlayer) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
