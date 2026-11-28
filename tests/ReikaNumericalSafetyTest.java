import cc.unilock.dragonfixes.compat.ReikaNumericalSafety;

public final class ReikaNumericalSafetyTest {
    static void check(boolean b) { if (!b) throw new AssertionError(); }
    static void eq(double a,double b) { if (Double.doubleToLongBits(a)!=Double.doubleToLongBits(b)) throw new AssertionError(a+" != "+b); }
    public static void main(String[] args) {
        for(int i=-100000;i<=100000;i++) {
            int old=i;
            while(old>3) old-=4;
            while(old<0) old+=4;
            check(old==ReikaNumericalSafety.cardinalIndex(i));
        }
        check(ReikaNumericalSafety.cardinalIndex(Integer.MIN_VALUE)==0);
        check(ReikaNumericalSafety.cardinalIndex(Integer.MAX_VALUE)==3);
        for(double d:new double[]{0.125,0.5,1,2,24,128}) {
            eq(d,ReikaNumericalSafety.nonzeroDistance(d));
            for(double e:new double[]{0,0.5,1,2}) {
                double old=e>0 ? 0.25/Math.pow(d,e) : 0.25;
                eq(old,ReikaNumericalSafety.scaledPower(d,0.25,e));
            }
        }
        eq(1,ReikaNumericalSafety.nonzeroDistance(0));
        eq(0,ReikaNumericalSafety.scaledPower(0,1,1));
        eq(1,ReikaNumericalSafety.scaledPower(0,1,0));
        eq(64,ReikaNumericalSafety.scaledPower(1e-200,1,2));
        eq(-64,ReikaNumericalSafety.scaledPower(1e-200,-1,2));
        eq(200,ReikaNumericalSafety.scaledPower(1e-200,200,2));
        eq(3,ReikaNumericalSafety.finiteOr(Double.NaN,3));
        eq(0,ReikaNumericalSafety.finiteOr(Double.NaN,Double.POSITIVE_INFINITY));
        double[] normal={1,90,180};
        check(ReikaNumericalSafety.repairPolar(0,0,1,normal)==normal);
        double[] zero=ReikaNumericalSafety.repairPolar(0,0,0,new double[]{0,Double.NaN,180});
        eq(0,zero[0]);eq(90,zero[1]);eq(180,zero[2]);
        double[] tiny=ReikaNumericalSafety.repairPolar(1e-200,0,0,new double[]{0,Double.NaN,270});
        eq(1e-200,tiny[0]);eq(90,tiny[1]);eq(270,tiny[2]);
        check(ReikaNumericalSafety.rayAllowed(0,0,0,0,0,0));
        check(ReikaNumericalSafety.rayAllowed(-30000000,64,0,-30000000+16384,64,0));
        check(!ReikaNumericalSafety.rayAllowed(0,0,0,16385,0,0));
        check(!ReikaNumericalSafety.rayAllowed(0,0,0,Double.NaN,0,0));
        check(!ReikaNumericalSafety.rayAllowed(0,0,0,Double.POSITIVE_INFINITY,0,0));
        check(!ReikaNumericalSafety.rayAllowed(-Double.MAX_VALUE,0,0,Double.MAX_VALUE,0,0));
        System.out.println("Reika numerical safety: cardinal parity, force and polar singularities, ray bounds passed");
    }
}
