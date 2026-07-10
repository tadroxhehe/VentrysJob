package com.ventrys.job.init;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.block.ModCropBlock;
import com.ventrys.job.block.OreBlock;
import com.ventrys.job.block.SacSelBlock;
import com.ventrys.job.block.MeuleBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    /** Graines VentrysItem (items) — utilisées comme référence pour les drops / recettes de culture. */
    private static ItemLike seedFromVentrysItem(String itemId) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        return item != null ? item : Items.WHEAT_SEEDS;
    }

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties cropProperties() {
        return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(Material.PLANT)
            .noCollission()
            .randomTicks()
            .strength(0f)
            .sound(SoundType.CROP);
    }

    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(ForgeRegistries.BLOCKS, VentrysJob.MOD_ID);
    
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, VentrysJob.MOD_ID);

    // Onglet créatif personnalisé pour VentrysJobs
    public static final CreativeModeTab VENTRYS_JOBS_TAB = new CreativeModeTab("ventrysjobs") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(FORGERON_TABLE.get());
        }
    };

    private static CreativeModeTab mobSpawnEggTab() {
        return VentrysMobCreativeTab.getOrFallback(VENTRYS_JOBS_TAB);
    }

    // Blocs existants
    public static final RegistryObject<Block> OUVRIER_FOUR = BLOCKS.register("ouvrier_four",
        () -> new com.ventrys.job.block.OuvrierFourBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.5f)
                .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> FORGERON_FOUR = BLOCKS.register("forgeron_four",
        () -> new com.ventrys.job.block.ForgeronFourBlock());

    public static final RegistryObject<Block> VASE_APOTHICAIRE = BLOCKS.register("vase_apothicaire",
        () -> new com.ventrys.job.block.VaseApothicaireBlock());

    public static final RegistryObject<Block> MEULE = BLOCKS.register("meule",
        () -> new MeuleBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));

    // Tables de métier
    public static final RegistryObject<Block> FORGERON_TABLE = BLOCKS.register("forgeron_table",
        () -> new com.ventrys.job.block.ForgeronTableBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.WOOD)
                .strength(2.0f)
                .noOcclusion()));

    public static final RegistryObject<Block> ARTISAN_TABLE = BLOCKS.register("artisan_table",
        () -> new com.ventrys.job.block.ArtisanTableBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.WOOD)
                .strength(2.0f)
                .noOcclusion()));

    public static final RegistryObject<Block> APOTHICAIRE_TABLE = BLOCKS.register("apothicaire_table",
        () -> new com.ventrys.job.block.ApothicaireTableBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.WOOD)
                .strength(2.0f)
                .noOcclusion()));

    public static final RegistryObject<Block> CUISINIER_TABLE = BLOCKS.register("cuisinier_table",
        () -> new com.ventrys.job.block.CuisinierTableBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.WOOD)
                .strength(2.0f)
                .noOcclusion()));

    public static final RegistryObject<Block> METIER_TISSER = BLOCKS.register("metier_tisser",
        () -> new com.ventrys.job.block.MetierTisserBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.WOOD)
                .strength(2.0f)
                .noOcclusion()));

    // Sac à Sel
    public static final RegistryObject<Block> SAC_SEL = BLOCKS.register("sac_sel",
        () -> new SacSelBlock());

    // Block Items - TOUS dans l'onglet VentrysJobs
    public static final RegistryObject<Item> OUVRIER_FOUR_ITEM = ITEMS.register("ouvrier_four",
        () -> new BlockItem(OUVRIER_FOUR.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));

    public static final RegistryObject<Item> FORGERON_FOUR_ITEM = ITEMS.register("forgeron_four",
        () -> new BlockItem(FORGERON_FOUR.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));

    public static final RegistryObject<Item> VASE_APOTHICAIRE_ITEM = ITEMS.register("vase_apothicaire",
        () -> new BlockItem(VASE_APOTHICAIRE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));

    public static final RegistryObject<Item> MEULE_ITEM = ITEMS.register("meule",
        () -> new BlockItem(MEULE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));

    public static final RegistryObject<Item> FORGERON_TABLE_ITEM = ITEMS.register("forgeron_table",
        () -> new BlockItem(FORGERON_TABLE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));

    public static final RegistryObject<Item> ARTISAN_TABLE_ITEM = ITEMS.register("artisan_table",
        () -> new BlockItem(ARTISAN_TABLE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));

    public static final RegistryObject<Item> APOTHICAIRE_TABLE_ITEM = ITEMS.register("apothicaire_table",
        () -> new BlockItem(APOTHICAIRE_TABLE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));

    public static final RegistryObject<Item> CUISINIER_TABLE_ITEM = ITEMS.register("cuisinier_table",
        () -> new BlockItem(CUISINIER_TABLE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));

    public static final RegistryObject<Item> METIER_TISSER_ITEM = ITEMS.register("metier_tisser",
        () -> new BlockItem(METIER_TISSER.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));

    public static final RegistryObject<Item> SAC_SEL_ITEM = ITEMS.register("sac_sel",
        () -> new BlockItem(SAC_SEL.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    // Nid de poule
    public static final RegistryObject<Block> CHICKEN_NEST = BLOCKS.register("chicken_nest",
        () -> new com.ventrys.job.block.ChickenNestBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.WOOD)
                .strength(2.0f)
                .noOcclusion()));
    
    public static final RegistryObject<Item> CHICKEN_NEST_ITEM = ITEMS.register("chicken_nest",
        () -> new BlockItem(CHICKEN_NEST.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    // Œufs de spawn pour les animaux
    public static final RegistryObject<Item> CUSTOM_PIG_SPAWN_EGG = ITEMS.register("custom_pig_spawn_egg",
        () -> new com.ventrys.job.item.CustomSpawnEggItem(
            () -> com.ventrys.job.init.ModEntities.CUSTOM_PIG.get(),
            new Item.Properties().tab(mobSpawnEggTab())));
    
    public static final RegistryObject<Item> CUSTOM_COW_SPAWN_EGG = ITEMS.register("custom_cow_spawn_egg",
        () -> new com.ventrys.job.item.CustomSpawnEggItem(
            () -> com.ventrys.job.init.ModEntities.CUSTOM_COW.get(),
            new Item.Properties().tab(mobSpawnEggTab())));
    
    public static final RegistryObject<Item> CUSTOM_CHICKEN_SPAWN_EGG = ITEMS.register("custom_chicken_spawn_egg",
        () -> new com.ventrys.job.item.CustomSpawnEggItem(
            () -> com.ventrys.job.init.ModEntities.CUSTOM_CHICKEN.get(),
            new Item.Properties().tab(mobSpawnEggTab())));

    public static final RegistryObject<Item> CUSTOM_SHEEP_SPAWN_EGG = ITEMS.register("custom_sheep_spawn_egg",
        () -> new com.ventrys.job.item.CustomSpawnEggItem(
            () -> com.ventrys.job.init.ModEntities.CUSTOM_SHEEP.get(),
            new Item.Properties().tab(mobSpawnEggTab())));
    
    // Blocs de minerais
    public static final RegistryObject<Block> ARGENT_ORE = BLOCKS.register("argent_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));
    
    public static final RegistryObject<Item> ARGENT_ORE_ITEM = ITEMS.register("argent_ore",
        () -> new BlockItem(ARGENT_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    public static final RegistryObject<Block> CHARBON_ORE = BLOCKS.register("charbon_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));
    
    public static final RegistryObject<Item> CHARBON_ORE_ITEM = ITEMS.register("charbon_ore",
        () -> new BlockItem(CHARBON_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    public static final RegistryObject<Block> CUIVRE_ORE = BLOCKS.register("cuivre_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));
    
    public static final RegistryObject<Item> CUIVRE_ORE_ITEM = ITEMS.register("cuivre_ore",
        () -> new BlockItem(CUIVRE_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    public static final RegistryObject<Block> DIAMANT_ORE = BLOCKS.register("diamant_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));
    
    public static final RegistryObject<Item> DIAMANT_ORE_ITEM = ITEMS.register("diamant_ore",
        () -> new BlockItem(DIAMANT_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    public static final RegistryObject<Block> EMERAUDE_ORE = BLOCKS.register("emeraude_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));
    
    public static final RegistryObject<Item> EMERAUDE_ORE_ITEM = ITEMS.register("emeraude_ore",
        () -> new BlockItem(EMERAUDE_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    public static final RegistryObject<Block> ETAIN_ORE = BLOCKS.register("etain_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));
    
    public static final RegistryObject<Item> ETAIN_ORE_ITEM = ITEMS.register("etain_ore",
        () -> new BlockItem(ETAIN_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    public static final RegistryObject<Block> FER_ORE = BLOCKS.register("fer_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));
    
    public static final RegistryObject<Item> FER_ORE_ITEM = ITEMS.register("fer_ore",
        () -> new BlockItem(FER_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    public static final RegistryObject<Block> OR_ORE = BLOCKS.register("or_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));
    
    public static final RegistryObject<Item> OR_ORE_ITEM = ITEMS.register("or_ore",
        () -> new BlockItem(OR_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    public static final RegistryObject<Block> RUBIS_ORE = BLOCKS.register("rubis_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));
    
    public static final RegistryObject<Item> RUBIS_ORE_ITEM = ITEMS.register("rubis_ore",
        () -> new BlockItem(RUBIS_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    public static final RegistryObject<Block> SAPHIRE_ORE = BLOCKS.register("saphire_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(3.0f)
                .requiresCorrectToolForDrops()));
    
    public static final RegistryObject<Item> SAPHIRE_ORE_ITEM = ITEMS.register("saphire_ore",
        () -> new BlockItem(SAPHIRE_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));

    public static final RegistryObject<Block> VERDRAGON_ORE = BLOCKS.register("verdragon_ore",
        () -> new OreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of(net.minecraft.world.level.material.Material.STONE)
                .strength(8.0f, 12.0f)
                .requiresCorrectToolForDrops()));

    public static final RegistryObject<Item> VERDRAGON_ORE_ITEM = ITEMS.register("verdragon_ore",
        () -> new BlockItem(VERDRAGON_ORE.get(), new Item.Properties().tab(VENTRYS_JOBS_TAB)));
    
    // Cultures moddées : blocs VentrysJob (textures paysans/), graines = items VentrysItem uniquement
    public static final RegistryObject<Block> CROP_ORGE = BLOCKS.register("crop_orge",
        () -> new ModCropBlock(() -> seedFromVentrysItem("ventrysitem:item_graine_orge"), cropProperties()));
    public static final RegistryObject<Block> CROP_TOMATES = BLOCKS.register("crop_tomates",
        () -> new ModCropBlock(() -> seedFromVentrysItem("ventrysitem:item_graine_tomates"), cropProperties()));
    public static final RegistryObject<Block> CROP_OIGNON = BLOCKS.register("crop_oignon",
        () -> new ModCropBlock(() -> seedFromVentrysItem("ventrysitem:item_graine_doignon"), cropProperties()));
    public static final RegistryObject<Block> CROP_SALADE = BLOCKS.register("crop_salade",
        () -> new ModCropBlock(() -> seedFromVentrysItem("ventrysitem:item_graine_salade"), cropProperties()));
    public static final RegistryObject<Block> CROP_RAISIN = BLOCKS.register("crop_raisin",
        () -> new ModCropBlock(() -> seedFromVentrysItem("ventrysitem:item_graine_raisin"), cropProperties()));
    public static final RegistryObject<Block> CROP_CHOUX = BLOCKS.register("crop_choux",
        () -> new ModCropBlock(() -> seedFromVentrysItem("ventrysitem:item_graine_choux"), cropProperties()));
    public static final RegistryObject<Block> CROP_CAROTTE = BLOCKS.register("crop_carotte",
        () -> new ModCropBlock(() -> seedFromVentrysItem("ventrysitem:item_graine_carotte"), cropProperties()));
    public static final RegistryObject<Block> CROP_BETRAVE = BLOCKS.register("crop_betrave",
        () -> new ModCropBlock(() -> seedFromVentrysItem("ventrysitem:item_graine_betrave"), cropProperties()));

    // Note: La fourche est maintenant configurée via extraction_config.json
    // Les items vanilla (ex: minecraft:yellow_dye) ou moddés peuvent être utilisés
}