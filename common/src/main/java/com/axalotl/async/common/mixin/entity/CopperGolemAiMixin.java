#if MC_VER_1_21_11 || MC_VER_1_21_10
package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
#if MC_VER_1_21_11
import net.minecraft.world.entity.animal.golem.CopperGolemAi;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
#elif MC_VER_1_21_10
import net.minecraft.world.entity.animal.coppergolem.CopperGolemAi;
import net.minecraft.world.entity.animal.coppergolem.CopperGolemState;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Predicate;

@Mixin(CopperGolemAi.class)
public class CopperGolemAiMixin {
    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "shouldQueueForTarget")
    private static Predicate<TransportItemsBetweenContainers.TransportItemTarget> shouldQueueForTarget(Operation<Predicate<TransportItemsBetweenContainers.TransportItemTarget>> original) {
        synchronized (async$lock) {
            return original.call();
        }
    }

    @WrapMethod(method = "onReachedTargetInteraction")
    private static TransportItemsBetweenContainers.OnTargetReachedInteraction onReachedTargetInteraction(CopperGolemState p_479346_, SoundEvent p_480078_, Operation<TransportItemsBetweenContainers.OnTargetReachedInteraction> original) {
        synchronized (async$lock) {
            return original.call(p_479346_, p_480078_);
        }
    }
}
#else
package com.axalotl.async.common.mixin.entity;
// Stub -- only exists in 1.21.10+
public class CopperGolemAiMixin {}
#endif
