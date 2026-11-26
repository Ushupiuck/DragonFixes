package cc.unilock.dragonfixes.debug;

import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.DragonFixesConfig;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Low-frequency wall-clock sampling, not per-chunk instrumentation.
 * No server locks, world queries, chunk creation or logging on each sample.
 */
public final class WorldStartupProfiler implements Runnable {
    private static final long PERIOD_MS = 50;
    private static final long REPORT_NS = TimeUnit.SECONDS.toNanos(5);
    private static WorldStartupProfiler active;

    private final Thread target;
    private final Thread sampler;
    private final long started = System.nanoTime();
    private final Map<String, Integer> stacks = new HashMap<>();
    private final Map<String, Integer> inclusive = new HashMap<>();
    private final Map<String, Integer> leafMethods = new HashMap<>();
    private final Map<String, Integer> subsystems = new HashMap<>();
    private final Map<String, Integer> windowSubsystems = new HashMap<>();
    private int windowSamples;
    private final Map<Thread.State, Integer> states = new HashMap<>();
    private final ThreadMXBean mx = ManagementFactory.getThreadMXBean();
    private final long cpuAtStart;
    private volatile boolean running = true;
    private volatile String reason = "startup thread exited";
    private int samples;

    private WorldStartupProfiler(Thread target) {
        this.target = target;
        cpuAtStart = cpuTime();
        sampler = new Thread(this, "DragonFixes startup sampler");
        sampler.setDaemon(true);
    }

    public static synchronized void start(Thread target) {
        stop("new startup");
        active = new WorldStartupProfiler(target);
        DragonFixes.LOGGER.info("[WorldStartupProfiler] start target={} storedLookup={} pylonCache={} pylonTracer={} rtgMapGenCorrection={} cliffOptimization={} caveBiomeOptimization={} cliffNeighborhoodScan={}",
                target.getName(), DragonFixesConfig.enableCompassStoredBiomeLookup,
                DragonFixesConfig.useSynchronizedPylonCache, DragonFixesConfig.useThreadLocalPylonTracer,
                DragonFixesConfig.optimizeRtgEndlessIdsMapGen, DragonFixesConfig.optimizeCliffBiomeQueries,
                DragonFixesConfig.optimizeCaveBiomeQueries, DragonFixesConfig.optimizeCliffNeighborhoodScan);
        active.sampler.start();
    }

    public static synchronized void stop(String reason) {
        if (active == null) return;
        active.reason = reason;
        active.running = false;
        active.sampler.interrupt();
        active = null;
    }

    private long cpuTime() {
        try {
            return mx.isThreadCpuTimeSupported() && mx.isThreadCpuTimeEnabled()
                    ? mx.getThreadCpuTime(target.getId()) : -1;
        } catch (RuntimeException ex) {
            return -1;
        }
    }

    @Override
    public void run() {
        long next = started + REPORT_NS;
        try {
            while (running && target.isAlive()) {
                sample();
                long now = System.nanoTime();
                if (now >= next) {
                    report("periodic");
                    next = now + REPORT_NS;
                }
                try {
                    Thread.sleep(PERIOD_MS);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        } finally {
            report(reason);
        }
    }

    private void sample() {
        StackTraceElement[] trace = target.getStackTrace();
        if (trace.length == 0) return;
        samples++;
        windowSamples++;
        leafMethods.merge(frame(trace[0]), 1, Integer::sum);
        String subsystem = StartupSubsystems.classify(trace);
        subsystems.merge(subsystem, 1, Integer::sum);
        windowSubsystems.merge(subsystem, 1, Integer::sum);
        states.merge(target.getState(), 1, Integer::sum);
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < Math.min(7, trace.length); i++) {
            if (i != 0) key.append(" <- ");
            key.append(frame(trace[i]));
        }
        stacks.merge(key.toString(), 1, Integer::sum);

        // Inclusive counts identify expensive callers even when their work is
        // actually executing in Minecraft, Java collections or other mods.
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (StackTraceElement element : trace) {
            String cls = element.getClassName();
            String method = element.getMethodName();
            if (cls.startsWith("Reika.") || cls.startsWith("cc.unilock.dragonfixes.")
                    || cls.startsWith("rwg.") || cls.startsWith("rwg2.") || cls.startsWith("rtg.")
                    || cls.startsWith("climateControl.") || cls.contains("ChunkProvider")
                    || cls.contains("AnvilChunkLoader") || cls.contains("RegionFile")
                    || cls.contains("GameRegistry") || cls.startsWith("micdoodle8.")
                    || cls.startsWith("ganymedes01.") || cls.contains("WorldGen")
                    || method.contains("dragonfixes") || method.contains("DragonFixes")) {
                seen.add(frame(element));
            }
        }
        for (String name : seen) inclusive.merge(name, 1, Integer::sum);
    }

    private static String frame(StackTraceElement element) {
        return element.getClassName() + "#" + element.getMethodName();
    }

    private void report(String phase) {
        long cpu = cpuTime();
        long cpuMs = cpu < 0 || cpuAtStart < 0 ? -1 : TimeUnit.NANOSECONDS.toMillis(cpu - cpuAtStart);
        DragonFixes.LOGGER.info("[WorldStartupProfiler] {} elapsed={}ms cpu={}ms samples={} states={}",
                phase, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), cpuMs, samples, states);
        logTop("subsystem", subsystems, 20, samples);
        logTop("window subsystem", windowSubsystems, 12, windowSamples);
        windowSubsystems.clear();
        windowSamples = 0;
        logTop("leaf", leafMethods, 12, samples);
        logTop("stack", stacks, 8, samples);
        logTop("inclusive", inclusive, 20, samples);
    }

    private void logTop(String label, Map<String, Integer> counts, int limit, int denominator) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));
        for (int i = 0; i < Math.min(limit, entries.size()); i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            DragonFixes.LOGGER.info("[WorldStartupProfiler] {} {}/{} {}", label,
                    entry.getValue(), denominator, entry.getKey());
        }
    }
}
