package cc.unilock.dragonfixes.compat;

/** Pure decoding of supported on-disk biome arrays. */
public final class StoredBiomeIds {
    public static final int UNKNOWN = -2;
    private StoredBiomeIds() {}

    public static int index(int x, int z) {
        return ((z & 15) << 4) | (x & 15);
    }

    public static int extended(byte[] data, int index) {
        if (data.length != 512 || index < 0 || index >= 256) return UNKNOWN;
        int offset = index << 1;
        int value = (data[offset] & 255) | ((data[offset + 1] & 255) << 8);
        return value == 65535 ? UNKNOWN : value;
    }

    public static int legacy(byte[] data, int index) {
        // Legacy conversion differs across versions. Do not invent a high ID
        // when the installed loader's migration behavior is not known.
        if (data.length != 512 || index < 0 || index >= 256 || data[index + 256] != 0) return UNKNOWN;
        int value = data[index] & 255;
        return value == 255 ? UNKNOWN : value;
    }

    public static int vanilla(byte[] data, int index) {
        if (data.length != 256 || index < 0 || index >= 256) return UNKNOWN;
        int value = data[index] & 255;
        return value == 255 ? UNKNOWN : value;
    }
}
