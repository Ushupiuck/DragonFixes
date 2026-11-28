package cc.unilock.dragonfixes.mixin.early.dragonapi;

import Reika.DragonAPI.Libraries.MathSci.ReikaPhysicsHelper;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.compat.ReikaNumericalSafety;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(value = ReikaPhysicsHelper.class, remap = false)
public abstract class ReikaPhysicsNumericalSafetyMixin {
    @Unique private static final AtomicBoolean dragonfixes$warnedTrajectory=new AtomicBoolean();

    @WrapMethod(method = {"cartesianToPolar","cartesianToPolarFast"})
    private static double[] dragonfixes$definedPolar(double x,double y,double z,Operation<double[]> original) {
        return ReikaNumericalSafety.repairPolar(x,y,z,original.call(x,y,z));
    }

    @WrapMethod(method = "targetPosn")
    private static double[] dragonfixes$boundedTrajectory(double x,double y,double z,double tx,double ty,double tz,
            double gravity,Operation<double[]> original) {
        if (ReikaNumericalSafety.finite(x,y,z) && ReikaNumericalSafety.finite(tx,ty,tz)) {
            try {
                double[] result=original.call(x,y,z,tx,ty,tz,gravity);
                if (ReikaNumericalSafety.finite(result[0],result[1],result[2])) return result;
            } catch (cc.unilock.dragonfixes.compat.TrajectoryBudgetExceeded expected) {
                // Only the explicit work-budget exception, not unrelated bugs.
            }
        }
        if (dragonfixes$warnedTrajectory.compareAndSet(false,true))
            DragonFixes.LOGGER.warn("DragonAPI trajectory had invalid inputs or exceeded its search budget; returning zero motion");
        return new double[3];
    }

    // Preserve the original trajectory formula and successful iteration
    // order exactly; abort only a search that exhausts its explicit budget.
    @WrapOperation(method = "targetPosn", at = @At(value = "INVOKE",
            target = "LReika/DragonAPI/Libraries/MathSci/ReikaMathLibrary;intpow(DI)D"), require = 1)
    private static double dragonfixes$trajectoryBudget(double velocity,int exponent,Operation<Double> original) {
        if (velocity > ReikaNumericalSafety.MAX_TRAJECTORY_SPEED)
            throw new cc.unilock.dragonfixes.compat.TrajectoryBudgetExceeded();
        return original.call(velocity,exponent);
    }
}
