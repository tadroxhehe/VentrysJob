package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Retire les graines de blé (et autres) des tables de butin de l'herbe / fougères vanilla.
 *
 * <p>On reconstruit ces tables par code plutôt que de se reposer uniquement sur la surcharge
 * de datapack (data/minecraft/loot_tables/...), car l'ordre de chargement des datapacks d'un mod
 * sur le namespace vanilla n'est pas garanti. {@link LootTableLoadEvent} s'exécute après le
 * chargement de toutes les tables et garantit que le drop de graines est supprimé.</p>
 *
 * <p>Comportement conservé : à la cisaille, l'herbe/fougère lâche toujours la plante elle-même
 * (1× pour l'herbe courte / fougère, 2× pour les versions doubles), comme en vanilla.
 * Sans cisaille (casse à la main ou retrait du bloc support), plus aucun drop.</p>
 */
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public class GrassSeedDropHandler {

    private static final ResourceLocation GRASS = new ResourceLocation("minecraft", "blocks/grass");
    private static final ResourceLocation FERN = new ResourceLocation("minecraft", "blocks/fern");
    private static final ResourceLocation TALL_GRASS = new ResourceLocation("minecraft", "blocks/tall_grass");
    private static final ResourceLocation LARGE_FERN = new ResourceLocation("minecraft", "blocks/large_fern");

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (GRASS.equals(name)) {
            event.setTable(plantTable(Blocks.GRASS, 1));
        } else if (FERN.equals(name)) {
            event.setTable(plantTable(Blocks.FERN, 1));
        } else if (TALL_GRASS.equals(name)) {
            event.setTable(plantTable(Blocks.GRASS, 2));
        } else if (LARGE_FERN.equals(name)) {
            event.setTable(plantTable(Blocks.FERN, 2));
        }
    }

    /**
     * Table de butin minimaliste : ne lâche {@code plant} (en quantité {@code count}) que
     * lorsqu'une cisaille est utilisée, et rien sinon. Aucune graine.
     */
    private static LootTable plantTable(ItemLike plant, int count) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(plant)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(count)))
                        .when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(Items.SHEARS))));
        return LootTable.lootTable()
                .setParamSet(LootContextParamSets.BLOCK)
                .withPool(pool)
                .build();
    }
}
