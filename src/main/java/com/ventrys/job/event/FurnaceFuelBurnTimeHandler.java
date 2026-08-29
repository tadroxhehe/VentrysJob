package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Duree de combustion custom (RP) pour le charbon et le charbon de bois : bien plus courts qu'en
 * vanilla (1600 ticks/80s chacun), pour forcer un ravitaillement frequent du four.
 *
 * <p>{@link FurnaceFuelBurnTimeEvent} est poste par {@code ForgeHooks.getBurnTime(...)}, appele
 * depuis {@code AbstractFurnaceBlockEntity.getBurnDuration(ItemStack)} — AVANT que {@code litTime}
 * ne soit assigne cote vanilla. C'est en amont de tout ce qu'Arclight patch pour son propre
 * {@code FurnaceBurnEvent} (Bukkit) ; aucune des bizarreries d'Arclight rencontrees sur ce dernier
 * (cf. {@code AbstractFurnaceBlockEntityMixin.arclight$setBurnTime}, qui ne relit jamais
 * {@code event.getBurnTime()}) ne s'applique ici.</p>
 *
 * <p>Essaye d'abord en Skript (event Forge brut via un proxy skript-reflect sur
 * {@code MinecraftForge.EVENT_BUS}) : jamais declenche en pratique (mapping SRG vs noms Mojang
 * lisibles pour la reflexion pure runtime, contrairement au code Java compile via ForgeGradle qui
 * est remappe automatiquement) — direct en Java ici, aucun souci de mapping.</p>
 */
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public class FurnaceFuelBurnTimeHandler {

    private static final int CHARCOAL_BURN_TICKS = 25;  // 1.25s
    private static final int COAL_BURN_TICKS = 50;      // 2.5s

    @SubscribeEvent
    public static void onFurnaceFuelBurnTime(FurnaceFuelBurnTimeEvent event) {
        Item item = event.getItemStack().getItem();
        if (item == Items.CHARCOAL) {
            event.setBurnTime(CHARCOAL_BURN_TICKS);
        } else if (item == Items.COAL) {
            event.setBurnTime(COAL_BURN_TICKS);
        }
    }
}
