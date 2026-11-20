package cc.unilock.dragonfixes.compat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private AE2CSVExportBridge() {}

    /** @return true when the start was deferred and the caller should cancel it. */
    public static boolean defer(Thread thread, String serviceName) {
        if (loadComplete) {
            return false;
        }

        // Match AE2's original AppEng.startService configuration.
        thread.setName(serviceName);
        thread.setPriority(Thread.MIN_PRIORITY);
        DEFERRED.add(thread);
        DragonFixes.LOGGER.info("Deferred {} until after Forge mod loading", serviceName);
        return true;
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
