package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.Entity.EntityDeathFog;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.compat.ReikaNumericalSafety;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntityDeathFog.class)
public abstract class EntityDeathFogSafetyMixin {
    @Unique private boolean dragonfixes$reportedEscape;

    // The first query precedes the loop; each subsequent query follows one
    // original 0.1-block rise. Preserve all successful searches verbatim.
    @WrapOperation(method = "onUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getCollidingBoundingBoxes(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;"),
            require = 2)
    private List<AxisAlignedBB> dragonfixes$budgetEscape(World world, Entity entity, AxisAlignedBB box,
            Operation<List<AxisAlignedBB>> original, @Share("escapeQueries") LocalIntRef queries,
            @Share("escapeDeferred") LocalBooleanRef deferred) {
        List<AxisAlignedBB> collisions = original.call(world,entity,box);
        int count=queries.get()+1;
        queries.set(count);
        if (collisions.isEmpty()) {
            dragonfixes$reportedEscape=false;
            return collisions;
        }
        if (count > ReikaNumericalSafety.FOG_ESCAPE_STEPS) {
            deferred.set(true);
            if (!dragonfixes$reportedEscape) {
                dragonfixes$reportedEscape=true;
                DragonFixes.LOGGER.warn("Death Fog collision escape deferred after {} steps: entity={}, dimension={}, position=({}, {}, {})",
                        ReikaNumericalSafety.FOG_ESCAPE_STEPS, entity.getEntityId(), world.provider.dimensionId,
                        entity.posX,entity.posY,entity.posZ);
            }
            // Exit only the escape loop. Lifetime bookkeeping still runs.
            return new ArrayList<>();
        }
        return collisions;
    }

    // Original code attacks only once escape is complete. Do not attack
    // through a wall merely because the current tick ran out of work budget.
    @WrapOperation(method = "onUpdate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/command/IEntitySelector;)Ljava/util/List;"),
            require = 1)
    private List<Entity> dragonfixes$deferAttack(World world, Entity entity, AxisAlignedBB box,
            IEntitySelector selector, Operation<List<Entity>> original,
            @Share("escapeDeferred") LocalBooleanRef deferred) {
        return deferred.get() ? new ArrayList<>() : original.call(world,entity,box,selector);
    }
}
