package cc.unilock.dragonfixes.compat.am2;

import cc.unilock.dragonfixes.DragonFixes;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Restores AM2's original Waila disguise behavior without an AM2 compile dependency.
 * Resolve the optional API once at registration, not on each tooltip lookup.
 */
public final class AM2WailaCompat implements IWailaDataProvider {
    private final Class<?> illusionBlock;
    private final Class<?> everstoneTile;
    private final Method mimicLocation, mimicBlock, alwaysPassable;
    private final Method facade, facadeMeta, solid;
    private final Field x, y, z, trueSight;
    private boolean failureLogged;

    private AM2WailaCompat() throws ReflectiveOperationException {
        ClassLoader loader = Launch.classLoader;
        illusionBlock = Class.forName("am2.blocks.IllusionBlock", false, loader);
        everstoneTile = Class.forName("am2.blocks.tileentities.TileEntityEverstone", false, loader);
        Class<?> vector = Class.forName("am2.api.math.AMVector3", false, loader);
        mimicLocation = illusionBlock.getMethod("getMimicLocation", IBlockAccess.class, int.class, int.class, int.class);
        mimicBlock = illusionBlock.getMethod("getMimicBlock", IBlockAccess.class, vector);
        alwaysPassable = illusionBlock.getMethod("alwaysPassable", IBlockAccess.class, int.class, int.class, int.class);
        facade = everstoneTile.getMethod("getFacade");
        facadeMeta = everstoneTile.getMethod("getFacadeMeta");
        solid = everstoneTile.getMethod("isSolid");
        x = vector.getField("x");
        y = vector.getField("y");
        z = vector.getField("z");
        trueSight = Class.forName("am2.buffs.BuffList", false, loader).getField("trueSight");
    }

    public static void callbackRegister(IWailaRegistrar registrar) {
        try {
            AM2WailaCompat provider = new AM2WailaCompat();
            Class<?> everstoneBlock = Class.forName("am2.blocks.BlockEverstone", false, Launch.classLoader);
            registrar.registerStackProvider(provider, provider.illusionBlock);
            registrar.registerStackProvider(provider, everstoneBlock);
            DragonFixes.LOGGER.info("Restored AM2 Waila disguise providers");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            DragonFixes.LOGGER.error("Unable to register AM2 Waila compatibility", e);
        }
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        try {
            Block actual = accessor.getBlock();
            TileEntity te = accessor.getTileEntity();
            if (everstoneTile.isInstance(te)) {
                if (!Boolean.TRUE.equals(solid.invoke(te))) return null;
                return displayStack((Block) facade.invoke(te), ((Number) facadeMeta.invoke(te)).intValue(), actual);
            }
            if (!illusionBlock.isInstance(actual)) return null;
            World world = accessor.getWorld();
            MovingObjectPosition pos = accessor.getPosition();
            if (world == null || pos == null) return null;
            EntityPlayer player = accessor.getPlayer();
            Object potion = trueSight.get(null);
            // Match IllusionBlock.getIcon: true sight reveals ordinary illusions,
            // but the ethereal/always-passable variant keeps its facade.
            if (player != null && potion instanceof Potion && player.isPotionActive((Potion) potion)
                    && !Boolean.TRUE.equals(alwaysPassable.invoke(actual, world, pos.blockX, pos.blockY, pos.blockZ))) {
                return null;
            }
            Object location = mimicLocation.invoke(actual, world, pos.blockX, pos.blockY, pos.blockZ);
            if (location == null) return null;
            Block mimic = (Block) mimicBlock.invoke(actual, world, location);
            if (mimic == null || mimic == Blocks.air || mimic == actual) return null;
            int mx = (int) x.getFloat(location);
            int my = (int) y.getFloat(location);
            int mz = (int) z.getFloat(location);
            if (!world.blockExists(mx, my, mz)) return null;
            return displayStack(mimic, world.getBlockMetadata(mx, my, mz), actual);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            if (!failureLogged) {
                failureLogged = true;
                DragonFixes.LOGGER.warn("AM2 Waila disguise lookup failed; using normal Waila display", e);
            }
            return null;
        }
    }

    private static ItemStack displayStack(Block mimic, int meta, Block actual) {
        if (mimic == null || mimic == Blocks.air || mimic == actual || meta < 0) return null;
        Item item = Item.getItemFromBlock(mimic);
        return item == null ? null : new ItemStack(item, 1, meta);
    }

    @Override
    public List<String> getWailaHead(ItemStack stack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return tip;
    }

    @Override
    public List<String> getWailaBody(ItemStack stack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return tip;
    }

    @Override
    public List<String> getWailaTail(ItemStack stack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return tip;
    }

    @Override
    public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x, int y, int z) {
        return tag;
    }
}
