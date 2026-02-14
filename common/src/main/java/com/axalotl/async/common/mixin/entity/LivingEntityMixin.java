package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
#endif
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
#if MC_VER_1_21_4 || MC_VER_1_21_8 || MC_VER_1_21_11
import net.minecraft.server.level.ServerLevel;
#endif
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
#if MC_VER_1_21_1
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#endif
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

#if MC_VER_1_21_11
import java.util.ArrayList;
import java.util.List;
#elif MC_VER_1_21_8
import java.util.Collections;
import java.util.List;
#endif
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = LivingEntity.class, priority = 1001)
public abstract class LivingEntityMixin extends Entity {

    @Shadow
    final private Map<Holder<MobEffect>, MobEffectInstance> activeEffects = new ConcurrentHashMap<>();

#if MC_VER_1_21_11
    @Shadow
    protected abstract void onEffectUpdated(MobEffectInstance effect, boolean reapply, Entity source);

    @Shadow
    protected abstract void onEffectsRemoved(java.util.Collection<MobEffectInstance> effects);
#endif

    @Unique
    private static final Object async$lock = new Object();

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @WrapMethod(method = "die")
    private synchronized void die(DamageSource damageSource, Operation<Void> original) {
        original.call(damageSource);
    }

#if MC_VER_1_21_1
    @WrapMethod(method = "dropFromLootTable")
    private synchronized void dropFromLootTable(DamageSource damageSource, boolean causedByPlayer, Operation<Void> original) {
        original.call(damageSource, causedByPlayer);
    }
#else
    @WrapMethod(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V")
    private synchronized void dropFromLootTable(ServerLevel level, DamageSource damageSource, boolean playerKill, Operation<Void> original) {
        original.call(level, damageSource, playerKill);
    }
#endif

#if MC_VER_1_21_1 || MC_VER_1_21_4
    @WrapMethod(method = "blockedByShield")
    private synchronized void knockback(LivingEntity defender, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(defender);
        }
    }
#elif MC_VER_1_21_8
    @WrapMethod(method = "knockback")
    private synchronized void knockback(double strength, double x, double z, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(strength, x, z);
        }
    }
#else
    @WrapMethod(method = "knockback")
    private void knockback(double strength, double x, double z, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(strength, x, z);
        }
    }
#endif

#if MC_VER_1_21_11
    @WrapMethod(method = "tickEffects")
    private void tickStatusEffects(Operation<Void> original) {
        synchronized (async$lock) {
            if (this.level() instanceof ServerLevel serverlevel) {
                List<Holder<MobEffect>> effectsToTick = new ArrayList<>(this.activeEffects.keySet());

                for (Holder<MobEffect> holder : effectsToTick) {
                    MobEffectInstance mobeffectinstance = this.activeEffects.get(holder);

                    if (mobeffectinstance != null) {
                        if (!mobeffectinstance.tickServer(serverlevel, (LivingEntity)(Object)this,
                                () -> this.onEffectUpdated(mobeffectinstance, true, null))) {
                            this.activeEffects.remove(holder);
                            this.onEffectsRemoved(List.of(mobeffectinstance));
                        } else if (mobeffectinstance.getDuration() % 600 == 0) {
                            this.onEffectUpdated(mobeffectinstance, false, null);
                        }
                    }
                }
            } else {
                original.call();
            }
        }
    }
#else
    @WrapMethod(method = "tickEffects")
    private void tickStatusEffects(Operation<Void> original) {
        synchronized (async$lock) {
            original.call();
        }
    }

#if MC_VER_1_21_1 || MC_VER_1_21_4
    @WrapOperation(method = "tickEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;tick(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/Runnable;)Z"))
    private boolean tickEffects(MobEffectInstance instance, LivingEntity entity, Runnable runnable, Operation<Boolean> original) {
        return instance != null ? original.call(instance, entity, runnable) : false;
    }
#elif MC_VER_1_21_8
    @WrapOperation(
            method = "tickEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffectInstance;tickServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/Runnable;)Z"
            )
    )
    private boolean wrapTickEffect(MobEffectInstance instance, ServerLevel level, LivingEntity entity, Runnable onEffectUpdated, Operation<Boolean> original) {
        return instance != null ? original.call(instance, level, entity, onEffectUpdated) : false;
    }

    @WrapOperation(
            method = "tickEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;of(Ljava/lang/Object;)Ljava/util/List;"
            )
    )
    private List<?> wrapListOf(Object element, Operation<List<?>> original) {
        return element != null ? original.call(element) : Collections.emptyList();
    }
#endif
#endif

#if MC_VER_1_21_1
    @Inject(method = "onEffectRemoved", at = @At("HEAD"), cancellable = true)
    private void onEffectRemoved(MobEffectInstance effectInstance, CallbackInfo ci) {
        if (effectInstance == null) {
            ci.cancel();
        }
    }
#endif

    @WrapMethod(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean addEffect(MobEffectInstance effect, Entity source, Operation<Boolean> original) {
        synchronized (async$lock) {
            return effect != null ? original.call(effect, source) : false;
        }
    }

    @WrapMethod(method = "removeEffect")
    private boolean removeEffect(Holder<MobEffect> effect, Operation<Boolean> original) {
        synchronized (async$lock) {
            return effect != null ? original.call(effect) : false;
        }
    }

    @WrapMethod(method = "hasEffect")
    public boolean hasEffect(Holder<MobEffect> effect, Operation<Boolean> original) {
        return effect != null ? original.call(effect) : false;
    }

    @WrapMethod(method = "removeAllEffects")
    private boolean removeAllEffects(Operation<Boolean> original) {
        synchronized (async$lock) {
            return original.call();
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
#if MC_VER_1_21_1 || MC_VER_1_21_4
    private void causeFallDamage(float fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
#else
    private void causeFallDamage(double fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
#endif
        BlockPos pos = new BlockPos(Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()));
        BlockState currentBlock = this.level().getBlockState(pos);

        if (currentBlock.is(BlockTags.CLIMBABLE)) {
            cir.setReturnValue(false);
        }
    }
}
