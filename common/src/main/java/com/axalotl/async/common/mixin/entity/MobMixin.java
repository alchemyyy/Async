package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
#if MC_VER_1_21_4 || MC_VER_1_21_8 || MC_VER_1_21_11
import net.minecraft.server.level.ServerLevel;
#endif
#if MC_VER_1_21_11
import net.minecraft.world.entity.*;
#else
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
#endif
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
#if MC_VER_1_21_11
import org.jspecify.annotations.Nullable;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Mob.class)
#if MC_VER_1_21_11
public abstract class MobMixin {
#else
public class MobMixin {
#endif

    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "equipItemIfPossible")
#if MC_VER_1_21_1
    private ItemStack tryEquip(ItemStack stack, Operation<ItemStack> original) {
        synchronized (async$lock) {
            return original.call(stack);
        }
    }
#elif MC_VER_1_21_4 || MC_VER_1_21_8 || MC_VER_1_21_11
    private ItemStack tryEquip(ServerLevel level, ItemStack stack, Operation<ItemStack> original) {
        synchronized (async$lock) {
            return original.call(level, stack);
        }
    }
#endif

    @WrapMethod(method = "pickUpItem")
#if MC_VER_1_21_1
    private void pickUpItem(ItemEntity itemEntity, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(itemEntity);
        }
    }
#elif MC_VER_1_21_4 || MC_VER_1_21_8 || MC_VER_1_21_11
    private void pickUpItem(ServerLevel level, ItemEntity entity, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(level, entity);
        }
    }
#endif

#if MC_VER_1_21_1
    @WrapMethod(method = "setItemSlot")
    private void equipStack(EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(slot, stack);
        }
    }

#endif
    @WrapMethod(method = "setItemSlotAndDropWhenKilled")
    private void equipLootStack(EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(slot, stack);
        }
    }

    @WrapMethod(method = "setBodyArmorItem")
#if MC_VER_1_21_11
    private void setBodyArmor(ItemStack stack, Operation<Void> original) {
#else
    private void equipLootStack(ItemStack stack, Operation<Void> original) {
#endif
        synchronized (async$lock) {
            original.call(stack);
        }
    }

#if MC_VER_1_21_11
    @WrapMethod(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;")
    private <T extends Mob> @Nullable T convertTo(
            EntityType<T> entityType,
            ConversionParams conversionParams,
            EntitySpawnReason spawnReason,
            ConversionParams.AfterConversion<T> afterConversion,
            Operation<T> original
    ) {
        synchronized (async$lock) {
            if (((Mob)(Object)this).isRemoved()) {
                return null;
            }
            return original.call(entityType, conversionParams, spawnReason, afterConversion);
        }
    }
#endif
}
