import cc.unilock.dragonfixes.compat.StoredBiomeIds;
import java.util.Arrays;

public final class StoredBiomeIdsTest {
    private static void check(int expected, int actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    public static void main(String[] args) {
        byte[] extended = new byte[512];
        byte[] vanilla = new byte[256];
        byte[] legacy = new byte[512];
        int[] ids = {0, 1, 99, 100, 255, 256, 257, 300, 4095, 32768, 65534, 65535};
        for (int i = 0; i < 256; i++) {
            int id = ids[i % ids.length];
            extended[i * 2] = (byte) id;
            extended[i * 2 + 1] = (byte) (id >>> 8);
            vanilla[i] = (byte) id;
            legacy[i] = (byte) id;
            legacy[i + 256] = (byte) (id >>> 8);
        }
        for (int cz : new int[] {-10, -1, 0, 1, 10}) {
            for (int cx : new int[] {-10, -1, 0, 1, 10}) {
                for (int i = 0; i < 256; i++) {
                    int index = StoredBiomeIds.index(cx * 16 + (i & 15), cz * 16 + (i >> 4));
                    check(i, index);
                    int id = ids[i % ids.length];
                    check(id == 65535 ? StoredBiomeIds.UNKNOWN : id, StoredBiomeIds.extended(extended, index));
                    check((id & 255) == 255 ? StoredBiomeIds.UNKNOWN : id & 255, StoredBiomeIds.vanilla(vanilla, index));
                    check(id >= 255 ? StoredBiomeIds.UNKNOWN : id, StoredBiomeIds.legacy(legacy, index));
                }
            }
        }
        check(StoredBiomeIds.UNKNOWN, StoredBiomeIds.extended(new byte[256], 0));
        check(StoredBiomeIds.UNKNOWN, StoredBiomeIds.vanilla(new byte[512], 0));
        check(StoredBiomeIds.UNKNOWN, StoredBiomeIds.legacy(new byte[0], 0));
        check(StoredBiomeIds.UNKNOWN, StoredBiomeIds.extended(extended, -1));
        check(StoredBiomeIds.UNKNOWN, StoredBiomeIds.extended(extended, 256));
        Arrays.fill(extended, (byte) 0xFF);
        check(StoredBiomeIds.UNKNOWN, StoredBiomeIds.extended(extended, 255));
        System.out.println("Stored biome decoding: 6,400 coordinate cases and all three formats passed");
    }
}
