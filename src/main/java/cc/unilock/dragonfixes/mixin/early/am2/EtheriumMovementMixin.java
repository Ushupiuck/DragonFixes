package cc.unilock.dragonfixes.mixin.early.am2;

import cc.unilock.dragonfixes.compat.am2.EtheriumCompat;
import cc.unilock.dragonfixes.compat.am2.EtheriumPhysics;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityLivingBase.class)
public abstract class EtheriumMovementMixin {
    @Unique private boolean dragonfixes$wasInEtherium;
    @Unique private boolean dragonfixes$travelScoped;
    @Unique private boolean dragonfixes$travelInEtherium;
    @Unique private boolean dragonfixes$usedEtheriumBranch;

    // Preserve virtual travel dispatch (player statistics, modded movers, etc.).
    // Scope the material query to this invocation, including reentrant travel.
    @WrapOperation(method = "onLivingUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;moveEntityWithHeading(FF)V"),
            require = 1)
    private void dragonfixes$travel(EntityLivingBase entity, float strafe, float forward, Operation<Void> original) {
        if (!EtheriumCompat.enabled()) {
            original.call(entity, strafe, forward);
            return;
        }
        boolean immersed = EtheriumCompat.isImmersed(entity);
        boolean entered = immersed && !dragonfixes$wasInEtherium;
        dragonfixes$wasInEtherium = immersed;
        boolean previousScope = dragonfixes$travelScoped;
        boolean previousContact = dragonfixes$travelInEtherium;
        boolean previousUsed = dragonfixes$usedEtheriumBranch;
        dragonfixes$travelScoped = true;
        dragonfixes$travelInEtherium = immersed;
        dragonfixes$usedEtheriumBranch = false;
        try {
            double startY = entity.posY;
            if (immersed) {
                EtheriumCompat.protect(entity);
                EtheriumCompat.effects(entity, entered);
            }

            original.call(entity, strafe, forward);

            if (immersed) {
                // Flying/special modded living entities can override the entire
                // native travel method. Do not move them twice or erase their
                // custom controller: apply the passive fluid forces once.
                if (!dragonfixes$usedEtheriumBranch && EtheriumCompat.canSwim(entity)
                        && !entity.isInWater()) {
                    entity.motionX *= EtheriumPhysics.HORIZONTAL_DRAG;
                    entity.motionZ *= EtheriumPhysics.HORIZONTAL_DRAG;
                    entity.motionY = EtheriumPhysics.verticalVelocity(entity.motionY);
                    if (entity.isCollidedHorizontally && entity.isOffsetPositionInLiquid(entity.motionX,
                            entity.motionY + 0.6000000238418579D - entity.posY + startY, entity.motionZ)) {
                        entity.motionY = 0.30000001192092896D;
                    }
                }
                EtheriumCompat.protect(entity);
            }
        } finally {
            dragonfixes$travelScoped = previousScope;
            dragonfixes$travelInEtherium = previousContact;
            dragonfixes$usedEtheriumBranch = previousUsed;
        }
    }

    // Enter the native swimming branch without changing isInWater globally:
    // Etherium must not become water for drowning, potions or affinity checks.
    @WrapOperation(method = "moveEntityWithHeading",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInWater()Z"),
            require = 1)
    private boolean dragonfixes$swimBranch(EntityLivingBase entity, Operation<Boolean> original,
            @Share("etherium") LocalBooleanRef etherium) {
        boolean water = original.call(entity);
        boolean custom = !water && EtheriumCompat.canSwim(entity)
                && (dragonfixes$travelScoped ? dragonfixes$travelInEtherium : EtheriumCompat.isImmersed(entity));
        etherium.set(custom);
        if (custom) {
            dragonfixes$usedEtheriumBranch = true;
            EtheriumCompat.protect(entity);
        }
        return water || custom;
    }

    @ModifyExpressionValue(method = "moveEntityWithHeading",
            at = {
                @At(value = "CONSTANT", args = "doubleValue=0.800000011920929", ordinal = 0),
                @At(value = "CONSTANT", args = "doubleValue=0.800000011920929", ordinal = 2)
            },
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInWater()Z"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;handleLavaMovement()Z")),
            require = 2)
    private double dragonfixes$horizontalDrag(double original, @Share("etherium") LocalBooleanRef etherium) {
        return etherium.get() ? EtheriumPhysics.HORIZONTAL_DRAG : original;
    }

    @ModifyExpressionValue(method = "moveEntityWithHeading",
            at = @At(value = "CONSTANT", args = "doubleValue=0.800000011920929", ordinal = 1),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInWater()Z"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;handleLavaMovement()Z")),
            require = 1)
    private double dragonfixes$verticalDrag(double original, @Share("etherium") LocalBooleanRef etherium) {
        return etherium.get() ? EtheriumPhysics.VERTICAL_DRAG : original;
    }

    @ModifyExpressionValue(method = "moveEntityWithHeading",
            at = @At(value = "CONSTANT", args = "doubleValue=0.02"),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInWater()Z"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;handleLavaMovement()Z")),
            require = 1)
    private double dragonfixes$lift(double original, @Share("etherium") LocalBooleanRef etherium) {
        // Vanilla subtracts gravity after drag. Subtracting a negative lift
        // produces the historical +0.05 upward force, without a second move.
        return etherium.get() ? -EtheriumPhysics.LIFT : original;
    }

    @WrapOperation(method = "onLivingUpdate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInWater()Z"),
            require = 1)
    private boolean dragonfixes$swimJump(EntityLivingBase entity, Operation<Boolean> original) {
        return original.call(entity) || (EtheriumCompat.enabled() && EtheriumCompat.canSwim(entity)
                && EtheriumCompat.isImmersed(entity));
    }

    @Inject(method = "fall", at = @At("HEAD"), cancellable = true, require = 1)
    private void dragonfixes$cushionImpact(float distance, CallbackInfo ci) {
        EntityLivingBase entity = (EntityLivingBase) (Object) this;
        if (EtheriumCompat.enabled() && EtheriumCompat.touches(entity)) {
            EtheriumCompat.protect(entity);
            ci.cancel();
        }
    }
}
