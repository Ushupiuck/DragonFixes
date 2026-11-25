package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.Registry.CrystalElement;
import Reika.ChromatiCraft.World.IWG.PylonGenerator;
import Reika.ChromatiCraft.World.IWG.PylonGenerator.PylonEntry;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.World;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;

@Mixin(value = PylonGenerator.class, remap = false)
public class PylonGeneratorMixin {
    @Shadow @Final
    private EnumMap<CrystalElement, Collection<PylonEntry>> colorCache;

    // Keep the remove/add pair atomic, including replacement of an equal entry
    // carrying updated pylon data. This method itself does not access the world.
    @WrapMethod(method = "addLocation(LReika/ChromatiCraft/World/IWG/PylonGenerator$PylonEntry;)V")
    private void dragonfixes$addLocationLocked(PylonEntry entry, Operation<Void> original) {
        synchronized (colorCache) {
            original.call(entry);
        }
    }

    // Constructing PylonEntry queries the world; only lock the cache operations.
    @WrapOperation(method = "removeCachedPylon",
            at = @At(value = "INVOKE", target = "Ljava/util/EnumMap;get(Ljava/lang/Object;)Ljava/lang/Object;"),
            require = 1)
    private Object dragonfixes$getForRemoval(EnumMap<?, ?> map, Object key, Operation<Object> original) {
        synchronized (colorCache) {
            return original.call(map, key);
        }
    }

    @WrapOperation(method = "removeCachedPylon",
            at = @At(value = "INVOKE", target = "Ljava/util/Collection;remove(Ljava/lang/Object;)Z"),
            require = 1)
    private boolean dragonfixes$removeLocked(Collection<?> entries, Object entry, Operation<Boolean> original) {
        synchronized (colorCache) {
            return original.call(entries, entry);
        }
    }

    // The client-only overloads may not exist on a dedicated server.
    @WrapOperation(method = "clearOnLogout",
            at = @At(value = "INVOKE", target = "Ljava/util/EnumMap;clear()V"), require = 1)
    private void dragonfixes$clearLocked(EnumMap<?, ?> map, Operation<Void> original) {
        synchronized (colorCache) {
            original.call(map);
        }
    }

    // Each save/send sees one consistent cache snapshot. Do not hold the cache
    // lock while serializing NBT or sending packets. PylonEntry data is immutable
    // after construction, so copying the collections suffices.
    @WrapOperation(method = {"savePylonLocations", "sendDimensionCacheToPlayer"},
            at = @At(value = "FIELD",
                    target = "LReika/ChromatiCraft/World/IWG/PylonGenerator;colorCache:Ljava/util/EnumMap;",
                    opcode = Opcodes.GETFIELD), require = 2)
    private EnumMap<CrystalElement, Collection<PylonEntry>> dragonfixes$snapshotForTraversal(
            PylonGenerator instance, Operation<EnumMap<CrystalElement, Collection<PylonEntry>>> original,
            @Share("pylonSnapshot") LocalRef<EnumMap<CrystalElement, Collection<PylonEntry>>> snapshotRef) {
        EnumMap<CrystalElement, Collection<PylonEntry>> snapshot = snapshotRef.get();
        if (snapshot == null) {
            synchronized (colorCache) {
                EnumMap<CrystalElement, Collection<PylonEntry>> live = original.call(instance);
                snapshot = new EnumMap<>(CrystalElement.class);
                for (CrystalElement color : live.keySet()) {
                    snapshot.put(color, new ArrayList<>(live.get(color)));
                }
            }
            snapshotRef.set(snapshot);
        }
        return snapshot;
    }

    @WrapMethod(method = "printPylonCache")
    private void dragonfixes$printLocked(ICommandSender sender, Operation<Void> original) {
        synchronized (colorCache) {
            original.call(sender);
        }
    }

    /**
     * @author thegamemaster1234, DragonFixes contributors
     * @reason Synchronize with cache writers instead of retrying indefinitely on concurrent mutation.
     */
    @Overwrite
    public PylonEntry getNearestPylonSpawn(World world, double x, double y, double z, CrystalElement e) {
        synchronized (colorCache) {
            Collection<PylonEntry> entries = colorCache.get(e);
            if (entries == null) return null;
            int dimension = world.provider.dimensionId;
            double dist = Double.POSITIVE_INFINITY;
            PylonEntry close = null;
            for (PylonEntry loc : entries) {
                if (loc.location.dimensionID == dimension) {
                    double d = loc.location.getDistanceTo(x, y, z);
                    if (d < dist) {
                        dist = d;
                        close = loc;
                    }
                }
            }
            return close;
        }
    }
}
