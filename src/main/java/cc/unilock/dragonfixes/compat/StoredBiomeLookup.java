package cc.unilock.dragonfixes.compat;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.gen.ChunkProviderServer;

/** Read only biome metadata, never load or generate a chunk. */
public final class StoredBiomeLookup {
    public static final int ABSENT = -1;
    public static final int UNKNOWN = StoredBiomeIds.UNKNOWN;
    public static final int PENDING = 1 << 16;
    private StoredBiomeLookup() {}

    public interface Access {
        int dragonfixes$pendingBiome(int x, int z);
        long dragonfixes$saveRevision();
    }

    public static boolean isKnown(int result) {
        return result >= 0;
    }

    public static boolean isPending(int result) {
        return result >= PENDING;
    }

    public static int id(int result) {
        return result & 65535;
    }

    public static int read(World world, int x, int z) {
        if (!(world.getChunkProvider() instanceof ChunkProviderServer)) return UNKNOWN;
        ChunkProviderServer provider = (ChunkProviderServer) world.getChunkProvider();
        if (!(provider.currentChunkLoader instanceof AnvilChunkLoader)) return UNKNOWN;
        AnvilChunkLoader loader = (AnvilChunkLoader) provider.currentChunkLoader;
        if (!(loader instanceof Access)) return UNKNOWN;
        Access access = (Access) loader;
        long revision = access.dragonfixes$saveRevision();
        int pending = access.dragonfixes$pendingBiome(x, z);
        if (pending != ABSENT) return pending;

        int cx = x >> 4, cz = z >> 4;
        File region = new File(new File(loader.chunkSaveLocation, "region"),
                "r." + (cx >> 5) + "." + (cz >> 5) + ".mca");
        int result = ABSENT;
        try {
            // RegionFileCache normally creates missing files. Avoid that for searches.
            if (region.isFile()) {
                RegionFile file = RegionFileCache.createOrLoadRegionFile(loader.chunkSaveLocation, cx, cz);
                if (file.chunkExists(cx & 31, cz & 31)) {
                    try (DataInputStream input = file.getChunkDataInputStream(cx & 31, cz & 31)) {
                        result = input == null ? UNKNOWN : decode(CompressedStreamTools.read(input), x, z);
                    }
                }
            }
        } catch (IOException | RuntimeException ex) {
            result = UNKNOWN;
        }
        // An asynchronous write may have overtaken our read. Prefer its data or retry.
        pending = access.dragonfixes$pendingBiome(x, z);
        if (pending != ABSENT) return pending;
        return revision == access.dragonfixes$saveRevision() ? result : UNKNOWN;
    }

    public static int decode(NBTTagCompound root, int x, int z) {
        if (root == null || !root.hasKey("Level", 10)) return UNKNOWN;
        NBTTagCompound level = root.getCompoundTag("Level");
        if (level.getInteger("xPos") != (x >> 4) || level.getInteger("zPos") != (z >> 4)) return UNKNOWN;
        int index = StoredBiomeIds.index(x, z);
        if (level.hasKey("Biomes16v2", 7)) {
            return StoredBiomeIds.extended(level.getByteArray("Biomes16v2"), index);
        }
        if (level.hasKey("Biomes16", 7)) {
            return StoredBiomeIds.legacy(level.getByteArray("Biomes16"), index);
        }
        if (level.hasKey("Biomes", 7)) {
            return StoredBiomeIds.vanilla(level.getByteArray("Biomes"), index);
        }
        return UNKNOWN;
    }
}
