package cc.unilock.dragonfixes.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

public final class CarverBiomeLookup {
    private CarverBiomeLookup() {}

    public static BiomeGenBase lookup(Object generator, World world, int x, int z,
            Operation<BiomeGenBase> original) {
        CarverBiomeScope scope = CarverBiomeScope.current(generator, world);
        if (scope == null) return original.call(world, x, z);
        int i = scope.columns.index(x, z);
        if (i < 0) return original.call(world, x, z);
        if (scope.columns.isKnown(i)) return scope.columns.get(i);
        BiomeGenBase biome = original.call(world, x, z);
        scope.columns.remember(i, biome);
        return biome;
    }
}
