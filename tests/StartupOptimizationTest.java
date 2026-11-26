import cc.unilock.dragonfixes.compat.RtgMapGenIds;
import cc.unilock.dragonfixes.debug.StartupSubsystems;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;

public class StartupOptimizationTest {
    private static void check(boolean v) { if (!v) throw new AssertionError(); }
    private static StackTraceElement f(String c, String m) { return new StackTraceElement(c,m,"test",1); }
    public static void main(String[] args) {
        Random r = new Random(431308650295624829L);
        for (int t = 0; t < 4096; t++) {
            int[] ids = new int[256];
            for (int i=0;i<ids.length;i++) ids[i] = r.nextInt(t % 2 == 0 ? 4 : 65536);
            int[] before = ids.clone();
            TreeSet<Integer> expected = new TreeSet<>();
            for(int id:ids) expected.add(id);
            check(Arrays.equals(RtgMapGenIds.uniqueSorted(ids), expected.stream().mapToInt(Integer::intValue).toArray()));
            check(Arrays.equals(ids,before));
        }
        check(Arrays.equals(RtgMapGenIds.uniqueSorted(new int[0]),new int[0]));
        check("RTG map/volcano generation".equals(StartupSubsystems.classify(new StackTraceElement[]{
            f("java.util.HashMap","get"), f("rtg.world.biome.WorldChunkManagerRTG","getBiomeGenAt"),
            f("rtg.world.biome.realistic.RealisticBiomeBase","rMapVolcanoes")})));
        check("ChromatiCraft cliff shaping".equals(StartupSubsystems.classify(new StackTraceElement[]{
            f("climateControl.customGenLayer.GenLayerLowlandRiverMix","getInts"),
            f("Reika.ChromatiCraft.World.GlowingCliffsColumnShaper","getBiome")})));
        check("Caves/ravines".equals(StartupSubsystems.classify(new StackTraceElement[]{
            f("net.minecraft.world.gen.MapGenCaves","digBlock"), f("bwcrossmod.galacticraft.ChunkProviderRoss128b","provideChunk")})));
        System.out.println("Startup optimization checks passed (4096 distinct-biome comparisons)");
    }
}
