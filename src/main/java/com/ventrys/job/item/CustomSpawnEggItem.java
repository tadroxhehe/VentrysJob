package com.ventrys.job.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import java.util.function.Supplier;

public class CustomSpawnEggItem extends Item {
    private final Supplier<EntityType<? extends Mob>> entityTypeSupplier;
    
    public CustomSpawnEggItem(Supplier<EntityType<? extends Mob>> entityTypeSupplier, Item.Properties properties) {
        super(properties);
        this.entityTypeSupplier = entityTypeSupplier;
    }
    
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        
        ItemStack itemstack = context.getItemInHand();
        BlockPos blockpos = context.getClickedPos();
        net.minecraft.core.Direction direction = context.getClickedFace();
        EntityType<? extends Mob> entityType = entityTypeSupplier.get();
        Mob mob = entityType.create(serverLevel);
        
        if (mob != null) {
            Vec3 vec3 = Vec3.atBottomCenterOf(blockpos.relative(direction));
            mob.moveTo(vec3.x, vec3.y, vec3.z, 
                      net.minecraft.util.Mth.wrapDegrees(level.random.nextFloat() * 360.0F), 0.0F);
            mob.yHeadRot = mob.getYRot();
            mob.yBodyRot = mob.getYRot();
            mob.finalizeSpawn(
                serverLevel,
                serverLevel.getCurrentDifficultyAt(blockpos),
                MobSpawnType.SPAWN_EGG,
                null,
                null
            );
            serverLevel.addFreshEntity(mob);
            serverLevel.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
            itemstack.shrink(1);
        }
        
        return net.minecraft.world.InteractionResult.CONSUME;
    }
}

