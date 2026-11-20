package cc.unilock.dragonfixes.compat;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * ForgeDirection.UNKNOWN has ordinal 6 but is deliberately excluded from
 * VALID_DIRECTIONS. Simulated block placement is allowed to use that side.
 */
public final class PlayerPlaceBlockSideBridge {
    private PlayerPlaceBlockSideBridge() {}

    public static ForgeDirection lookup(ForgeDirection[] validDirections, int side) {
        return validDirections != null && side >= 0 && side < validDirections.length
                ? validDirections[side] : ForgeDirection.UNKNOWN;
    }
}
