package cc.unilock.dragonfixes.compat;

import java.util.Arrays;

/** Restore RTG's original ascending-ID traversal without a full ID-space scan. */
public final class RtgMapGenIds {
    private RtgMapGenIds() {}

    public static int[] uniqueSorted(int[] ids) {
        int[] sorted = ids.clone();
        Arrays.sort(sorted);
        int n = 0;
        for (int id : sorted) {
            if (n == 0 || sorted[n - 1] != id) sorted[n++] = id;
        }
        return Arrays.copyOf(sorted, n);
    }
}
