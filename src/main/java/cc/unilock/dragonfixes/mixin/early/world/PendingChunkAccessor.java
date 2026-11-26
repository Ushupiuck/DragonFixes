package cc.unilock.dragonfixes.mixin.early.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.chunk.storage.AnvilChunkLoader$PendingChunk")
public interface PendingChunkAccessor {
    @Accessor("chunkCoordinate")
    ChunkCoordIntPair dragonfixes$getCoordinate();

    @Accessor("nbtTags")
    NBTTagCompound dragonfixes$getNbt();
}
