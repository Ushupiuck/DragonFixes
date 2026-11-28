package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.Entity.EntityChainGunShot;
import cc.unilock.dragonfixes.compat.ReikaNumericalSafety;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = EntityChainGunShot.class, remap = false)
public abstract class EntityChainGunShotSafetyMixin extends Entity {
    protected EntityChainGunShotSafetyMixin(World world) { super(world); }

    @WrapMethod(method = "targetEntity")
    private void dragonfixes$finiteRetarget(EntityLivingBase target, Operation<Void> original) {
        double x=motionX,y=motionY,z=motionZ;
        original.call(target);
        // A coincident target has no direction. Keep the previous trajectory
        // rather than changing target selection, RNG or immunity timing.
        motionX=ReikaNumericalSafety.finiteOr(motionX,x);
        motionY=ReikaNumericalSafety.finiteOr(motionY,y);
        motionZ=ReikaNumericalSafety.finiteOr(motionZ,z);
    }
}
