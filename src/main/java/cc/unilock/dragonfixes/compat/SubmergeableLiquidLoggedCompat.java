package cc.unilock.dragonfixes.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameData;

import Reika.DragonAPI.Interfaces.Block.Submergeable;

import cc.unilock.dragonfixes.DragonFixes;

/**
 * Optional, one-way bridge. Reika marks blocks that can visually coexist
 * with water; LiquidLogged owns the actual stored-fluid state and physics.
 *
 * Register only water as a supported fluid. Lava or other Forge fluids should
 * retain whatever behavior the block or another mod already implements.
 */
public final class SubmergeableLiquidLoggedCompat {
    private static final String API = "com.gtnewhorizons.liquidlogged.api.LiquidLoggedAPI";
    private static final String PREDICATE =
            "com.gtnewhorizons.liquidlogged.api.FluidHolderPredicate";
    private static volatile MethodHandle getStoredFluid;
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private SubmergeableLiquidLoggedCompat() {}

    public static void register() {
        if (!Loader.isModLoaded("liquidlogged") || !Loader.isModLoaded("DragonAPI")
                || !REGISTERED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> api = Class.forName(API);
            Class<?> predicateType = Class.forName(PREDICATE);
            Class<?> holderType = Class.forName(
                    "com.gtnewhorizons.liquidlogged.api.IFluidHolder");
            Method registration = api.getMethod("registerPredicate", Block.class, predicateType);
            // Link once without introducing a hard LiquidLogged dependency. The
            // render path invokes the handle with primitive coordinates, not varargs.
            getStoredFluid = MethodHandles.publicLookup().unreflect(api.getMethod("getFluid", IBlockAccess.class,
                    int.class, int.class, int.class)).asType(MethodType.methodType(Object.class,
                            IBlockAccess.class, int.class, int.class, int.class));

            int count = 0;
            for (Object candidate : GameData.getBlockRegistry()) {
                if (!(candidate instanceof Block)) {
                    continue;
                }
                Block block = (Block) candidate;
                if (!(block instanceof Submergeable) || holderType.isInstance(block)) {
                    continue; // An explicit LiquidLogged contract takes priority.
                }
                Object predicate = Proxy.newProxyInstance(
                        predicateType.getClassLoader(), new Class<?>[] { predicateType },
                        (proxy, method, args) -> {
                            if ("canHoldFluid".equals(method.getName())) {
                                Fluid fluid = (Fluid) args[6];
                                if (fluid != FluidRegistry.WATER) {
                                    return null; // Preserve other mods' fluid rules.
                                }
                                IBlockAccess world = (IBlockAccess) args[0];
                                return ((Submergeable) args[4]).isSubmergeable(
                                        world, (Integer) args[1],
                                        (Integer) args[2], (Integer) args[3]);
                            }
                            if ("toString".equals(method.getName())) {
                                return "DragonFixes.SubmergeableWaterPredicate";
                            }
                            if ("hashCode".equals(method.getName())) {
                                return System.identityHashCode(proxy);
                            }
                            if ("equals".equals(method.getName())) {
                                return proxy == args[0];
                            }
                            throw new UnsupportedOperationException(method.getName());
                        });
                registration.invoke(null, block, predicate);
                count++;
            }
            DragonFixes.LOGGER.info(
                    "Registered {} Reika Submergeable blocks as water holders in LiquidLogged", count);
        } catch (ClassNotFoundException | NoSuchMethodException
                | IllegalAccessException | InvocationTargetException
                | LinkageError | SecurityException e) {
            DragonFixes.LOGGER.warn("Unable to register optional LiquidLogged/Submergeable bridge", e);
        }
    }

    /** Only consult the stored fluid for a Reika block that wants to draw water. */
    public static boolean hasStoredFluid(IBlockAccess world, int x, int y, int z) {
        MethodHandle getter = getStoredFluid;
        if (getter == null) {
            return false;
        }
        try {
            return (Object) getter.invokeExact(world, x, y, z) != null;
        } catch (Throwable e) {
            // Method.invoke used to wrap any target failure in InvocationTargetException.
            // Preserve that fallback when calling the same optional API via a handle.
            return false;
        }
    }
}
