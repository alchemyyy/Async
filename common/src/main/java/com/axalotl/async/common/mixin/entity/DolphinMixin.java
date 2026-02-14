package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
#if MC_VER_1_21_1
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.Dolphin;
#elif MC_VER_1_21_11
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
#else
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.Dolphin;
#endif
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Dolphin.class)
#if MC_VER_1_21_1
public abstract class DolphinMixin extends WaterAnimal {
#else
public abstract class DolphinMixin extends AgeableWaterCreature {
#endif

    @Unique
    private static final Object async$lock = new Object();

#if MC_VER_1_21_1
    protected DolphinMixin(EntityType<? extends WaterAnimal> entityType, Level world) {
        super(entityType, world);
    }
#else
    protected DolphinMixin(EntityType<? extends AgeableWaterCreature> entityType, Level level) {
        super(entityType, level);
    }
#endif

    @WrapMethod(method = "pickUpItem")
#if MC_VER_1_21_1
    private void pickUpItem(ItemEntity itemEntity, Operation<Void> original) {
        synchronized (async$lock) {
            if (!itemEntity.isRemoved()) {
                original.call(itemEntity);
            }
        }
    }
#else
    private void pickUpItem(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (async$lock) {
            if (!entity.isRemoved()) {
                original.call(level, entity);
            }
        }
    }
#endif
}
