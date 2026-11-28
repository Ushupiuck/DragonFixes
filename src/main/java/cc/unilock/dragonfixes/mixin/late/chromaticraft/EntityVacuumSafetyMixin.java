package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.Entity.EntityVacuum;
import cc.unilock.dragonfixes.compat.ReikaNumericalSafety;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EntityVacuum.class, remap = false)
public abstract class EntityVacuumSafetyMixin {
    @ModifyExpressionValue(method = "suck", at = @At(value = "INVOKE",
            target = "LReika/DragonAPI/Libraries/MathSci/ReikaMathLibrary;py3d(DDD)D"), require = 1)
    private double dragonfixes$definedZeroDistance(double distance) {
        // Zero direction contributes no radial impulse; gravity compensation,
        // final burst RNG, noClip and flare aggro still execute.
        return ReikaNumericalSafety.nonzeroDistance(distance);
    }

    @WrapMethod(method = "suck")
    private void dragonfixes$finiteSuction(Entity entity, Operation<Void> original) {
        double x=entity.motionX,y=entity.motionY,z=entity.motionZ;
        original.call(entity);
        entity.motionX=ReikaNumericalSafety.finiteOr(entity.motionX,x);
        entity.motionY=ReikaNumericalSafety.finiteOr(entity.motionY,y);
        entity.motionZ=ReikaNumericalSafety.finiteOr(entity.motionZ,z);
    }
}
