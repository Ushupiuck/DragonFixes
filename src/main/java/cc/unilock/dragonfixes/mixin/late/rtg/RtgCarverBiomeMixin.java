package cc.unilock.dragonfixes.mixin.late.rtg;

import cc.unilock.dragonfixes.compat.CarverBiomeLookup;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtg.world.gen.MapGenCavesRTG;
import rtg.world.gen.MapGenRavineRTG;

@Mixin({MapGenCavesRTG.class, MapGenRavineRTG.class})
public abstract class RtgCarverBiomeMixin {
    // Forge-added methods retain their names; the World invocation is remapped.
    @WrapOperation(method = {"isTopBlock", "digBlock"}, remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBiomeGenForCoords(II)Lnet/minecraft/world/biome/BiomeGenBase;", remap = true),
            require = 2)
    private BiomeGenBase dragonfixes$reuseColumnBiome(World world, int x, int z,
            Operation<BiomeGenBase> original) {
        return CarverBiomeLookup.lookup(this, world, x, z, original);
    }
}
