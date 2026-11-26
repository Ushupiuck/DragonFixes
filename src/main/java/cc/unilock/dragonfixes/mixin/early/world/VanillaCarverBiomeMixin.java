package cc.unilock.dragonfixes.mixin.early.world;

import cc.unilock.dragonfixes.compat.CarverBiomeLookup;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.world.gen.MapGenCaves;
import net.minecraft.world.gen.MapGenRavine;

@Mixin({MapGenCaves.class, MapGenRavine.class})
public abstract class VanillaCarverBiomeMixin {
    // Forge-added methods retain their names; the World invocation is remapped.
    @WrapOperation(method = {"isTopBlock", "digBlock"}, remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBiomeGenForCoords(II)Lnet/minecraft/world/biome/BiomeGenBase;", remap = true),
            require = 2)
    private BiomeGenBase dragonfixes$reuseColumnBiome(World world, int x, int z,
            Operation<BiomeGenBase> original) {
        return CarverBiomeLookup.lookup(this, world, x, z, original);
    }
}
