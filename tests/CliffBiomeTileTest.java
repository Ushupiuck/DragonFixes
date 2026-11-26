import cc.unilock.dragonfixes.compat.CliffBiomeNeighborhood;
import java.util.Random;

public final class CliffBiomeTileTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }

    private static long first(int x, int z, CliffBiomeNeighborhood.Lookup<Integer> raw) {
        for (int d = 1; d <= 16; d += 4) for (double a = 0; a < 360; a += 7.5) {
            int qx = (int)Math.floor(x + d*Math.cos(Math.toRadians(a)));
            int qz = (int)Math.floor(z + d*Math.sin(Math.toRadians(a)));
            if (Integer.valueOf(1).equals(raw.get(qx,qz))) return ((long)qx << 32) ^ (qz & 0xffffffffL);
        }
        return Long.MIN_VALUE;
    }

    public static void main(String[] args) {
        Random random = new Random(431308650295624829L);
        int tested = 0;
        for (int cx : new int[]{-1874999,-3,-1,0,2,1874999}) for (int cz : new int[]{-1874999,-1,0,3,1874999}) {
            final int ox=cx*16, oz=cz*16;
            for (int scenario=0; scenario<6; scenario++) {
                final int mode=scenario;
                final int cliffX = ox + random.nextInt(48)-16, cliffZ = oz + random.nextInt(48)-16;
                CliffBiomeNeighborhood.Lookup<Integer> raw = (x,z) -> {
                    if (mode == 0 || mode == 1) return 0;
                    if (mode == 2) return x==cliffX && z==cliffZ ? 1 : 0;
                    if (mode == 3) return x==ox-16 ? 1 : 0;
                    if (mode == 4) return Math.floorMod(x*17+z*31,29)==0 ? 1 : 0;
                    return x==ox-13 && z==oz ? 1 : null;
                };
                CliffBiomeNeighborhood<Integer> c = new CliffBiomeNeighborhood<>(cx,cz);
                int[] scalars={0}, tiles={0};
                CliffBiomeNeighborhood.Lookup<Integer> scalar=(x,z)->{scalars[0]++;return raw.get(x,z);};
                CliffBiomeNeighborhood.TileLookup<Integer> tile=(x,z)->{
                    tiles[0]++;
                    check((x&15)==0 && (z&15)==0);
                    Integer[] result=new Integer[256];
                    for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++){
                        // Mode 5: emulate RTG nulls resolved by the scalar patcher.
                        result[dx+dz*16]=mode==5?null:raw.get(x+dx,z+dz);
                    }
                    return result;
                };
                boolean may=c.mayContainReachableCliff(scalar, mode==0?null:tile,b->Integer.valueOf(1).equals(b));
                for(int x=ox;x<ox+16;x++)for(int z=oz;z<oz+16;z++){
                    long expected=first(x,z,raw);
                    long actual=may?first(x,z,(qx,qz)->{
                        int i=c.index(qx,qz);
                        return c.isKnown(i)?c.get(i):raw.get(qx,qz);
                    }):Long.MIN_VALUE;
                    check(actual==expected);
                    tested++;
                }
                check(tiles[0]<=9);
                if(mode==0)check(!may && scalars[0]==42*42 && tiles[0]==0);
                if(mode==1)check(!may && scalars[0]==0 && tiles[0]==9);
                if(mode==3)check(!may);
                if(mode==5)check(may && scalars[0]>0);
                int before=scalars[0]+tiles[0];
                check(c.mayContainReachableCliff(scalar,tile,b->Integer.valueOf(1).equals(b))==may);
                check(before==scalars[0]+tiles[0]);
            }
        }
        System.out.println("Cliff tile search parity: "+tested+" columns");
    }
}
