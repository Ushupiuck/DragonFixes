package cc.unilock.dragonfixes.compat;

import cc.unilock.dragonfixes.DragonFixes;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.tileentity.TileEntity;

/**
 * ProjectRed gates are parts inside a TileMultipart, not TileEntities.
 * Keep the original six exclusions, but evaluate each container's live
 * contents rather than caching a decision against the container class.
 */
public final class ProjectRedAcceleratorCompat {
    private ProjectRedAcceleratorCompat() {}

    private static final class Access {
        final Class<?> multipart;
        final Class<?> gate;
        final Method parts;
        final Method subId;

        Access() throws ReflectiveOperationException {
            ClassLoader loader = Launch.classLoader;
            multipart = Class.forName("codechicken.multipart.TileMultipart", false, loader);
            gate = Class.forName("mrtjp.projectred.integration.GatePart", false, loader);
            // Both FMP 1.7.18 and 2.0.1 expose the Java view.
            parts = multipart.getMethod("jPartList");
            subId = gate.getMethod("subID");
            if (!List.class.isAssignableFrom(parts.getReturnType()) || subId.getReturnType() != int.class) {
                throw new NoSuchMethodException("Unexpected ProjectRed/FMP gate access signatures");
            }
        }
    }

    private static final Access ACCESS = resolve();
    private static boolean lookupFailureLogged;

    private static Access resolve() {
        try {
            Access access = new Access();
            DragonFixes.LOGGER.info("ProjectRed accelerator compatibility: using live multipart gate exclusions");
            return access;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            DragonFixes.LOGGER.warn("Unable to resolve ProjectRed multipart gate API; leaving legacy accelerator registrations intact", e);
            return null;
        }
    }

    public static boolean replacesLegacyRegistration(String name) {
        if (ACCESS == null) return false;
        switch (name) {
            case "mrtjp.projectred.integration.Timer":
            case "mrtjp.projectred.integration.Sequencer":
            case "mrtjp.projectred.integration.Repeater":
            case "mrtjp.projectred.integration.PulseFormer":
            case "mrtjp.projectred.integration.StateCell":
            case "mrtjp.projectred.integration.Synchronizer":
                return true;
            default:
                return false;
        }
    }

    /**
     * GateDefinition ordinals in ProjectRed 4.13.4-GTNH, matching the
     * six gate item metadata values in ChromatiCraft's original blacklist.
     */
    static boolean isProtectedGate(int id) {
        switch (id) {
            case 9:  // Pulse
            case 10: // Repeater
            case 17: // Timer
            case 18: // Sequencer
            case 20: // State Cell
            case 21: // Synchronizer
                return true;
            default:
                return false;
        }
    }

    public static boolean containsProtectedGate(TileEntity tile) {
        Access access = ACCESS;
        if (access == null || tile == null || tile.isInvalid() || !access.multipart.isInstance(tile)) {
            return false;
        }
        try {
            List<?> parts = (List<?>) access.parts.invoke(tile);
            for (Object part : parts) {
                if (access.gate.isInstance(part)
                        && isProtectedGate((Integer) access.subId.invoke(part))) {
                    return true;
                }
            }
            return false;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            // Do not accelerate a container we cannot safely inspect.
            if (!lookupFailureLogged) {
                lookupFailureLogged = true;
                DragonFixes.LOGGER.warn("Unable to inspect a multipart accelerator target; refusing extra ticks for that target", e);
            }
            return true;
        }
    }
}
