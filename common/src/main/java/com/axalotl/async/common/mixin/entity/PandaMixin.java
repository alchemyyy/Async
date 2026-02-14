package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
#if MC_VER_1_21_4 || MC_VER_1_21_8 || MC_VER_1_21_11
import net.minecraft.server.level.ServerLevel;
#endif
#if MC_VER_1_21_11
import net.minecraft.world.entity.animal.panda.Panda;
#else
import net.minecraft.world.entity.animal.Panda;
#endif
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Panda.class)
public class PandaMixin {

    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "pickUpItem")
#if MC_VER_1_21_1
    private void pickUpItem(ItemEntity entity, Operation<Void> original) {
#else
    private void pickUpItem(ServerLevel level, ItemEntity entity, Operation<Void> original) {
#endif
        synchronized (async$lock) {
            if (!entity.isRemoved()) {
#if MC_VER_1_21_1
                original.call(entity);
#else
                original.call(level, entity);
#endif
            }
        }
    }
}
