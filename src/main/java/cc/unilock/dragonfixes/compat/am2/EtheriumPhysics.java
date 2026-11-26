package cc.unilock.dragonfixes.compat.am2;

/** Etherium's movement profile, independent of Minecraft or the AM2 API. */
public final class EtheriumPhysics {
    public static final double HORIZONTAL_DRAG = 0.5D;
    public static final double VERTICAL_DRAG = 0.05D;
    public static final double LIFT = 0.05D;

    private EtheriumPhysics() {}

    public static double verticalVelocity(double velocity) {
        return velocity * VERTICAL_DRAG + LIFT;
    }

    // Entry burst, then a small staggered trail instead of two full bursts
    // per entity per tick. Stationary entities still emit occasional bubbles.
    public static int particleCount(float width, boolean entered, boolean moving, int tick, int entityId) {
        if (entered) return Math.max(1, Math.min(24, 1 + (int) (width * 20)));
        if (((tick + entityId) & 3) != 0) return 0;
        return moving ? 2 : 1;
    }
}
