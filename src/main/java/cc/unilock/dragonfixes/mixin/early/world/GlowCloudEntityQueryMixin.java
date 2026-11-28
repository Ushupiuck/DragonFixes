package cc.unilock.dragonfixes.mixin.early.world;

import cc.unilock.dragonfixes.compat.GlowCloudCollisionSafety;
import cc.unilock.dragonfixes.compat.GlowCloudPhysics;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(World.class)
public abstract class GlowCloudEntityQueryMixin {
    // func_94576_a: the exact innermost method in both stall thread dumps.
    // Keep the normal query (including all Forge/mod behavior) for valid
    // bounds. Do not impose a global radius restriction on other entities.
    @Inject(method = "getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/command/IEntitySelector;)Ljava/util/List;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void dragonfixes$boundCloudQuery(Entity entity, AxisAlignedBB box, IEntitySelector selector,
            CallbackInfoReturnable<List<Entity>> cir) {
        if (!(entity instanceof GlowCloudCollisionSafety) || box == null) return;
        if (!GlowCloudPhysics.validQuery(box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ)) {
            ((GlowCloudCollisionSafety)entity).dragonfixes$recoverInvalidCollisionQuery(box);
            // The rejected AABB is unusable; the entity resumes normal queries
            // on its repaired position. Return a mutable list, like vanilla.
            cir.setReturnValue(new ArrayList<>());
        }
    }
}
