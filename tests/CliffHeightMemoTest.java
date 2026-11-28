import cc.unilock.dragonfixes.compat.CliffBiomeNeighborhood;
import cc.unilock.dragonfixes.compat.CliffHeightMemo;
import cc.unilock.dragonfixes.compat.CliffRadialAngles;

public final class CliffHeightMemoTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        long points = 0;
        for (int cx : new int[]{-1874999,-3,-1,0,1,3,1874999}) {
            int ox = cx * 16;
            CliffBiomeNeighborhood<Integer> wide = new CliffBiomeNeighborhood<>(cx,cx,48);
            CliffHeightMemo memo = new CliffHeightMemo(cx,cx);
            Object owner = new Object(), other = new Object(), main = new Object(), edge = new Object();
            for (int x=ox; x<ox+16; x++) for(int z=ox;z<ox+16;z++) {
                int i = memo.index(x,z);
                check(!memo.contains(owner,i,main));
                double h = Double.longBitsToDouble(0x3ff0000000000000L + i);
                memo.remember(owner,i,main,h);
                check(memo.contains(owner,i,main) && !memo.contains(owner,i,edge));
                check(Double.doubleToRawLongBits(memo.get(i)) == Double.doubleToRawLongBits(h));
                check(!memo.contains(other,i,main));
                for(int d=1;d<=24;d++) for(double a=0;a<360;a+=7.5) {
                    double r=Math.toRadians(a);
                    int qx=(int)Math.floor(x+d*Math.cos(r)), qz=(int)Math.floor(z+d*Math.sin(r));
                    check(qx==(int)Math.floor(x+d*CliffRadialAngles.cos(r)));
                    check(qz==(int)Math.floor(z+d*CliffRadialAngles.sin(r)));
                    check(memo.index(qx,qz)>=0);
                    check(wide.index(qx-24,qz-24)>=0 && wide.index(qx+24,qz+24)>=0);
                    points++;
                }
            }
            int center=memo.index(ox,ox);
            memo.remember(other,center,edge,-0.0);
            check(!memo.contains(owner,center,main) && memo.contains(other,center,edge));
            check(Double.doubleToRawLongBits(memo.get(center))==Double.doubleToRawLongBits(-0.0));
            int nullIndex=wide.index(ox-48,ox-48);
            check(!wide.isKnown(nullIndex));wide.remember(nullIndex,null);
            check(wide.isKnown(nullIndex)&&wide.get(nullIndex)==null);
            int[] tiles={0};
            check(!wide.mayContainReachableCliff((x,z)->0,(x,z)->{
                tiles[0]++;Integer[] a=new Integer[256];java.util.Arrays.fill(a,0);return a;
            }, b->Integer.valueOf(1).equals(b)));
            check(tiles[0]==9);
        }
        System.out.println("Cliff radial coordinate parity: "+points+" points; height identity and expanded bounds passed");
    }
}
