package com.axalotl.async.common.mixin.entity.sensor;

#if MC_VER_1_21_11
import com.axalotl.async.common.config.AsyncConfig;
#endif
import net.minecraft.server.level.ServerLevel;
#if !MC_VER_1_21_11
import net.minecraft.world.entity.Entity;
#endif
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor;
#if !MC_VER_1_21_11
import net.minecraft.world.phys.Vec3;
#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Comparator;
#if !MC_VER_1_21_11
import java.util.HashMap;
#else
import java.util.IdentityHashMap;
#endif
import java.util.Map;
import java.util.function.ToDoubleFunction;

@Mixin(value = NearestLivingEntitySensor.class, priority = 1500)
#if MC_VER_1_21_11
public class NearestLivingEntitiesSensorMixin<T extends LivingEntity> {

    @Redirect(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Comparator;comparingDouble(Ljava/util/function/ToDoubleFunction;)Ljava/util/Comparator;"))
    private <E extends LivingEntity> Comparator<E> async$safeComparator(ToDoubleFunction<? super E> keyExtractor,
                                                                        ServerLevel world, T entity) {
        if (AsyncConfig.disabled) {
            return Comparator.comparingDouble(keyExtractor);
        }

        final double ex = entity.getX();
        final double ey = entity.getY();
        final double ez = entity.getZ();

        Map<E, double[]> cache = new IdentityHashMap<>();

        return (t1, t2) -> {
            double[] p1 = cache.computeIfAbsent(t1, e -> new double[]{e.getX(), e.getY(), e.getZ()});
            double[] p2 = cache.computeIfAbsent(t2, e -> new double[]{e.getX(), e.getY(), e.getZ()});

            double d1 = (p1[0]-ex)*(p1[0]-ex) + (p1[1]-ey)*(p1[1]-ey) + (p1[2]-ez)*(p1[2]-ez);
            double d2 = (p2[0]-ex)*(p2[0]-ex) + (p2[1]-ey)*(p2[1]-ey) + (p2[2]-ez)*(p2[2]-ez);

            return Double.compare(d1, d2);
        };
    }
}
#else
public class NearestLivingEntitiesSensorMixin {

    @Redirect(method = "doTick",
            at = @At(value = "INVOKE", target = "Ljava/util/Comparator;comparingDouble(Ljava/util/function/ToDoubleFunction;)Ljava/util/Comparator;"))
    private Comparator<LivingEntity> doTick(ToDoubleFunction<? super LivingEntity> keyExtractor, ServerLevel world, LivingEntity entity) {
        Map<LivingEntity, Vec3> positionCache = new HashMap<>();
        return (entity1, entity2) -> {
            Vec3 pos1 = positionCache.computeIfAbsent(entity1, Entity::position);
            Vec3 pos2 = positionCache.computeIfAbsent(entity2, Entity::position);
            double dist1 = entity.distanceToSqr(pos1);
            double dist2 = entity.distanceToSqr(pos2);
            return Double.compare(dist1, dist2);
        };
    }
}
#endif
