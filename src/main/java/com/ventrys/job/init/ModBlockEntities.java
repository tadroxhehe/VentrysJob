package com.ventrys.job.init;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.block.entity.SacSelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, VentrysJob.MOD_ID);

    // BlockEntities existants
    public static final RegistryObject<BlockEntityType<com.ventrys.job.block.entity.OuvrierFourBlockEntity>> OUVRIER_FOUR =
        BLOCK_ENTITIES.register("ouvrier_four", () -> BlockEntityType.Builder.of(
            com.ventrys.job.block.entity.OuvrierFourBlockEntity::new, ModBlocks.OUVRIER_FOUR.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.ventrys.job.block.entity.ForgeronFourBlockEntity>> FORGERON_FOUR =
        BLOCK_ENTITIES.register("forgeron_four", () -> BlockEntityType.Builder.of(
            com.ventrys.job.block.entity.ForgeronFourBlockEntity::new, ModBlocks.FORGERON_FOUR.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.ventrys.job.block.entity.VaseApothicaireBlockEntity>> VASE_APOTHICAIRE =
        BLOCK_ENTITIES.register("vase_apothicaire", () -> BlockEntityType.Builder.of(
            com.ventrys.job.block.entity.VaseApothicaireBlockEntity::new, ModBlocks.VASE_APOTHICAIRE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.ventrys.job.block.entity.MeuleBlockEntity>> MEULE =
        BLOCK_ENTITIES.register("meule", () -> BlockEntityType.Builder.of(
            com.ventrys.job.block.entity.MeuleBlockEntity::new, ModBlocks.MEULE.get()).build(null));

    // Tables de métier
    public static final RegistryObject<BlockEntityType<com.ventrys.job.block.entity.ForgeronTableBlockEntity>> FORGERON_TABLE =
        BLOCK_ENTITIES.register("forgeron_table", () -> BlockEntityType.Builder.of(
            com.ventrys.job.block.entity.ForgeronTableBlockEntity::new, ModBlocks.FORGERON_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.ventrys.job.block.entity.ArtisanTableBlockEntity>> ARTISAN_TABLE =
        BLOCK_ENTITIES.register("artisan_table", () -> BlockEntityType.Builder.of(
            com.ventrys.job.block.entity.ArtisanTableBlockEntity::new, ModBlocks.ARTISAN_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.ventrys.job.block.entity.ApothicaireTableBlockEntity>> APOTHICAIRE_TABLE =
        BLOCK_ENTITIES.register("apothicaire_table", () -> BlockEntityType.Builder.of(
            com.ventrys.job.block.entity.ApothicaireTableBlockEntity::new, ModBlocks.APOTHICAIRE_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.ventrys.job.block.entity.CuisinierTableBlockEntity>> CUISINIER_TABLE =
        BLOCK_ENTITIES.register("cuisinier_table", () -> BlockEntityType.Builder.of(
            com.ventrys.job.block.entity.CuisinierTableBlockEntity::new, ModBlocks.CUISINIER_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.ventrys.job.block.entity.MetierTisserBlockEntity>> METIER_TISSER =
        BLOCK_ENTITIES.register("metier_tisser", () -> BlockEntityType.Builder.of(
            com.ventrys.job.block.entity.MetierTisserBlockEntity::new, ModBlocks.METIER_TISSER.get()).build(null));

    // Sac à Sel
    public static final RegistryObject<BlockEntityType<SacSelBlockEntity>> SAC_SEL =
        BLOCK_ENTITIES.register("sac_sel", () -> BlockEntityType.Builder.of(
            SacSelBlockEntity::new, ModBlocks.SAC_SEL.get()).build(null));
    
    // Nid de poule
    public static final RegistryObject<BlockEntityType<com.ventrys.job.block.entity.ChickenNestBlockEntity>> CHICKEN_NEST =
        BLOCK_ENTITIES.register("chicken_nest", () -> BlockEntityType.Builder.of(
            com.ventrys.job.block.entity.ChickenNestBlockEntity::new, ModBlocks.CHICKEN_NEST.get()).build(null));
}