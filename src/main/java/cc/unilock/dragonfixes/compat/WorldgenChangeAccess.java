package cc.unilock.dragonfixes.compat;

import Reika.DragonAPI.Auxiliary.WorldGenInterceptionRegistry.BlockSetData;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;

public interface WorldgenChangeAccess {
    WorldgenChangeBuffer<Coordinate, BlockSetData> dragonfixes$worldgenChanges();
}
