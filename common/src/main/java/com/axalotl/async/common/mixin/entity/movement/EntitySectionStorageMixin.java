package com.axalotl.async.common.mixin.entity.movement;

import com.axalotl.async.common.parallelised.fastutil.ConcurrentLongSortedSet;
import com.axalotl.async.common.parallelised.fastutil.Long2ObjectConcurrentHashMap;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.*;

import java.util.Objects;
import java.util.stream.LongStream;
import java.util.stream.Stream;
#if MC_VER_1_21_11 || MC_VER_1_21_10
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#endif

@Mixin(value = EntitySectionStorage.class)
public abstract class EntitySectionStorageMixin<T extends EntityAccess> {

    @Shadow
    #if MC_VER_1_21_11 || MC_VER_1_21_10
    @Final
    @Mutable
    #endif
    private Long2ObjectMap<EntitySection<T>> sections
    #if !MC_VER_1_21_11 && !MC_VER_1_21_10
    = new Long2ObjectConcurrentHashMap<>()
    #endif
    ;

    @Shadow
    #if MC_VER_1_21_11 || MC_VER_1_21_10
    @Final
    @Mutable
    #endif
    private LongSortedSet sectionIds
    #if !MC_VER_1_21_11 && !MC_VER_1_21_10
    = new ConcurrentLongSortedSet()
    #endif
    ;

    @Shadow
    public abstract LongStream getExistingSectionPositionsInChunk(long pos);

    #if MC_VER_1_21_11 || MC_VER_1_21_10
    @Unique
    private final Object async$Lock = new Object();

    @WrapMethod(method = "getOrCreateSection")
    private EntitySection<T> getOrCreateSection(long pos, Operation<EntitySection<T>> original) {
        EntitySection<T> existing = this.sections.get(pos);
        if (existing != null) return existing;
        synchronized (async$Lock) {
            return original.call(pos);
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void replaceCollections(CallbackInfo ci) {
        this.sections = new Long2ObjectConcurrentHashMap<>();
        this.sectionIds = new ConcurrentLongSortedSet();
    }
    #endif

    @WrapMethod(method = "getExistingSectionsInChunk")
    private Stream<EntitySection<T>> getExistingSections(long pos, Operation<Stream<EntitySection<T>>> original) {
        return this.getExistingSectionPositionsInChunk(pos)
                .mapToObj(this.sections::get)
                .filter(Objects::nonNull)
                .toList()
                .stream();
    }
}
