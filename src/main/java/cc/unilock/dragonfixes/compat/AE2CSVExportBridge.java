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

    public static void startDeferred() {
        loadComplete = true;
        Thread thread;
        while ((thread = DEFERRED.poll()) != null) {
            DragonFixes.LOGGER.info("Starting deferred {}", thread.getName());
            thread.start();
        }
    }
}
