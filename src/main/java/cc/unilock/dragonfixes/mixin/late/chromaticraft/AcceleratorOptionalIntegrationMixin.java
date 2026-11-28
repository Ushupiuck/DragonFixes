package cc.unilock.dragonfixes.mixin.late.chromaticraft;

import Reika.ChromatiCraft.API.AdjacencyUpgradeAPI.BlacklistReason;
import Reika.DragonAPI.ModList;
import cpw.mods.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The original Torcherino registrations pass a null ModList entry. */
@Mixin(targets = "Reika.ChromatiCraft.TileEntity.AOE.Effect.TileEntityAccelerator", remap = false)
public abstract class AcceleratorOptionalIntegrationMixin {
    @Inject(method = "blacklistTile(Ljava/lang/String;LReika/DragonAPI/ModList;Ljava/lang/String;LReika/ChromatiCraft/API/AdjacencyUpgradeAPI$BlacklistReason;)V",
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void dragonfixes$skipAbsentTorcherino(String name, ModList mod, String item,
            BlacklistReason reason, CallbackInfo ci) {
        if ("com.sci.torcherino.tile.TileTorcherino".equals(name) && !Loader.isModLoaded("Torcherino")) {
            ci.cancel();
        }
    }
}
