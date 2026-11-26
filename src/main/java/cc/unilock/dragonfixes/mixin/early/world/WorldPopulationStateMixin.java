package cc.unilock.dragonfixes.mixin.early.world;

import Reika.DragonAPI.Auxiliary.WorldGenInterceptionRegistry.BlockSetData;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;
import cc.unilock.dragonfixes.compat.WorldgenChangeAccess;
import cc.unilock.dragonfixes.compat.WorldgenChangeBuffer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(World.class)
public abstract class WorldPopulationStateMixin implements WorldgenChangeAccess {
    @Unique
    private final WorldgenChangeBuffer<Coordinate, BlockSetData> dragonfixes$population =
            new WorldgenChangeBuffer<>();

    @Override
    public WorldgenChangeBuffer<Coordinate, BlockSetData> dragonfixes$worldgenChanges() {
        return dragonfixes$population;
    }
}
