package com.ventrys.job.util;

import com.ventrys.job.block.JobTableBlock;
import com.ventrys.job.block.entity.JobTableBlockEntity;
import com.ventrys.job.data.DecoJobTableRegistry;
import com.ventrys.job.data.JobManager;
import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.menu.JobTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Ouverture unifiée des GUI de tables de métier (blocs VentrysJob et blocs déco VentrysDeco).
 */
public final class JobTableInteractionHelper {

    private JobTableInteractionHelper() {
    }

    public static InteractionResult tryOpen(Player player, Level level, BlockPos pos, String jobId) {
        if (level.isClientSide || jobId == null || jobId.isEmpty()) {
            return InteractionResult.SUCCESS;
        }

        if (!player.isCreative() && !PlayerJobData.canAccessJobTable(player, jobId)) {
            player.displayClientMessage(wrongJobMessage(jobId), true);
            return InteractionResult.FAIL;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        NetworkHooks.openGui(serverPlayer, new net.minecraft.world.SimpleMenuProvider(
                (containerId, inventory, ignored) -> new JobTableMenu(containerId, inventory, pos, jobId),
                JobTableBlockEntity.titleForJob(jobId)
        ), buf -> {
            buf.writeBlockPos(pos);
            buf.writeUtf(jobId);
        });

        return InteractionResult.SUCCESS;
    }

    public static boolean isJobTableBlock(BlockState state, String jobId) {
        if (state == null || jobId == null) {
            return false;
        }

        if (state.getBlock() instanceof JobTableBlock tableBlock) {
            return jobId.equals(tableBlock.getJobId());
        }

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return jobId.equals(DecoJobTableRegistry.getJobId(blockId));
    }

    public static InteractionResult tryOpenJobTableBlock(Player player, Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof JobTableBlock tableBlock)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            blockEntity = tableBlock.newBlockEntity(pos, state);
            if (blockEntity != null) {
                level.setBlockEntity(blockEntity);
            }
        }

        return tryOpen(player, level, pos, tableBlock.getJobId());
    }

    private static Component wrongJobMessage(String requiredJobId) {
        if ("forgeron".equals(requiredJobId)) {
            return new TranslatableComponent("ventrysjob.message.furnace.forgeron.must_be_blacksmith");
        }
        if ("couturier".equals(requiredJobId)) {
            return new TranslatableComponent("ventrysjob.message.metier.must_be_tailor");
        }
        if ("apothicaire".equals(requiredJobId)) {
            return new TranslatableComponent("ventrysjob.message.metier.must_be_apothecary");
        }
        if ("cuisinier".equals(requiredJobId)) {
            return new TranslatableComponent("ventrysjob.message.job_table.must_be_cook");
        }
        if ("artisan".equals(requiredJobId)) {
            return new TranslatableComponent("ventrysjob.message.job_table.must_be_artisan");
        }

        String jobName = requiredJobId;
        var job = JobManager.getJob(requiredJobId);
        if (job != null && job.getName() != null) {
            jobName = job.getName();
        }
        return new TranslatableComponent("ventrysjob.message.craft.must_be_job", jobName);
    }
}
