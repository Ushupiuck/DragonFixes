package cc.unilock.dragonfixes.mixin.late.rtg;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.compat.RtgLandscapeAccess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import rtg.world.gen.ChunkLandscape;
import rtg.world.gen.ChunkProviderRTG;
import rtg.world.gen.LandscapeGenerator;
import rtg.world.biome.realistic.RealisticBiomeBase;

@Mixin(ChunkProviderRTG.class)
public abstract class ChunkProviderRTGMixin implements RtgLandscapeAccess {
    @Shadow(remap = false) @Final private World worldObj;
    @Shadow(remap = false) @Final private LandscapeGenerator landscapeGenerator;
    @Unique private boolean dragonfixes$loggedStream;
    @Unique private boolean dragonfixes$loggedCliffs;
    @Unique private boolean dragonfixes$loggedShores;

    @Override
    public World dragonfixes$getWorld() {
        return worldObj;
    }

    @Override
    public LandscapeGenerator dragonfixes$getLandscape() {
        return landscapeGenerator;
    }

    // Log only when the real chunk provider consumes a landscape, not a compass preview.
    @ModifyExpressionValue(method = "provideChunk",
            at = @At(value = "INVOKE", target = "Lrtg/world/gen/LandscapeGenerator;landscape(Lrtg/world/biome/RTGBiomeProvider;II)Lrtg/world/gen/ChunkLandscape;", remap = false),
            require = 1)
    private ChunkLandscape dragonfixes$reportStreamGeneration(ChunkLandscape landscape, int chunkX, int chunkZ) {
        if (!dragonfixes$loggedStream && ChromatiCraft.rainbowRiver != null) {
            int count = 0;
            for (RealisticBiomeBase biome : landscape.biome) {
                if (biome != null && biome.baseBiome == ChromatiCraft.rainbowRiver) count++;
            }
            if (count > 0) {
                dragonfixes$loggedStream = true;
                DragonFixes.LOGGER.info("RTG: generating Rainbow Stream at chunk {}, {} ({} columns)", chunkX, chunkZ, count);
            }
        }
        if (!dragonfixes$loggedCliffs || !dragonfixes$loggedShores) {
            int cliffs = 0;
            int shores = 0;
            for (RealisticBiomeBase biome : landscape.biome) {
                if (biome == null) continue;
                if (biome.baseBiome == ChromatiCraft.glowingcliffs) cliffs++;
                if (biome.baseBiome == ChromatiCraft.glowingcliffsEdge) shores++;
            }
            if (!dragonfixes$loggedCliffs && cliffs > 0) {
                dragonfixes$loggedCliffs = true;
                DragonFixes.LOGGER.info("RTG: generating Luminous Cliffs at chunk {}, {} ({} columns)",
                        chunkX, chunkZ, cliffs);
            }
            if (!dragonfixes$loggedShores && shores > 0) {
                dragonfixes$loggedShores = true;
                DragonFixes.LOGGER.info("RTG: generating Luminous Cliffs Shores at chunk {}, {} ({} columns)",
                        chunkX, chunkZ, shores);
            }
        }
        return landscape;
    }
}
