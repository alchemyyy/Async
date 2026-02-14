package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
#if MC_VER_1_21_11
import net.minecraft.world.entity.animal.bee.Bee;
#else
import net.minecraft.world.entity.animal.Bee;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Bee.class)
public class BeeMixin {
    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "wantsToEnterHive")
    private boolean loot(Operation<Boolean> original) {
        synchronized (async$lock) {
            return original.call();
        }
    }
}
