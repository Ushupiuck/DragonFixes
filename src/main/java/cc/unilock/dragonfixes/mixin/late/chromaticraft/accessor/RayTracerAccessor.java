package cc.unilock.dragonfixes.mixin.late.chromaticraft.accessor;

import Reika.DragonAPI.Instantiable.Data.Immutable.BlockKey;
import Reika.DragonAPI.Instantiable.RayTracer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.ArrayList;

@Mixin(value = RayTracer.class, remap = false)
public interface RayTracerAccessor {
    @Accessor("allowedBlocks")
    ArrayList<BlockKey> dragonfixes$getAllowedBlocks();

    @Accessor("forbiddenBlocks")
    ArrayList<BlockKey> dragonfixes$getForbiddenBlocks();

    @Accessor("allowedOneTimeBlocks")
    ArrayList<BlockKey> dragonfixes$getOneTimeBlocks();
}
