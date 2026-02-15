package com.axalotl.async.common.mixin.server;

import com.axalotl.async.common.parallelised.fastutil.Long2ObjectConcurrentHashMap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin<
    T extends EntityAccess
> implements AutoCloseable {

    @Shadow
    @Final
    @Mutable
    private Long2ObjectMap<Visibility> chunkVisibility;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void replaceChunkVisibility(CallbackInfo ci) {
        Long2ObjectConcurrentHashMap<Visibility> concurrent =
            new Long2ObjectConcurrentHashMap<>();
        concurrent.defaultReturnValue(Visibility.HIDDEN);
        concurrent.putAll(this.chunkVisibility);
        this.chunkVisibility = concurrent;
    }

    @WrapMethod(method = "getEffectiveStatus")
    private static <T extends EntityAccess> Visibility getEffectiveStatus(
        T entity,
        Visibility visibility,
        Operation<Visibility> original
    ) {
        Visibility result = original.call(entity, visibility);
        if (result == null) {
            return entity.isAlwaysTicking()
                ? Visibility.TICKING
                : Visibility.TRACKED;
        }
        return result;
    }
}
