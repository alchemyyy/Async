package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
#if MC_VER_1_21_11
import net.minecraft.world.entity.npc.villager.Villager;
#else
import net.minecraft.world.entity.npc.Villager;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Villager.class)
public class VillagerMixin {

    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "pickUpItem")
#if MC_VER_1_21_1
    private void pickUpItem(ItemEntity itemEntity, Operation<Void> original) {
        synchronized (async$lock) {
            if (!itemEntity.isRemoved()) {
                original.call(itemEntity);
            }
        }
    }
#elif MC_VER_1_21_4 || MC_VER_1_21_8 || MC_VER_1_21_11
    private void pickUpItem(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (async$lock) {
            if (!entity.isRemoved()) {
                original.call(level, entity);
            }
        }
    }
#endif

    @WrapMethod(method = "spawnGolemIfNeeded")
    private void spawnGolemIfNeeded(ServerLevel world, long time, int requiredCount, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(world, time, requiredCount);
        }
    }
}
