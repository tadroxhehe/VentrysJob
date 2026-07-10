package com.ventrys.job.init;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.menu.SacSelMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = 
        DeferredRegister.create(ForgeRegistries.CONTAINERS, VentrysJob.MOD_ID);

    // Menus existants
    public static final RegistryObject<MenuType<com.ventrys.job.menu.OuvrierFourMenu>> OUVRIER_FOUR =
        MENUS.register("ouvrier_four", () -> IForgeMenuType.create((containerId, inventory, buffer) -> {
            net.minecraft.core.BlockPos pos = buffer.readBlockPos();
            com.ventrys.job.block.entity.OuvrierFourBlockEntity entity = null;
            if (inventory.player.level != null) {
                net.minecraft.world.level.block.entity.BlockEntity be = inventory.player.level.getBlockEntity(pos);
                if (be instanceof com.ventrys.job.block.entity.OuvrierFourBlockEntity) {
                    entity = (com.ventrys.job.block.entity.OuvrierFourBlockEntity) be;
                }
            }
            return new com.ventrys.job.menu.OuvrierFourMenu(containerId, inventory, entity, new net.minecraft.world.inventory.SimpleContainerData(3));
        }));

    public static final RegistryObject<MenuType<com.ventrys.job.menu.ForgeronFourMenu>> FORGERON_FOUR =
        MENUS.register("forgeron_four", () -> IForgeMenuType.create((containerId, inventory, buffer) -> {
            net.minecraft.core.BlockPos pos = buffer.readBlockPos();
            com.ventrys.job.block.entity.ForgeronFourBlockEntity entity = null;
            if (inventory.player.level != null) {
                net.minecraft.world.level.block.entity.BlockEntity be = inventory.player.level.getBlockEntity(pos);
                if (be instanceof com.ventrys.job.block.entity.ForgeronFourBlockEntity) {
                    entity = (com.ventrys.job.block.entity.ForgeronFourBlockEntity) be;
                }
            }
            return new com.ventrys.job.menu.ForgeronFourMenu(containerId, inventory, entity, new net.minecraft.world.inventory.SimpleContainerData(5));
        }));

    public static final RegistryObject<MenuType<com.ventrys.job.menu.JobTableMenu>> JOB_TABLE =
        MENUS.register("job_table", () -> IForgeMenuType.create((containerId, inventory, buffer) -> {
            net.minecraft.core.BlockPos pos = buffer.readBlockPos();
            String jobId = buffer.readUtf(128);
            return new com.ventrys.job.menu.JobTableMenu(containerId, inventory, pos, jobId);
        }));

    public static final RegistryObject<MenuType<com.ventrys.job.menu.MeuleMenu>> MEULE =
        MENUS.register("meule", () -> IForgeMenuType.create((containerId, inventory, buffer) ->
            new com.ventrys.job.menu.MeuleMenu(containerId, inventory, buffer.readBlockPos())));

    // Sac à Sel
    public static final RegistryObject<MenuType<SacSelMenu>> SAC_SEL =
        MENUS.register("sac_sel", () -> IForgeMenuType.create(SacSelMenu::new));
    
    // Nid de poule
    public static final RegistryObject<MenuType<com.ventrys.job.menu.ChickenNestMenu>> CHICKEN_NEST =
        MENUS.register("chicken_nest", () -> IForgeMenuType.create((containerId, inventory, buffer) ->
            new com.ventrys.job.menu.ChickenNestMenu(containerId, inventory, 
                inventory.player.level.getBlockEntity(buffer.readBlockPos()) instanceof com.ventrys.job.block.entity.ChickenNestBlockEntity nest ? nest : null,
                new net.minecraft.world.inventory.ContainerData() {
                    private final int[] values = new int[8];
                    @Override
                    public int get(int index) { return values[index]; }
                    @Override
                    public void set(int index, int value) { values[index] = value; }
                    @Override
                    public int getCount() { return values.length; }
                })));
}