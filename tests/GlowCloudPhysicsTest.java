import cc.unilock.dragonfixes.compat.GlowCloudPhysics;

public final class GlowCloudPhysicsTest {
    private static void check(boolean result) { if (!result) throw new AssertionError(); }
    private static void eq(double expected,double actual) {
        if (Double.doubleToLongBits(expected)!=Double.doubleToLongBits(actual))
            throw new AssertionError(expected+" != "+actual);
    }
    public static void main(String[] args) {
        for (double v : new double[]{-0.2,-0.01,0,0.01,0.15,0.2,0.375,0.75})
            eq(v,GlowCloudPhysics.safeMagnitude(v));
        double lift=0.375;
        for(int i=0;i<20000;i++) {
            lift=GlowCloudPhysics.safeMagnitude(lift*2);
            eq(0.75,lift);
        }
        eq(-0.75,GlowCloudPhysics.safeMagnitude(-Double.MAX_VALUE));
        eq(0.75,GlowCloudPhysics.safeMagnitude(Double.MAX_VALUE));
        for(double bad : new double[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}) {
            eq(0.15,GlowCloudPhysics.safeMagnitude(bad));
            eq(90,GlowCloudPhysics.safeAngle(bad,90));
            check(!GlowCloudPhysics.validPosition(bad,0,0));
            check(!GlowCloudPhysics.validMotion(0,bad,0));
            check(!GlowCloudPhysics.validQuery(0,0,0,bad,1,1));
        }
        eq(-270,GlowCloudPhysics.safeAngle(-270,0));
        check(GlowCloudPhysics.validMotion(0.75,-0.375,0.15));
        check(!GlowCloudPhysics.validMotion(1e10,0,0));
        for(double x : new double[]{-30000000,-16,-0.1,0,16,30000000}) {
            check(GlowCloudPhysics.validBody(x,64,x,x-0.3,64,x-0.3,x+0.3,65.8,x+0.3));
            check(GlowCloudPhysics.validQuery(x-64,0,x-64,x+64,128,x+64));
            check(!GlowCloudPhysics.validBody(x,64,x,x-100,64,x-0.3,x+100,65.8,x+0.3));
            check(!GlowCloudPhysics.validBody(x,64,x,x+10,64,x+10,x+11,65,x+11));
        }
        check(!GlowCloudPhysics.validQuery(-1e12,0,-1e12,1e12,1,1e12));
        check(!GlowCloudPhysics.validQuery(0,0,0,257,1,1));
        check(!GlowCloudPhysics.validQuery(2,0,0,1,1,1));
        check(!GlowCloudPhysics.validPosition(1e12,64,0));
        System.out.println("Glow Cloud motion, non-finite values, body integrity, and query bounds passed");
    }
}
