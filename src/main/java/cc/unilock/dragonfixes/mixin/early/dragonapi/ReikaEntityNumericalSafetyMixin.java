package cc.unilock.dragonfixes.mixin.early.dragonapi;

import Reika.DragonAPI.Libraries.ReikaEntityHelper;
import cc.unilock.dragonfixes.compat.ReikaNumericalSafety;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraftforge.common.util.ForgeDirection;

@Mixin(value = ReikaEntityHelper.class, remap = false)
public abstract class ReikaEntityNumericalSafetyMixin {
    @WrapMethod(method = "knockbackEntityFromPos(DDDLnet/minecraft/entity/Entity;DDD)V")
    private static void dragonfixes$finiteKnockback(double x,double y,double z,Entity entity,
            double power,double exponent,double yscale,Operation<Void> original) {
        double dx=x-entity.posX,dz=z-entity.posZ;
        double distance=Math.sqrt(dx*dx+dz*dz);
        if (!ReikaNumericalSafety.finite(x,y,z) || !Double.isFinite(distance)
                || !Double.isFinite(power) || !Double.isFinite(exponent)) return;
        double effective=ReikaNumericalSafety.scaledPower(distance,power,exponent);
        double oldX=entity.motionX,oldY=entity.motionY,oldZ=entity.motionZ;
        if (distance > 0D && Double.isFinite(effective)
                && (exponent <= 0D || effective == power/Math.pow(distance,exponent))) {
            original.call(x,y,z,entity,power,exponent,yscale);
        } else {
            // Undefined horizontal direction: no horizontal impulse. Retain
            // the ordinary unscaled upward push, but not a singular scaled one.
            if (distance > 0D && Double.isFinite(effective)) {
                entity.motionX-=dx/distance/2*effective;
                entity.motionZ-=dz/distance/2*effective;
            }
            if (Double.isFinite(effective) && (entity.onGround || entity.posY > y))
                entity.motionY+=0.4*effective;
            entity.velocityChanged=true;
        }
        entity.motionX=ReikaNumericalSafety.finiteOr(entity.motionX,oldX);
        entity.motionY=ReikaNumericalSafety.finiteOr(entity.motionY,oldY);
        entity.motionZ=ReikaNumericalSafety.finiteOr(entity.motionZ,oldZ);
    }

    @Inject(method = "getDirectionFromEntityLook", at = @At("HEAD"), cancellable = true, require = 1)
    private static void dragonfixes$constantTimeDirection(EntityLivingBase e,boolean vertical,
            CallbackInfoReturnable<ForgeDirection> cir) {
        if (MathHelper.abs(e.rotationPitch) < 60 || !vertical) {
            int i=ReikaNumericalSafety.cardinalIndex(MathHelper.floor_double((e.rotationYaw*4F)/360F+0.5D));
            switch(i) {
                case 0: cir.setReturnValue(ForgeDirection.SOUTH); break;
                case 1: cir.setReturnValue(ForgeDirection.WEST); break;
                case 2: cir.setReturnValue(ForgeDirection.NORTH); break;
                case 3: cir.setReturnValue(ForgeDirection.EAST); break;
            }
        }
        // Vertical branch remains the original implementation.
    }
}
