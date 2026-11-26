package cc.unilock.dragonfixes.compat;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

/** Exact carver and world identity; nested calls restore their parent. */
public final class CarverBiomeScope {
    private static final ThreadLocal<CarverBiomeScope> ACTIVE = new ThreadLocal<>();
    private final Object generator;
    private final World world;
    public final CarverBiomeColumns<BiomeGenBase> columns;

    private CarverBiomeScope(Object generator, World world, int x, int z) {
        this.generator = generator;
        this.world = world;
        this.columns = new CarverBiomeColumns<>(x, z);
    }

    public static CarverBiomeScope enter(Object generator, World world, int x, int z) {
        CarverBiomeScope previous = ACTIVE.get();
        ACTIVE.set(new CarverBiomeScope(generator, world, x, z));
        return previous;
    }

    public static CarverBiomeScope current(Object generator, World world) {
        CarverBiomeScope scope = ACTIVE.get();
        return scope != null && scope.generator == generator && scope.world == world ? scope : null;
    }

    public static void exit(CarverBiomeScope previous) {
        if (previous == null) ACTIVE.remove();
        else ACTIVE.set(previous);
    }
}
