package cc.unilock.dragonfixes.compat;

import net.minecraft.world.World;
import rtg.world.gen.LandscapeGenerator;

public interface RtgLandscapeAccess {
    World dragonfixes$getWorld();
    LandscapeGenerator dragonfixes$getLandscape();
}
