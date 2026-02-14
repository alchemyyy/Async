package com.axalotl.async.common.mixin.entity;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
#if !MC_VER_1_21_1
import net.minecraft.server.level.ServerLevel;
#endif
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

@Mixin(NearestVisibleLivingEntities.class)
public class NearestVisibleLivingEntitiesMixin {

    @Mutable
    @Shadow
    @Final
    private Predicate<LivingEntity> lineOfSightTest;

#if MC_VER_1_21_1
    @Inject(method = "<init>(Lnet/minecraft/world/entity/LivingEntity;Ljava/util/List;)V", at = @At("RETURN"))
    private void init(LivingEntity owner, List<LivingEntity> entities, CallbackInfo ci) {
        Object2BooleanOpenHashMap<LivingEntity> object2BooleanOpenHashMap = new Object2BooleanOpenHashMap<>(entities.size());
        Predicate<LivingEntity> predicate = target -> Sensor.isEntityTargetable(owner, target);
#else
    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/List;)V", at = @At("RETURN"))
    private void init(ServerLevel level, LivingEntity owner, List<LivingEntity> nearbyEntities, CallbackInfo ci) {
        Object2BooleanOpenHashMap<LivingEntity> object2BooleanOpenHashMap = new Object2BooleanOpenHashMap<>(nearbyEntities.size());
        Predicate<LivingEntity> predicate = target -> Sensor.isEntityTargetable(level, owner, target);
#endif
        this.lineOfSightTest = entity -> {
            synchronized (object2BooleanOpenHashMap) {
                return object2BooleanOpenHashMap.computeIfAbsent(entity, predicate);
            }
        };
    }
}
