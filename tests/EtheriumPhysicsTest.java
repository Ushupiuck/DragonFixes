import cc.unilock.dragonfixes.compat.am2.EtheriumPhysics;

public class EtheriumPhysicsTest {
    private static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }

    public static void main(String[] args) {
        check(EtheriumPhysics.HORIZONTAL_DRAG == 0.5, "horizontal drag");
        check(EtheriumPhysics.VERTICAL_DRAG == 0.05, "vertical drag");
        check(EtheriumPhysics.verticalVelocity(0) == 0.05, "positive buoyancy");
        check(EtheriumPhysics.verticalVelocity(-0.5) > 0, "recover from descent");
        double speed = -4;
        for (int i = 0; i < 20; i++) speed = EtheriumPhysics.verticalVelocity(speed);
        check(Math.abs(speed - 0.05 / 0.95) < 1e-12, "stable terminal ascent");
        check(EtheriumPhysics.particleCount(0.6F, true, false, 0, 0) == 13, "entry burst");
        check(EtheriumPhysics.particleCount(50, true, true, 0, 0) == 24, "bounded large mobs");
        int moving = 0, idle = 0;
        for (int tick = 0; tick < 20; tick++) {
            moving += EtheriumPhysics.particleCount(0.6F, false, true, tick, 9);
            idle += EtheriumPhysics.particleCount(0.6F, false, false, tick, 9);
        }
        check(moving == 10 && idle == 5, "staggered particle budget");
        System.out.println("Etherium movement profile and particle budget tests passed");
    }
}
