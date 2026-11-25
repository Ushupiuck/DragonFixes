package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.Magic.Network.LOSData;
import Reika.ChromatiCraft.Magic.Network.PylonFinder;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;
import Reika.DragonAPI.Instantiable.RayTracer;
import cc.unilock.dragonfixes.mixin.late.chromaticraft.accessor.RayTracerAccessor;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Mixin(value = PylonFinder.class, remap = false)
public class PylonFinderMixin {
    @Shadow @Final private static RayTracer tracer;

    // PylonFinder configures the original tracer once in its static initializer.
    // Its origins, one-time ignores and ray-block set are mutable per trace.
    // Keep these on the calling thread rather than serializing world access.
    @Unique private static final ThreadLocal<RayTracer> dragonfixes$reusable = new ThreadLocal<>();
    @Unique private static final ThreadLocal<RayTracer> dragonfixes$active = new ThreadLocal<>();

    @Unique
    private static RayTracer dragonfixes$copyTracer() {
        RayTracer copy = new RayTracer(0, 0, 0, 0, 0, 0);
        copy.airOnly = tracer.airOnly;
        copy.softBlocksOnly = tracer.softBlocksOnly;
        copy.allowFluids = tracer.allowFluids;
        copy.uniDirectionalChecks = tracer.uniDirectionalChecks;
        copy.cacheBlockRay = tracer.cacheBlockRay;
        RayTracerAccessor from = (RayTracerAccessor) (Object) tracer;
        RayTracerAccessor to = (RayTracerAccessor) (Object) copy;
        to.dragonfixes$getAllowedBlocks().addAll(from.dragonfixes$getAllowedBlocks());
        to.dragonfixes$getForbiddenBlocks().addAll(from.dragonfixes$getForbiddenBlocks());
        return copy;
    }

    @WrapMethod(method = "lineOfSight(Lnet/minecraft/world/World;IIIIII[Lnet/minecraft/block/Block;)LReika/ChromatiCraft/Magic/Network/LOSData;")
    private static LOSData dragonfixes$isolatedTrace(World world, int x1, int y1, int z1, int x2, int y2, int z2,
            Block[] extra, Operation<LOSData> original) {
        RayTracer previous = dragonfixes$active.get();
        RayTracer current;
        if (previous != null) {
            // A world lookup may reenter this method on the same thread.
            current = dragonfixes$copyTracer();
        } else {
            current = dragonfixes$reusable.get();
            if (current == null) {
                current = dragonfixes$copyTracer();
                dragonfixes$reusable.set(current);
            }
        }
        dragonfixes$active.set(current);
        try {
            return original.call(world, x1, y1, z1, x2, y2, z2, extra);
        } finally {
            // LOSData holds a snapshot, so the scratch set can be cleared now.
            current.setOrigins(0, 0, 0, 0, 0, 0);
            ((RayTracerAccessor) (Object) current).dragonfixes$getOneTimeBlocks().clear();
            if (previous == null) dragonfixes$active.remove();
            else dragonfixes$active.set(previous);
        }
    }

    @ModifyExpressionValue(method = {
            "lineOfSight(Lnet/minecraft/world/World;IIIIII[Lnet/minecraft/block/Block;)LReika/ChromatiCraft/Magic/Network/LOSData;",
            "isBlockPassable"},
            at = @At(value = "FIELD",
                    target = "LReika/ChromatiCraft/Magic/Network/PylonFinder;tracer:LReika/DragonAPI/Instantiable/RayTracer;",
                    opcode = Opcodes.GETSTATIC), require = 1)
    private static RayTracer dragonfixes$useActiveTracer(RayTracer original) {
        RayTracer current = dragonfixes$active.get();
        return current != null ? current : original;
    }

    // getRayBlocks returns a live unmodifiable view, not a snapshot. Copy once
    // for BOTH the rain check and the returned LOSData. No CME retry is needed.
    @WrapOperation(method = "lineOfSight(Lnet/minecraft/world/World;IIIIII[Lnet/minecraft/block/Block;)LReika/ChromatiCraft/Magic/Network/LOSData;",
            at = @At(value = "INVOKE", target = "LReika/DragonAPI/Instantiable/RayTracer;getRayBlocks()Ljava/util/Set;"),
            require = 1)
    private static Set<Coordinate> dragonfixes$snapshotRay(RayTracer instance, Operation<Set<Coordinate>> original) {
        return Collections.unmodifiableSet(new HashSet<>(original.call(instance)));
    }
}
