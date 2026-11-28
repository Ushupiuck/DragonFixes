package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.API.AdjacencyUpgradeAPI.BlacklistReason;
import Reika.ChromatiCraft.TileEntity.AOE.Effect.TileEntityAccelerator;
import Reika.DragonAPI.Libraries.Registry.ReikaItemHelper;
import Reika.DragonAPI.ModList;
import cc.unilock.dragonfixes.compat.ProjectRedAcceleratorCompat;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "Reika.ChromatiCraft.TileEntity.AOE.Effect.TileEntityAccelerator", remap = false)
public abstract class ProjectRedAcceleratorMixin {
    // Keep the handbook entries but do not put gate logic classes in
    // ChromatiCraft's class-keyed TileEntity acceleration map.
    @Inject(method = "blacklistTile(Ljava/lang/String;LReika/DragonAPI/ModList;Ljava/lang/String;LReika/ChromatiCraft/API/AdjacencyUpgradeAPI$BlacklistReason;)V",
            at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private static void dragonfixes$replaceProjectRedRegistration(String name, ModList mod, String item,
            BlacklistReason reason, CallbackInfo ci) {
        if (mod == ModList.PROJRED && ProjectRedAcceleratorCompat.replacesLegacyRegistration(name)) {
            ItemStack stack = ReikaItemHelper.lookupItem(item);
            if (stack != null && stack.getItem() != null) {
                TileEntityAccelerator.addBlacklistItems(stack);
            }
            ci.cancel();
        }
    }

    // getAccelerate(null) returns the native blacklist sentinel. Only the
    // local lookup argument is changed; the actual tile/world is untouched.
    // This runs before the cached class lookup, so part changes are respected.
    @ModifyVariable(
            method = "getAccelerate(Lnet/minecraft/tileentity/TileEntity;)LReika/ChromatiCraft/TileEntity/AOE/Effect/TileEntityAccelerator$Acceleration;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1, remap = false)
    private static TileEntity dragonfixes$excludeProtectedMultipart(TileEntity tile) {
        return ProjectRedAcceleratorCompat.containsProtectedGate(tile) ? null : tile;
    }
}
