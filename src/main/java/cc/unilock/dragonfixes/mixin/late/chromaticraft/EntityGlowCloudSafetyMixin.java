package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.Entity.EntityGlowCloud;
import Reika.DragonAPI.Instantiable.Data.SphericalVector;
import cc.unilock.dragonfixes.DragonFixes;
import cc.unilock.dragonfixes.compat.GlowCloudCollisionSafety;
import cc.unilock.dragonfixes.compat.GlowCloudPhysics;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityGlowCloud.class)
public abstract class EntityGlowCloudSafetyMixin extends EntityLiving implements GlowCloudCollisionSafety {
    @Shadow(remap = false) private SphericalVector velocity;

    @Unique private boolean dragonfixes$hasSafePosition;
    @Unique private double dragonfixes$safeX, dragonfixes$safeY, dragonfixes$safeZ;
    @Unique private boolean dragonfixes$loggedRecovery;
    @Unique private int dragonfixes$lastRecoveryTick;

    protected EntityGlowCloudSafetyMixin(World world) { super(world); }

    // The inherited living tick runs BEFORE ChromatiCraft sets its next motion.
    // Repair loaded or externally corrupted state before any vanilla searches.
    @Inject(method = {"onUpdate", "onLivingUpdate"}, at = @At("HEAD"), require = 2)
    private void dragonfixes$beforeTick(CallbackInfo ci) {
        dragonfixes$repairState("before entity update", null);
    }

    // Bound the controller after the original *= 2 and before projection.
    // Do not change the random sequence, angular steering or normal motion.
    @WrapOperation(method = "onUpdate", at = @At(value = "INVOKE",
            target = "LReika/DragonAPI/Instantiable/Data/SphericalVector;getCartesian()[D", remap = false),
            require = 1)
    private double[] dragonfixes$boundedProjection(SphericalVector vector, Operation<double[]> original) {
        dragonfixes$sanitizeVector(vector);
        return original.call(vector);
    }

    @Inject(method = "onUpdate", at = @At("RETURN"), require = 1)
    private void dragonfixes$afterTick(CallbackInfo ci) {
        dragonfixes$repairState("after entity update", null);
    }

    @Unique
    private void dragonfixes$sanitizeVector(SphericalVector v) {
        if (v == null) return;
        double speed = GlowCloudPhysics.safeMagnitude(v.magnitude);
        double theta = GlowCloudPhysics.safeAngle(v.inclination, 90D);
        double phi = GlowCloudPhysics.safeAngle(v.rotation, 0D);
        // Ordinary ground lift reaching its cap is not a corrupt state.
        // Log only non-finite values here, not normal bounded acceleration.
        if (!Double.isFinite(v.magnitude) || !Double.isFinite(v.inclination) || !Double.isFinite(v.rotation)) {
            dragonfixes$logRecovery("non-finite spherical velocity", null);
        }
        v.magnitude = speed;
        v.inclination = theta;
        v.rotation = phi;
    }

    @Unique
    private boolean dragonfixes$validBody() {
        return GlowCloudPhysics.validBody(posX,posY,posZ,boundingBox.minX,boundingBox.minY,
                boundingBox.minZ,boundingBox.maxX,boundingBox.maxY,boundingBox.maxZ);
    }

    @Unique
    private void dragonfixes$repairState(String reason, AxisAlignedBB query) {
        boolean bodyOK = dragonfixes$validBody();
        boolean motionOK = GlowCloudPhysics.validMotion(motionX,motionY,motionZ);
        if (!bodyOK || !motionOK || query != null) {
            dragonfixes$logRecovery(reason, query);
        }
        if (!bodyOK) {
            double x = posX, y = posY, z = posZ;
            if (!GlowCloudPhysics.validPosition(x,y,z)) {
                if (dragonfixes$hasSafePosition) {
                    x=dragonfixes$safeX; y=dragonfixes$safeY; z=dragonfixes$safeZ;
                } else if (GlowCloudPhysics.validPosition(prevPosX,prevPosY,prevPosZ)) {
                    x=prevPosX; y=prevPosY; z=prevPosZ;
                } else {
                    ChunkCoordinates spawn = worldObj.getSpawnPoint();
                    x=spawn.posX+0.5D; y=spawn.posY+1D; z=spawn.posZ+0.5D;
                    if (!GlowCloudPhysics.validPosition(x,y,z)) { x=0.5D; y=64D; z=0.5D; }
                }
            }
            if (!Float.isFinite(width) || width <= 0F || width > 4F) width=0.25F;
            if (!Float.isFinite(height) || height <= 0F || height > 4F) height=0.25F;
            if (!Float.isFinite(yOffset) || Math.abs(yOffset)>4F) yOffset=0F;
            if (!Float.isFinite(ySize) || Math.abs(ySize)>4F) ySize=0F;
            setPosition(x,y,z);
            if (!dragonfixes$validBody()) {
                // An inconsistent offset/size combination can keep a rebuilt
                // box invalid. Restore the native cloud's intended dimensions.
                width=height=0.25F;
                yOffset=ySize=0F;
                setPosition(x,y,z);
            }
            prevPosX=lastTickPosX=x;
            prevPosY=lastTickPosY=y;
            prevPosZ=lastTickPosZ=z;
        }
        if (!motionOK || !bodyOK) {
            motionX=motionY=motionZ=0D;
            if (velocity != null) velocity.magnitude=0.15D;
            velocityChanged=true;
        }
        dragonfixes$sanitizeVector(velocity);
        if (dragonfixes$validBody()) {
            dragonfixes$hasSafePosition=true;
            dragonfixes$safeX=posX; dragonfixes$safeY=posY; dragonfixes$safeZ=posZ;
        }
    }

    @Override
    public void dragonfixes$recoverInvalidCollisionQuery(AxisAlignedBB query) {
        dragonfixes$repairState("rejected pathological entity collision query", query);
    }

    @Unique
    private void dragonfixes$logRecovery(String reason, AxisAlignedBB query) {
        if (dragonfixes$loggedRecovery && ticksExisted-dragonfixes$lastRecoveryTick < 200) return;
        dragonfixes$loggedRecovery=true;
        dragonfixes$lastRecoveryTick=ticksExisted;
        DragonFixes.LOGGER.warn("Glow Cloud recovery: {}; entity={}, dimension={}, side={}, position=({}, {}, {}), motion=({}, {}, {}), body={}, query={}, controller={}",
                reason,getEntityId(),worldObj.provider.dimensionId,worldObj.isRemote?"client":"server",
                posX,posY,posZ,motionX,motionY,motionZ,boundingBox,query,
                velocity==null?"null":velocity.magnitude+"/"+velocity.inclination+"/"+velocity.rotation);
    }
}
