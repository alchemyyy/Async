package com.axalotl.async.common.mixin.lithium;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Исправляет thread-safety проблему в Lithium's LevelChunkMixin.
 * Lithium заменяет HashMap на Object2ObjectOpenHashMap для tickersInLevel,
 * но Object2ObjectOpenHashMap не является thread-safe, что приводит к крашу
 * при параллельной обработке чанков в Async.
 */
@Mixin(value = LevelChunk.class, priority = 1100)
public class LithiumLevelChunkTickersMixin {

    @Mutable
    @Shadow
    @Final
    private Map<BlockPos, ?> tickersInLevel;

    /**
     * Перехватываем инициализацию ПОСЛЕ того как Lithium создаст Object2ObjectOpenHashMap,
     * и заменяем его на thread-safe ConcurrentHashMap.
     *
     * Priority 1100 гарантирует что этот миксин выполнится ПОСЛЕ Lithium миксина (priority 1000).
     */
    @Inject(
            method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/UpgradeData;Lnet/minecraft/world/ticks/LevelChunkTicks;Lnet/minecraft/world/ticks/LevelChunkTicks;J[Lnet/minecraft/world/level/chunk/LevelChunkSection;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;Lnet/minecraft/world/level/levelgen/blending/BlendingData;)V",
            at = @At("RETURN"),
            remap = false
    )
    private void replaceFastUtilMapWithConcurrentMap(CallbackInfo ci) {
        // Проверяем, создал ли Lithium Object2ObjectOpenHashMap
        if (this.tickersInLevel instanceof Object2ObjectOpenHashMap) {
            // Заменяем на thread-safe ConcurrentHashMap
            // Копируем существующие данные если они есть
            Map<BlockPos, ?> oldMap = this.tickersInLevel;
            this.tickersInLevel = new ConcurrentHashMap<>(oldMap);
        }
    }
}