package cc.unilock.dragonfixes.compat;

import net.minecraft.util.AxisAlignedBB;

/** Implemented only by the optional ChromatiCraft entity mixin. */
public interface GlowCloudCollisionSafety {
    void dragonfixes$recoverInvalidCollisionQuery(AxisAlignedBB query);
}
