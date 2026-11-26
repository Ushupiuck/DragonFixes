package cc.unilock.dragonfixes.mixin.late.dragonapi.accessor;

import Reika.DragonAPI.Auxiliary.WorldGenInterceptionRegistry;
import Reika.DragonAPI.Auxiliary.WorldGenInterceptionRegistry.BlockSetData;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = BlockSetData.class, remap = false)
public interface BlockSetDataInvoker {
    // Non-static inner classes have a synthetic enclosing-instance argument.
    @Invoker("<init>")
    static BlockSetData dragonfixes$create(WorldGenInterceptionRegistry owner,
            Coordinate coordinate, Block oldBlock, int oldMeta, Block newBlock, int newMeta) {
        throw new AssertionError();
    }
}
