package com.ventrys.job.init;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomChicken;
import com.ventrys.job.entity.CustomCow;
import com.ventrys.job.entity.CustomPig;
import com.ventrys.job.entity.CustomSheep;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(ForgeRegistries.ENTITIES, VentrysJob.MOD_ID);
    
    public static final RegistryObject<EntityType<CustomPig>> CUSTOM_PIG = ENTITIES.register("custom_pig",
        () -> EntityType.Builder.of(CustomPig::new, MobCategory.CREATURE)
            .sized(0.9f, 0.9f)
            .clientTrackingRange(10)
            .build("custom_pig"));
    
    public static final RegistryObject<EntityType<CustomCow>> CUSTOM_COW = ENTITIES.register("custom_cow",
        () -> EntityType.Builder.of(CustomCow::new, MobCategory.CREATURE)
            .sized(0.9f, 1.4f)
            .clientTrackingRange(10)
            .build("custom_cow"));
    
    public static final RegistryObject<EntityType<CustomChicken>> CUSTOM_CHICKEN = ENTITIES.register("custom_chicken",
        () -> EntityType.Builder.of(CustomChicken::new, MobCategory.CREATURE)
            .sized(0.4f, 0.7f)
            .clientTrackingRange(10)
            .build("custom_chicken"));

    public static final RegistryObject<EntityType<CustomSheep>> CUSTOM_SHEEP = ENTITIES.register("custom_sheep",
        () -> EntityType.Builder.of(CustomSheep::new, MobCategory.CREATURE)
            .sized(0.9f, 1.3f)
            .clientTrackingRange(10)
            .build("custom_sheep"));
}

