package cc.unilock.dragonfixes.mixin.early.dragonapi;

import Reika.DragonAPI.Instantiable.Data.Immutable.BlockKey;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;
import Reika.DragonAPI.Instantiable.RayTracer;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.compat.ReikaNumericalSafety;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(value = RayTracer.class, remap = false)
public abstract class RayTracerNumericalSafetyMixin {
    @Shadow private double originX,originY,originZ,targetX,targetY,targetZ;
    @Shadow @Final private ArrayList<BlockKey> allowedOneTimeBlocks;
    @Shadow @Final private HashSet<Coordinate> blockRay;
    @Unique private static final AtomicBoolean dragonfixes$warned=new AtomicBoolean();

    @Inject(method = "isClearLineOfSight", at = @At("HEAD"), cancellable = true, require = 1)
    private void dragonfixes$boundRay(World world, CallbackInfoReturnable<Boolean> cir) {
        if (ReikaNumericalSafety.rayAllowed(originX,originY,originZ,targetX,targetY,targetZ)) return;
        // Fail closed, never award a line of sight for invalid geometry.
        // One-time ignores must be consumed even when the trace is rejected.
        allowedOneTimeBlocks.clear();
        blockRay.clear();
        if (dragonfixes$warned.compareAndSet(false,true))
            DragonFixes.LOGGER.warn("Rejected non-finite or excessive DragonAPI ray: ({}, {}, {}) -> ({}, {}, {})",
                    originX,originY,originZ,targetX,targetY,targetZ);
        cir.setReturnValue(false);
    }
}
