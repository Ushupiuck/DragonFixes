package cc.unilock.dragonfixes.mixin.late.rwg1;

import java.util.Random;

import Reika.ChromatiCraft.ChromatiCraft;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.DragonFixesConfig;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import rwg.support.RealisticBiomeSupport;

@Mixin(value = RealisticBiomeSupport.class, remap = false)
public abstract class RealisticBiomeSupportMixin {
    @Unique private boolean dragonfixes$loggedStreamDecoration;

    // RWG1 chooses the parent wrapper for its weighted population pass. Its actual
    // decoration area is [chunkX+8, chunkX+23] x [chunkZ+8, chunkZ+23].
    // As in vanilla, use the actual biome at the center of that area. Replace the
    // existing decorator invocation, rather than running an extra complete pass.
    @WrapOperation(method = "rDecorate",
            at = @At(value = "FIELD", target = "Lrwg/support/RealisticBiomeSupport;customBiome:Lnet/minecraft/world/biome/BiomeGenBase;",
                    opcode = Opcodes.GETFIELD), require = 1)
    private BiomeGenBase dragonfixes$selectStreamDecorator(RealisticBiomeSupport wrapper,
            Operation<BiomeGenBase> original, World world, Random random, int chunkX, int chunkZ) {
        BiomeGenBase parent = original.call(wrapper);
        if (parent == null || parent != ChromatiCraft.rainbowforest || ChromatiCraft.rainbowRiver == null
                || world.isRemote) return parent;
        int x = chunkX + 16;
        int z = chunkZ + 16;
        // The vanilla population window normally has this chunk loaded already.
        // Never generate a new chunk solely to decide which decorator to use.
        if (!world.getChunkProvider().chunkExists(x >> 4, z >> 4)) return parent;
        if (world.getBiomeGenForCoordsBody(x, z) != ChromatiCraft.rainbowRiver) return parent;
        if (DragonFixesConfig.enableBiomeGenerationDiagnostics && !dragonfixes$loggedStreamDecoration) {
            dragonfixes$loggedStreamDecoration = true;
            DragonFixes.LOGGER.info("RWG1: using native Rainbow Stream decorator at population origin {}, {}", chunkX, chunkZ);
        }
        return ChromatiCraft.rainbowRiver;
    }
}
