import cc.unilock.dragonfixes.compat.WorldgenChangeBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WorldgenChangeBufferTest {
    private static void check(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        WorldgenChangeBuffer<Object, Integer, Integer> buffer = new WorldgenChangeBuffer<>();
        Object a = new Object(), b = new Object();
        check(buffer.recording(a) == null, "idle recording");
        buffer.begin(a);
        buffer.begin(a);
        buffer.recording(a).put(1, 10);
        buffer.recording(a).put(1, 11);
        check(buffer.recording(b) == null, "cross-world recording");
        buffer.begin(b);
        buffer.recording(b).put(1, 20);
        buffer.finish(b, map -> check(map.get(1) == 20, "world B data"));
        List<Map<Integer, Integer>> retained = new ArrayList<>();
        buffer.finish(a, map -> {
            check(map.get(1) == 11, "last write wins");
            check(buffer.recording(a) == null, "watcher recursion guard");
            buffer.begin(a);
            buffer.finish(a, ignored -> { throw new AssertionError("nested redispatch"); });
            check(buffer.recording(a) == null, "outer guard remains");
            retained.add(map);
            try {
                map.put(2, 2);
                throw new AssertionError("mutable snapshot");
            } catch (UnsupportedOperationException expected) {}
        });
        check(buffer.recording(a) != null, "remaining generation depth");
        buffer.recording(a).put(1, 99);
        buffer.finish(a, map -> check(map.get(1) == 99, "subsequent population"));
        check(retained.get(0).get(1) == 11, "retained snapshot changed");
        check(buffer.recording(a) == null, "depth after finish");

        // Fresh world: identical copy iteration order across differently sized batches.
        Object orderWorld = new Object();
        HashMap<Integer, Integer> original = new HashMap<>();
        Random random = new Random(42);
        for (int count : new int[] {1, 1000, 2, 300, 1, 40}) {
            buffer.begin(orderWorld);
            for (int i = 0; i < count; i++) {
                int key = random.nextInt(10000);
                original.put(key, i);
                buffer.recording(orderWorld).put(key, i);
            }
            Map<Integer, Integer> expected = new HashMap<>(original);
            buffer.finish(orderWorld, map -> {
                check(map.equals(expected), "snapshot contents");
                check(new ArrayList<>(map.keySet()).equals(new ArrayList<>(expected.keySet())), "snapshot iteration order");
            });
            original.clear();
        }

        // Two threads with the same world and overlapping coordinates.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CyclicBarrier barrier = new CyclicBarrier(2);
            AtomicInteger callbacks = new AtomicInteger();
            List<Future<?>> results = new ArrayList<>();
            for (int id = 0; id < 2; id++) {
                final int value = id;
                results.add(executor.submit(() -> {
                    for (int pass = 0; pass < 50; pass++) {
                        buffer.begin(a);
                        for (int i = 0; i < 500; i++) buffer.recording(a).put(i, value);
                        barrier.await(10, TimeUnit.SECONDS);
                        buffer.finish(a, map -> {
                            check(map.size() == 500, "concurrent snapshot size");
                            for (int v : map.values()) check(v == value, "thread contamination");
                            callbacks.incrementAndGet();
                        });
                    }
                    return null;
                }));
            }
            for (Future<?> result : results) result.get(30, TimeUnit.SECONDS);
            check(callbacks.get() == 100, "callback count");

            CountDownLatch ready = new CountDownLatch(1), reset = new CountDownLatch(1);
            Future<?> worker = executor.submit(() -> {
                buffer.begin(a);
                buffer.recording(a).put(1, 5);
                ready.countDown();
                check(reset.await(10, TimeUnit.SECONDS), "reset timeout");
                check(buffer.recording(a) == null, "logout did not invalidate worker");
                return null;
            });
            check(ready.await(10, TimeUnit.SECONDS), "worker timeout");
            buffer.reset();
            reset.countDown();
            worker.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        buffer.begin(a);
        buffer.recording(a).put(1, 1);
        try {
            buffer.finish(a, map -> { throw new IllegalStateException("watcher"); });
            throw new AssertionError("swallowed watcher failure");
        } catch (IllegalStateException expected) {}
        buffer.begin(a);
        buffer.recording(a).put(2, 2);
        buffer.finish(a, map -> check(map.size() == 1 && map.containsKey(2), "exception cleanup"));
        System.out.println("Worldgen change buffer tests passed");
    }
}
