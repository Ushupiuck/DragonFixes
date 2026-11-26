package cc.unilock.dragonfixes.mixin.early.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.unilock.dragonfixes.compat.StoredBiomeLookup;
import cpw.mods.fml.common.Loader;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilChunkLoader.class)
public abstract class AnvilChunkLoaderBiomeMixin implements StoredBiomeLookup.Access {
    @Shadow private List<?> chunksToRemove;
    @Shadow private Set<?> pendingAnvilChunksCoordinates;
    @Shadow private Object syncLockObject;
    @Unique private final boolean dragonfixes$observe = Loader.isModLoaded("naturescompass");
    @Unique private final Map<ChunkCoordIntPair, PendingChunkAccessor> dragonfixes$inFlight = new HashMap<>();
    @Unique private final ThreadLocal<PendingChunkAccessor> dragonfixes$writing = new ThreadLocal<>();
    @Unique private long dragonfixes$revision;

    // Observe the entry before the removal, under the existing queue lock.
    // Hodgepodge's LoadDuringSave redirects the removal itself. Injecting
    // before it (normal callback order, before redirects) lets both coexist.
    @Inject(method = "writeNextIO",
            at = @At(value = "INVOKE", target = "Ljava/util/List;remove(I)Ljava/lang/Object;", remap = false),
            require = 1)
    private void dragonfixes$captureWriting(CallbackInfoReturnable<Boolean> cir) {
        if (!dragonfixes$observe) return;
        PendingChunkAccessor entry = (PendingChunkAccessor) chunksToRemove.get(0);
        dragonfixes$inFlight.put(entry.dragonfixes$getCoordinate(), entry);
        dragonfixes$writing.set(entry);
        dragonfixes$revision++;
    }

    @WrapMethod(method = "writeNextIO")
    private boolean dragonfixes$clearWriting(Operation<Boolean> original) {
        if (!dragonfixes$observe) return original.call();
        try {
            return original.call();
        } finally {
            PendingChunkAccessor entry = dragonfixes$writing.get();
            if (entry != null) {
                dragonfixes$writing.remove();
                synchronized (syncLockObject) {
                    // Another thread can be writing a newer save of this chunk.
                    if (dragonfixes$inFlight.get(entry.dragonfixes$getCoordinate()) == entry) {
                        dragonfixes$inFlight.remove(entry.dragonfixes$getCoordinate());
                    }
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
            PendingChunkAccessor writing = dragonfixes$inFlight.get(coordinate);
            if (writing != null) {
                int result = StoredBiomeLookup.decode(writing.dragonfixes$getNbt(), x, z);
                return result < 0 ? StoredBiomeLookup.UNKNOWN : result | StoredBiomeLookup.PENDING;
            }
        }
        return StoredBiomeLookup.ABSENT;
    }
}
