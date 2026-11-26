import cc.unilock.dragonfixes.compat.WorldgenChangeBuffer;
import java.util.*;
import java.util.concurrent.*;

public class WorldgenChangeBufferTest {
    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws Exception {
        WorldgenChangeBuffer<Integer, Integer> a = new WorldgenChangeBuffer<>();
        WorldgenChangeBuffer<Integer, Integer> b = new WorldgenChangeBuffer<>();
        a.put(0, 0);
        check(!a.isRecording(), "idle buffer");
        a.begin();
        a.begin();
        a.put(1, 10);
        a.put(1, 11);
        b.begin();
        b.put(1, 20);
        b.finish(map -> check(map.get(1) == 20, "world B"));
        List<Map<Integer, Integer>> retained = new ArrayList<>();
        a.finish(map -> {
            check(map.size() == 1 && map.get(1) == 11, "world A and last write");
            check(!a.isRecording(), "callback guard");
            a.put(2, 2);
            a.begin();
            a.finish(m -> { throw new AssertionError("nested redispatch"); });
            check(!a.isRecording(), "nested guard");
            retained.add(map);
            try {
                map.put(3, 3);
                throw new AssertionError("mutable snapshot");
            } catch (UnsupportedOperationException expected) {}
        });
        check(a.isRecording(), "remaining depth");
        a.put(1, 99);
        a.finish(map -> check(map.size() == 1 && map.get(1) == 99, "next batch"));
        check(retained.get(0).get(1) == 11 && !a.isRecording(), "retained snapshot");

        WorldgenChangeBuffer<Integer, Integer> order = new WorldgenChangeBuffer<>();
        HashMap<Integer, Integer> original = new HashMap<>();
        Random random = new Random(42);
        for (int count : new int[] {1, 1000, 2, 300, 1, 40}) {
            order.begin();
            for (int i = 0; i < count; i++) {
                int key = random.nextInt(10000);
                original.put(key, i);
                order.put(key, i);
            }
            Map<Integer, Integer> expected = new HashMap<>(original);
            order.finish(map -> {
                check(map.equals(expected), "snapshot contents");
                check(new ArrayList<>(map.keySet()).equals(new ArrayList<>(expected.keySet())),
                        "snapshot ordering");
            });
            original.clear();
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // A begin/record on one thread must be visible to another.
            executor.submit(() -> { a.begin(); a.put(7, 8); }).get(10, TimeUnit.SECONDS);
            check(a.isRecording(), "cross-thread begin");
            a.finish(map -> check(map.get(7) == 8, "cross-thread handoff"));

            // Keep a pending generation while another thread repeatedly drains.
            // Every unique key must be delivered exactly once, including writes
            // that occur while an earlier watcher is running.
            a.begin();
            CountDownLatch start = new CountDownLatch(1);
            Future<?> writer = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 20000; i++) a.put(i, i);
                return null;
            });
            Set<Integer> seen = new HashSet<>();
            start.countDown();
            do {
                a.begin();
                a.finish(map -> {
                    for (int key : map.keySet()) check(seen.add(key), "duplicate delivery");
                    Thread.yield();
                });
            } while (!writer.isDone());
            writer.get(10, TimeUnit.SECONDS);
            a.finish(map -> {
                for (int key : map.keySet()) check(seen.add(key), "duplicate final delivery");
            });
            check(seen.size() == 20000 && !a.isRecording(), "lost concurrent writes");

            a.begin();
            a.begin();
            a.put(1, 1);
            a.finish(map -> {
                try {
                    // If finish holds a lock around callbacks, this deadlocks.
                    executor.submit(() -> a.put(2, 2)).get(10, TimeUnit.SECONDS);
                } catch (Exception e) { throw new AssertionError(e); }
            });
            a.finish(map -> check(map.size() == 1 && map.get(2) == 2, "write during callback"));
        } finally {
            executor.shutdownNow();
        }

        a.begin();
        a.put(1, 1);
        try {
            a.finish(map -> { throw new IllegalStateException("watcher"); });
            throw new AssertionError("swallowed watcher failure");
        } catch (IllegalStateException expected) {}
        a.begin();
        a.put(2, 2);
        a.finish(map -> check(map.size() == 1 && map.containsKey(2), "exception cleanup"));
        System.out.println("World-owned population buffer tests passed");
    }
}
