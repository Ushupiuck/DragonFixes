import cc.unilock.dragonfixes.compat.CarverBiomeColumns;
import java.util.Random;

public class CarverBiomeColumnsTest {
    private static void check(boolean b) { if (!b) throw new AssertionError(); }

    public static void main(String[] args) {
        Random r = new Random(431308650295624829L);
        for (int t = 0; t < 1024; t++) {
            int cx = r.nextInt(3750001)-1875000, cz = r.nextInt(3750001)-1875000;
            CarverBiomeColumns<Integer> cache = new CarverBiomeColumns<>(cx, cz);
            int misses=0;
            for (int y=0; y<256; y++) {
                for (int x=0; x<16; x++) for (int z=0; z<16; z++) {
                    int i=cache.index(cx*16+x,cz*16+z);
                    check(i==x*16+z);
                    Integer expected = i==0 ? null : Integer.valueOf(i);
                    if (!cache.isKnown(i)) { cache.remember(i,expected); misses++; }
                    check(java.util.Objects.equals(expected,cache.get(i)));
                }
            }
            check(misses==256);
            check(cache.index(cx*16-1,cz*16)==-1);
            check(cache.index(cx*16+16,cz*16)==-1);
            check(cache.index(cx*16,cz*16-1)==-1);
            check(cache.index(cx*16,cz*16+16)==-1);
            check(!new CarverBiomeColumns<Integer>(cx,cz).isKnown(0));
        }
        System.out.println("Carver biome columns: 1024 chunk simulations passed");
    }
}
