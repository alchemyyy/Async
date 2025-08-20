package com.axalotl.async.common.mixin.entity;

import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(AreaEffectCloud.class)
public class AreaEffectCloudMixin {

    // Только то поле, которое мы действительно используем и модифицируем
    @Shadow @Mutable @Final
    private Map<Entity, Integer> victims;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void makeCollectionsThreadSafe(EntityType<?> entityType, Level level, CallbackInfo ci) {
        // Единственное необходимое изменение - делаем мапу потокобезопасной
        this.victims = new ConcurrentHashMap<>(this.victims);
    }
}