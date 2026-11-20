package cc.unilock.dragonfixes.compat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cc.unilock.dragonfixes.DragonFixes;

/**
 * AE2's optional item CSV exporter runs during FMLInitializationEvent and
 * enumerates ItemBlock.getSubItems on a background thread. This can trigger
 * ChromatiCraft's circular research-class initializers before ChromatiCraft
 * initializes them on the main thread, permanently marking a class erroneous.
 *
 * Delay only this service until FMLLoadCompleteEvent. Keep the original
 * Thread/ExportProcess, caching, and CSV generation entirely unchanged.
 */
public final class AE2CSVExportBridge {
    private static final Queue<Thread> DEFERRED = new ConcurrentLinkedQueue<>();
    private static volatile boolean loadComplete;
    private static final AtomicInteger BAD_DISPLAY_NAMES = new AtomicInteger();

    private AE2CSVExportBridge() {}

    /**
     * Replaces only the Thread.start invocation in AppEng.startService.
     * AE2 has already set the thread name/priority and logged the service.
     */
    public static void startOrDefer(Thread thread, String serviceName) {
        if (!"AE2 CSV Export".equals(serviceName) || loadComplete) {
            thread.start();
            return;
        }
        DEFERRED.add(thread);
        DragonFixes.LOGGER.info("Deferred {} until Forge completes mod loading", serviceName);
    }

    /**
     * AE2's CSV exporter should not die because a third-party item provides
     * an invalid display name (e.g. a BuildCraft facade containing a GregTech
     * frame with a null material). This is only used by the CSV exporter.
     */
    public static String csvDisplayName(ItemStack stack) {
        if (stack == null) {
            return "<null stack>";
        }
        try {
            return stack.getDisplayName();
        } catch (RuntimeException e) {
            Item item = stack.getItem();
            int id = item == null ? -1 : Item.getIdFromItem(item);
            int meta = stack.getItemDamage();
            int count = BAD_DISPLAY_NAMES.incrementAndGet();
            if (count <= 8) {
                DragonFixes.LOGGER.warn("AE2 CSV exporter: unusable item display name "
                        + "(item ID {}, meta {}); exporting fallback instead", id, meta, e);
            } else if (count == 9) {
                DragonFixes.LOGGER.warn("AE2 CSV exporter: suppressing further invalid display name warnings");
            }
            return "<item " + id + ":" + meta + " (name unavailable)>";
        }
    }

    public static void startDeferred() {
        loadComplete = true;
        Thread thread;
        while ((thread = DEFERRED.poll()) != null) {
            DragonFixes.LOGGER.info("Starting deferred {}", thread.getName());
            thread.start();
        }
    }
}
