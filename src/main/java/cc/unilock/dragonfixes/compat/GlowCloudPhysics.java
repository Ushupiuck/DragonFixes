package cc.unilock.dragonfixes.compat;

/** Numerical limits for Glow Clouds only. No dependence on Minecraft classes. */
public final class GlowCloudPhysics {
    // Angry flight is 0.375 blocks/tick. Allow its original one-time 2x lift.
    public static final double MAX_CONTROLLER_SPEED = 0.75D;
    // Leave ample headroom for ordinary external impulses/vanilla movement.
    public static final double MAX_MOTION_COMPONENT = 8D;
    private static final double WORLD_LIMIT = 30000000D;
    private static final double QUERY_LIMIT = WORLD_LIMIT + 256D;
    // Cloud alert propagation searches up to 63 blocks in each direction.
    private static final double MAX_QUERY_SPAN = 256D;
    private GlowCloudPhysics() {}

    public static double safeMagnitude(double magnitude) {
        if (!Double.isFinite(magnitude)) return 0.15D;
        return Math.max(-MAX_CONTROLLER_SPEED, Math.min(MAX_CONTROLLER_SPEED, magnitude));
    }

    public static double safeAngle(double angle, double fallback) {
        return Double.isFinite(angle) ? angle : fallback;
    }

    public static boolean validMotion(double x, double y, double z) {
        return bounded(x, MAX_MOTION_COMPONENT) && bounded(y, MAX_MOTION_COMPONENT)
                && bounded(z, MAX_MOTION_COMPONENT);
    }

    public static boolean validPosition(double x, double y, double z) {
        return bounded(x, WORLD_LIMIT) && bounded(y, WORLD_LIMIT) && bounded(z, WORLD_LIMIT);
    }

    private static boolean bounded(double value, double limit) {
        return Double.isFinite(value) && Math.abs(value) <= limit;
    }

    private static boolean axis(double min, double max, double limit, double span) {
        return bounded(min, limit) && bounded(max, limit) && min <= max && max - min <= span;
    }

    public static boolean validQuery(double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ) {
        return axis(minX, maxX, QUERY_LIMIT, MAX_QUERY_SPAN)
                && axis(minY, maxY, QUERY_LIMIT, MAX_QUERY_SPAN)
                && axis(minZ, maxZ, QUERY_LIMIT, MAX_QUERY_SPAN);
    }

    public static boolean validBody(double x, double y, double z,
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return validPosition(x, y, z) && validQuery(minX,minY,minZ,maxX,maxY,maxZ)
                && minX < maxX && minY < maxY && minZ < maxZ
                && Math.abs(x-minX) <= 4D && Math.abs(x-maxX) <= 4D
                && Math.abs(y-minY) <= 4D && Math.abs(y-maxY) <= 4D
                && Math.abs(z-minZ) <= 4D && Math.abs(z-maxZ) <= 4D
                && maxX-minX <= 4D && maxY-minY <= 4D && maxZ-minZ <= 4D;
    }
}
