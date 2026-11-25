package cc.unilock.dragonfixes.compat;

/** Exact RWG1 alpha-1.6.0 getNewNoise weighted selection, after interpolation. */
public final class Rwg1BiomeSelection {
    private Rwg1BiomeSelection() {}

    public static int select(float[] weights, float noise) {
        float threshold = 0.5f + noise;
        threshold = threshold < 0f ? 0f : threshold > 0.99999f ? 0.99999f : threshold;
        float count = 0f;
        for (int id = 0; id < 256; id++) {
            if (weights[id] > 0f && count <= 1f) {
                count += weights[id];
                if (count > threshold) return id;
            }
        }
        return -1;
    }
}
