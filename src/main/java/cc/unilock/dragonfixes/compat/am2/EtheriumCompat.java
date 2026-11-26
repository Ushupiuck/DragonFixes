package cc.unilock.dragonfixes.compat.am2;

import cc.unilock.dragonfixes.DragonFixes;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;

import java.util.concurrent.ThreadLocalRandom;

/** Optional AM2 restoration, linked only after all mod registrations complete. */
public final class EtheriumCompat {
    private static volatile Material material;

    private EtheriumCompat() {}

    public static void initialize() {
        if (!Loader.isModLoaded("arsmagica2")) return;
        try {
            Class<?> handler = Class.forName("am2.AMEventHandler", false,
                    EtheriumCompat.class.getClassLoader());
            try {
                handler.getDeclaredMethod("handleEtherMovement", EntityLivingBase.class);
                // Older AM2.5 builds already supply their own movement handler.
                DragonFixes.LOGGER.info("AM2 already has Etherium movement; leaving it unchanged");
                return;
            } catch (NoSuchMethodException missingHandler) {
                // The Legacy fork dropped this mechanic.
            }
            Block block = GameRegistry.findBlock("arsmagica2", "liquidEssence");
            if (block == null || block == Blocks.air || !(block instanceof IFluidBlock)) {
                DragonFixes.LOGGER.warn("AM2 Etherium block not found; movement restoration not activated");
                return;
            }
            Material candidate = block.getMaterial();
            if (!candidate.isLiquid() || candidate == Material.water || candidate == Material.lava) {
                DragonFixes.LOGGER.warn("AM2 Etherium no longer has a distinct liquid material; leaving movement unchanged");
                return;
            }
            material = candidate;
            DragonFixes.LOGGER.info("Restored Liquid Etherium movement and effects");
        } catch (ClassNotFoundException | LinkageError | SecurityException e) {
            DragonFixes.LOGGER.warn("Unable to inspect optional AM2 Etherium compatibility", e);
        }
    }

    public static boolean enabled() {
        return material != null;
    }

    public static boolean isEtherium(Material candidate) {
        return material != null && candidate == material;
    }

    public static boolean canSwim(EntityLivingBase entity) {
        return entity.ridingEntity == null
                && (!(entity instanceof EntityPlayer) || !((EntityPlayer) entity).capabilities.isFlying);
    }

    public static boolean isImmersed(Entity entity) {
        return contains(entity, false);
    }

    /** For impact protection and particles, use contact rather than the inset swimming volume. */
    public static boolean touches(Entity entity) {
        return contains(entity, true);
    }

    private static boolean contains(Entity entity, boolean contact) {
        Material target = material;
        World world = entity.worldObj;
        if (target == null || world == null) return false;
        AxisAlignedBB box = entity.boundingBox;
        if (box == null) return false;

        // Keep the old player's inset, but don't invert the box for tiny mobs.
        double x = contact ? 0 : Math.min(0.1D, (box.maxX - box.minX) * 0.25D);
        double y = contact ? 0 : Math.min(0.4D, (box.maxY - box.minY) * 0.25D);
        double z = contact ? 0 : Math.min(0.1D, (box.maxZ - box.minZ) * 0.25D);
        double minX = box.minX + x, minY = box.minY + y, minZ = box.minZ + z;
        double maxX = box.maxX - x - 1.0E-7D;
        double maxY = box.maxY - y - 1.0E-7D;
        double maxZ = box.maxZ - z - 1.0E-7D;
        if (maxX <= minX || maxY <= minY || maxZ <= minZ) return false;

        // A cosmetic/movement query must not cause chunk generation.
        if (!world.checkChunksExist(MathHelper.floor_double(minX), MathHelper.floor_double(minY),
                MathHelper.floor_double(minZ), MathHelper.floor_double(maxX),
                MathHelper.floor_double(maxY), MathHelper.floor_double(maxZ))) return false;

        // LiquidLogged extends this query to include fluid held inside blocks.
        return world.isMaterialInBB(AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ), target);
    }

    public static void protect(EntityLivingBase entity) {
        entity.fallDistance = 0;
        if (entity.isBurning()) entity.extinguish();
    }

    public static void effects(EntityLivingBase entity, boolean entered) {
        World world = entity.worldObj;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (!world.isRemote) {
            if (entered) {
                world.playSoundAtEntity(entity, "game.neutral.swim.splash", 0.13F,
                        1.0F + (random.nextFloat() - random.nextFloat()) * 0.5F);
            }
            return;
        }

        boolean moving = entity.motionX * entity.motionX + entity.motionZ * entity.motionZ > 0.0004D;
        int count = EtheriumPhysics.particleCount(entity.width, entered, moving,
                entity.ticksExisted, entity.getEntityId());
        double bubbleY = entity.boundingBox.minY + Math.min(0.6D, entity.height * 0.5D);
        double splashY = MathHelper.floor_double(entity.boundingBox.minY) + 1.0D;
        for (int i = 0; i < count; i++) {
            double x = entity.posX + (random.nextDouble() - 0.5D) * entity.width;
            double z = entity.posZ + (random.nextDouble() - 0.5D) * entity.width;
            world.spawnParticle("bubble", x, bubbleY, z, entity.motionX,
                    entity.motionY - random.nextDouble() * 0.2D, entity.motionZ);
            world.spawnParticle("splash", x, splashY, z, entity.motionX, entity.motionY, entity.motionZ);
        }
    }
}
