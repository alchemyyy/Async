package com.axalotl.async.common.mixin.server;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
import net.minecraft.world.level.ChunkPos;
#endif
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Mixin;
#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
import org.spongepowered.asm.mixin.Unique;
#endif

@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntitySectionManagerMixin implements AutoCloseable {
#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "updateChunkStatus(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/entity/Visibility;)V")
    #if MC_VER_1_21_1
    private void updateStatus(ChunkPos pos, Visibility p_visibility, Operation<Void> original) {
    #elif MC_VER_1_21_4 || MC_VER_1_21_8
    private void updateChunkStatus(ChunkPos pos, Visibility p_visibility, Operation<Void> original) {
    #endif
        synchronized (async$lock) {
            original.call(pos, p_visibility);
        }
    }
#endif

    @WrapMethod(method = "getEffectiveStatus")
    private static <T extends EntityAccess> Visibility getEffectiveStatus(T entity, Visibility visibility, Operation<Visibility> original) {
        Visibility result = original.call(entity, visibility);
#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
        return result != null ? result : Visibility.HIDDEN;
#elif MC_VER_1_21_11
        if (result == null) {
            return entity.isAlwaysTicking() ? Visibility.TICKING : Visibility.TRACKED;
        }
        return result;
#endif
    }
}
