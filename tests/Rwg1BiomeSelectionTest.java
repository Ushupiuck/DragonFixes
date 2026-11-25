import java.util.Random;
import cc.unilock.dragonfixes.compat.Rwg1BiomeSelection;

/** Standalone parity check against RWG1 alpha-1.6.0's original selection loop. */
public final class Rwg1BiomeSelectionTest {
    private static int reference(float[] weights, float noise, int dominant) {
        boolean randBiome = dominant < 0;
        int result = dominant;
        float bCount = 0f;
        float bRand = 0f;
        if (randBiome) {
            bRand = 0.5f + noise;
            bRand = bRand < 0f ? 0f : bRand > 0.99999f ? 0.99999f : bRand;
        }
        for (int k = 0; k < 256; k++) {
            if (weights[k] > 0f) {
                if (randBiome && bCount <= 1f) {
                    bCount += weights[k];
                    if (bCount > bRand) {
                        result = k;
                        bCount = 2f;
                    }
                }
            }
        }
        return result;
    }

    private static void check(float[] weights, float noise, int dominant) {
        int expected = reference(weights, noise, dominant);
        int actual = dominant >= 0 ? dominant : Rwg1BiomeSelection.select(weights, noise);
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    public static void main(String[] args) {
        Random random = new Random(0x52475731L);
        float[] weights = new float[256];
        check(weights, 0f, -1);
        weights[0] = 0.5f;
        weights[255] = 0.5f;
        for (float noise : new float[] {-2f, -0.5f, -Math.ulp(0.5f), 0f, Math.ulp(0.5f), 0.5f, 2f, Float.NaN}) {
            check(weights, noise, -1);
            check(weights, noise, 42);
        }
        int cases = 0;
        for (int seed = 0; seed < 128; seed++) {
            random.setSeed(seed);
            for (int column = 0; column < 256; column++) {
                java.util.Arrays.fill(weights, 0f);
                int entries = 1 + random.nextInt(16);
                for (int i = 0; i < entries; i++) weights[random.nextInt(256)] += random.nextFloat();
                float total = 0f;
                for (float weight : weights) total += weight;
                if (total > 0f) {
                    for (int i = 0; i < weights.length; i++) weights[i] /= total;
                }
                float noise = random.nextFloat() * 2f - 1f;
                check(weights, noise, -1);
                check(weights, noise, random.nextInt(256));
                cases += 2;
            }
        }
        System.out.println("RWG1 selection parity: " + cases + " randomized cases plus threshold edge cases passed");
    }
}
