package com.ventrys.job.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Les bols de soupe ({@code ventrysitem}) peuvent encore utiliser {@link SoundEvents#GENERIC_EAT}.
 * Remplace ce son par un son de boisson pendant la consommation.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VentrysSoupEatSoundReplacement {

    private VentrysSoupEatSoundReplacement() {
    }

    @SubscribeEvent
    public static void onSoundAtEntity(PlaySoundAtEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getSound() != SoundEvents.GENERIC_EAT) {
            return;
        }
        ItemStack consuming = player.getUseItem();
        if (consuming.isEmpty() || !isVentrySoupItem(consuming)) {
            return;
        }
        event.setSound(SoundEvents.HONEY_DRINK);
    }

    private static boolean isVentrySoupItem(ItemStack stack) {
        ResourceLocation key = stack.getItem().getRegistryName();
        if (key == null || !"ventrysitem".equals(key.getNamespace())) {
            return false;
        }
        String path = key.getPath();
        return path.contains("soupe");
    }
}
