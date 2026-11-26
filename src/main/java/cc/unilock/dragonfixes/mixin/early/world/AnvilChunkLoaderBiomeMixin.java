package cc.unilock.dragonfixes.mixin.early.world;

import java.util.List;
import java.util.Set;

import cc.unilock.dragonfixes.compat.StoredBiomeLookup;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilChunkLoader.class)
public abstract class AnvilChunkLoaderBiomeMixin implements StoredBiomeLookup.Access {
    @Shadow private List<?> chunksToRemove;
    @Shadow private Set<?> pendingAnvilChunksCoordinates;
    @Shadow private Object syncLockObject;
    @Unique private PendingChunkAccessor dragonfixes$writing;
    @Unique private long dragonfixes$revision;

    // Capture the removed entry while vanilla still holds its existing queue lock.
    @WrapOperation(method = "writeNextIO",
            at = @At(value = "INVOKE", target = "Ljava/util/List;remove(I)Ljava/lang/Object;", remap = false),
            require = 1)
    private Object dragonfixes$captureWriting(List<?> queue, int index, Operation<Object> original) {
        Object entry = original.call(queue, index);
        dragonfixes$writing = (PendingChunkAccessor) entry;
        dragonfixes$revision++;
        return entry;
    }

    @WrapMethod(method = "writeNextIO")
    private boolean dragonfixes$clearWriting(Operation<Boolean> original) {
        try {
            return original.call();
        } finally {
            synchronized (syncLockObject) {
                if (dragonfixes$writing != null) {
                    dragonfixes$writing = null;
                    dragonfixes$revision++;
                }
            }
        }
    }

    @Override
    public long dragonfixes$saveRevision() {
        synchronized (syncLockObject) {
            return dragonfixes$revision;
        }
    }

    @Override
    public int dragonfixes$pendingBiome(int x, int z) {
        ChunkCoordIntPair coordinate = new ChunkCoordIntPair(x >> 4, z >> 4);
        synchronized (syncLockObject) {
            // A newer queued save takes precedence over an older in-flight write.
            if (pendingAnvilChunksCoordinates.contains(coordinate)) {
                for (Object value : chunksToRemove) {
                    PendingChunkAccessor entry = (PendingChunkAccessor) value;
                    if (coordinate.equals(entry.dragonfixes$getCoordinate())) {
                        int result = StoredBiomeLookup.decode(entry.dragonfixes$getNbt(), x, z);
                        return result < 0 ? StoredBiomeLookup.UNKNOWN : result | StoredBiomeLookup.PENDING;
                    }
                }
                return StoredBiomeLookup.UNKNOWN;
            }
            if (dragonfixes$writing != null && coordinate.equals(dragonfixes$writing.dragonfixes$getCoordinate())) {
                int result = StoredBiomeLookup.decode(dragonfixes$writing.dragonfixes$getNbt(), x, z);
                return result < 0 ? StoredBiomeLookup.UNKNOWN : result | StoredBiomeLookup.PENDING;
            }
        }
        return StoredBiomeLookup.ABSENT;
    }
}
