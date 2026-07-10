package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.DecoJobTableRegistry;
import com.ventrys.job.util.JobTableInteractionHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public final class DecoJobTableInteractionHandler {

    private DecoJobTableInteractionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getWorld().isClientSide()) {
            return;
        }

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(event.getWorld().getBlockState(event.getPos()).getBlock());
        String jobId = DecoJobTableRegistry.getJobId(blockId);
        if (jobId == null) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        InteractionResult result = JobTableInteractionHelper.tryOpen(
                player, event.getWorld(), event.getPos(), jobId);
        if (result.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }
}
