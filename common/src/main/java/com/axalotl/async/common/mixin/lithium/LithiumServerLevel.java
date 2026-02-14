package com.axalotl.async.common.mixin.lithium;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.lithium.common.entity.NavigatingEntity;
import net.caffeinemc.mods.lithium.common.world.ServerWorldExtended;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
#if MC_VER_1_21_1
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.function.Supplier;
#endif
#if MC_VER_1_21_11 || MC_VER_1_21_10
import com.axalotl.async.common.parallelised.utils.AsyncSafeNavigation;
#endif

@Mixin(value = ServerLevel.class, priority = 1500)
public abstract class LithiumServerLevel extends Level implements WorldGenLevel, ServerWorldExtended {
    @Unique
    private final Set<PathNavigation> async$activeNavigationsOver = Collections.newSetFromMap(new ConcurrentHashMap<>());

#if MC_VER_1_21_1
    protected LithiumServerLevel(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }
#else
    protected LithiumServerLevel(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }
#endif

    @Inject(
            method = "sendBlockUpdated",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"
            )
    )
    private void updateActiveListeners(BlockPos pos, BlockState oldState, BlockState newState, int arg3, CallbackInfo ci, @Local List<PathNavigation> list) {
        for (PathNavigation nav : async$activeNavigationsOver) {
#if MC_VER_1_21_11 || MC_VER_1_21_10
            if (((AsyncSafeNavigation) nav).async$shouldRecomputePathSafe(pos)) {
#else
            if (nav.shouldRecomputePath(pos)) {
#endif
                list.add(nav);
            }
        }
    }

    @Override
    public void lithium$setNavigationActive(Mob mobEntity) {
#if MC_VER_1_21_11 || MC_VER_1_21_10
        PathNavigation nav = ((NavigatingEntity) mobEntity).lithium$getRegisteredNavigation();
        if (nav != null) {
            async$activeNavigationsOver.add(nav);
        }
#else
        async$activeNavigationsOver.add(((NavigatingEntity) mobEntity).lithium$getRegisteredNavigation());
#endif
    }

    @Override
    public void lithium$setNavigationInactive(Mob mobEntity) {
#if MC_VER_1_21_11 || MC_VER_1_21_10
        PathNavigation nav = ((NavigatingEntity) mobEntity).lithium$getRegisteredNavigation();
        if (nav != null) {
            async$activeNavigationsOver.remove(nav);
        }
#else
        async$activeNavigationsOver.remove(((NavigatingEntity) mobEntity).lithium$getRegisteredNavigation());
#endif
    }
}
