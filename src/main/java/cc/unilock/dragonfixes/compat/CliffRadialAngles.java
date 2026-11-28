package cc.unilock.dragonfixes.compat;

/** Cache trig values, never rounded coordinate offsets. Keep absolute
 * coordinate addition and flooring in the original search instructions. */
public final class CliffRadialAngles {
    private static final double[] RADIANS = new double[48], COS = new double[48], SIN = new double[48];
    static {
        for (int i = 0; i < 48; i++) {
            double a = Math.toRadians(i * 7.5);
            RADIANS[i] = a;
            COS[i] = Math.cos(a);
            SIN[i] = Math.sin(a);
        }
    }
    private CliffRadialAngles() {}
    private static int index(double a) {
        int i = (int)(a * (24D / Math.PI) + 0.5D);
        return i >= 0 && i < 48 && RADIANS[i] == a ? i : -1;
    }
    public static double cos(double a) { int i = index(a); return i < 0 ? Math.cos(a) : COS[i]; }
    public static double sin(double a) { int i = index(a); return i < 0 ? Math.sin(a) : SIN[i]; }
}
