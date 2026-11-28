package cc.unilock.dragonfixes.compat;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

/** Shared between the event's outer terrain pass and the inner biome query. */
public final class CliffTerrainQueryScope {
    private static final ThreadLocal<CliffTerrainQueryScope> ACTIVE = new ThreadLocal<>();
    public final World world;
    public final CliffBiomeNeighborhood<BiomeGenBase> neighborhood;
    public final CliffHeightMemo heights;
    public final boolean stableManager;

    private CliffTerrainQueryScope(World world, int chunkX, int chunkZ) {
        this.world = world;
        Class<?> manager = world.getWorldChunkManager().getClass();
        stableManager = manager == net.minecraft.world.biome.WorldChunkManager.class
                || manager.getName().equals("rtg.world.biome.WorldChunkManagerRTG");
        neighborhood = new CliffBiomeNeighborhood<>(chunkX, chunkZ, stableManager ? 48 : 16);
        heights = new CliffHeightMemo(chunkX, chunkZ);
    }

    public static CliffTerrainQueryScope enter(World world, int chunkX, int chunkZ) {
        CliffTerrainQueryScope previous = ACTIVE.get();
        ACTIVE.set(new CliffTerrainQueryScope(world, chunkX, chunkZ));
        return previous;
    }

    public static CliffTerrainQueryScope current(World world) {
        CliffTerrainQueryScope scope = ACTIVE.get();
        return scope != null && scope.world == world ? scope : null;
    }

    public static void exit(CliffTerrainQueryScope previous) {
        if (previous == null) ACTIVE.remove();
        else ACTIVE.set(previous);
    }
}
