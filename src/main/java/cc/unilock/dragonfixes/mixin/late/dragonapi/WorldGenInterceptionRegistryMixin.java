package cc.unilock.dragonfixes.mixin.late.dragonapi;

import Reika.DragonAPI.Auxiliary.WorldGenInterceptionRegistry;
import Reika.DragonAPI.Auxiliary.WorldGenInterceptionRegistry.BlockSetData;
import Reika.DragonAPI.Auxiliary.WorldGenInterceptionRegistry.BlockSetWatcher;
import Reika.DragonAPI.Auxiliary.WorldGenInterceptionRegistry.InterceptionException;
import Reika.DragonAPI.Auxiliary.Trackers.WorldgenProfiler;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;
import Reika.DragonAPI.Instantiable.Event.ChunkGenerationEvent;
import Reika.DragonAPI.Instantiable.Event.SetBlockEvent;
import Reika.DragonAPI.ModInteract.DeepInteract.PlanetDimensionHandler;
import cc.unilock.dragonfixes.compat.WorldgenChangeBuffer;
import cc.unilock.dragonfixes.compat.WorldgenChangeAccess;
import cc.unilock.dragonfixes.mixin.late.dragonapi.accessor.BlockSetDataInvoker;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;

@Mixin(value = WorldGenInterceptionRegistry.class, remap = false)
public abstract class WorldGenInterceptionRegistryMixin {
    @Shadow @Final private ArrayList<BlockSetWatcher> watchers;
    @Shadow @Final private ArrayList<InterceptionException> exceptions;

    @Unique
    private static WorldgenChangeBuffer<Coordinate, BlockSetData> dragonfixes$changes(World world) {
        return ((WorldgenChangeAccess) world).dragonfixes$worldgenChanges();
    }

    /**
     * @author DragonFixes
     * @reason Track pending changes in the world that owns them.
     */
    @Overwrite
    @SubscribeEvent
    public void prePopulation(ChunkGenerationEvent event) {
        World world = event.world;
        // Galacticraft is excluded by the original onSetBlock method.
        // No reason to open a recording batch for it in the first place.
        if (!watchers.isEmpty() && !world.isRemote && !PlanetDimensionHandler.isGalacticWorld(world)) {
            dragonfixes$changes(world).begin();
        }
    }

    /**
     * @author DragonFixes
     * @reason Avoid collecting unrelated client/dimension changes and shared-map races.
     */
    @Overwrite
    @SubscribeEvent
    public void onSetBlock(SetBlockEvent.Pre event) {
        if (event.world.isRemote || watchers.isEmpty()) return;
        WorldgenChangeBuffer<Coordinate, BlockSetData> batch = dragonfixes$changes(event.world);
        if (!batch.isRecording()) return;

        // Keep every exception and the last-write-wins behavior. Do not filter
        // on event.isWorldgen: generators can touch already populated chunks.
        for (InterceptionException exception : exceptions) {
            if (exception.doesExceptionApply(event.world, event.xCoord, event.yCoord,
                    event.zCoord, event.newBlock, event.newMeta)) return;
        }
        Coordinate coordinate = new Coordinate(event.xCoord, event.yCoord, event.zCoord);
        batch.put(coordinate, BlockSetDataInvoker.dragonfixes$create(
                (WorldGenInterceptionRegistry) (Object) this, coordinate,
                event.currentBlock, event.currentMeta, event.newBlock, event.newMeta));
    }

    /**
     * @author DragonFixes
     * @reason Dispatch stable snapshots without shared mutable state.
     */
    @Overwrite
    public void postPopulation(World world, int cx, int cz) {
        dragonfixes$changes(world).finish(snapshot -> {
            for (BlockSetWatcher watcher : watchers) {
                watcher.onChunkGeneration(world, snapshot);
            }
        });
        if (WorldgenProfiler.profilingEnabled()) {
            WorldgenProfiler.finishChunk(System.nanoTime(), cx, cz);
        }
    }

}

