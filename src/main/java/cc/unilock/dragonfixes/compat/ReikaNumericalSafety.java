package cc.unilock.dragonfixes.compat;

/** Shared numerical guards. Keep the original arithmetic on ordinary inputs. */
public final class ReikaNumericalSafety {
    public static final int FOG_ESCAPE_STEPS = 64;
    // 65,536 original quarter-block segments, far beyond local crystal links.
    public static final double MAX_RAY_LENGTH = 16384D;
    public static final int MAX_TRAJECTORY_SPEED = 65536;
    private ReikaNumericalSafety() {}

    public static boolean finite(double x, double y, double z) {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    public static double finiteOr(double value, double previous) {
        return Double.isFinite(value) ? value : Double.isFinite(previous) ? previous : 0D;
    }

    public static double nonzeroDistance(double distance) {
        return distance == 0D ? 1D : distance;
    }

    public static boolean rayAllowed(double x, double y, double z, double tx, double ty, double tz) {
        if (!finite(x,y,z) || !finite(tx,ty,tz)) return false;
        double dx=tx-x, dy=ty-y, dz=tz-z;
        double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
        return Double.isFinite(distance) && distance <= MAX_RAY_LENGTH;
    }

    public static int cardinalIndex(int index) {
        return index & 3;
    }

    /** Only singular sub-block distance scaling is capped; ordinary impulses
     * and explicitly high base powers remain untouched. */
    public static double scaledPower(double distance, double power, double exponent) {
        if (!(exponent > 0D)) return power;
        if (distance == 0D) return 0D;
        double result = power / Math.pow(distance, exponent);
        if (distance < 1D && Double.isFinite(power) && !Double.isNaN(result)) {
            double limit = Math.max(64D, Math.abs(power));
            if (Math.abs(result) > limit) return Math.copySign(limit,power);
        }
        return result;
    }

    /** Repair only invalid polar output; preserve the original array otherwise. */
    public static double[] repairPolar(double x, double y, double z, double[] original) {
        if (finite(original[0],original[1],original[2])) return original;
        if (!finite(x,y,z)) return new double[]{0D,90D,180D};
        double len=Math.hypot(Math.hypot(x,z),y);
        if (!Double.isFinite(len)) return new double[]{0D,90D,180D};
        double phi=180D+Math.toDegrees(Math.atan2(x,z));
        if (len==0D) return new double[]{0D,90D,phi};
        double theta=Math.toDegrees(Math.acos(Math.max(-1D,Math.min(1D,y/len))));
        return new double[]{len,theta,phi};
    }
}
