package cc.unilock.dragonfixes.compat;

import net.minecraft.block.Block;
import rtg.world.gen.ChunkLandscape;
import rtg.world.gen.ChunkProviderRTG;

/** One scope per provideChunk invocation, including recursive generation. */
public final class RtgMapGenScope {
    private static final ThreadLocal<RtgMapGenScope> ACTIVE = new ThreadLocal<>();
    public final ChunkProviderRTG provider;
    public final int x, z;
    public ChunkLandscape landscape;
    public boolean examined, dispatching;
    public int remaining;
    public Block[] blocks;
    public byte[] metadata;

    private RtgMapGenScope(ChunkProviderRTG provider, int x, int z) {
        this.provider = provider;
        this.x = x;
        this.z = z;
    }

    public static RtgMapGenScope enter(ChunkProviderRTG provider, int x, int z) {
        RtgMapGenScope previous = ACTIVE.get();
        ACTIVE.set(new RtgMapGenScope(provider, x, z));
        return previous;
    }

    public static RtgMapGenScope current() { return ACTIVE.get(); }

    public static void exit(RtgMapGenScope previous) {
        if (previous == null) ACTIVE.remove();
        else ACTIVE.set(previous);
    }

    /** Match the actual EID redirect, not the native RTG path. Only once per chunk. */
    public static boolean calledFromEndlessIds() {
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            if (e.getClassName().equals(ChunkProviderRTG.class.getName())
                    && e.getMethodName().endsWith("$endlessids$injectTheFunny")) return true;
        }
        return false;
    }
}
