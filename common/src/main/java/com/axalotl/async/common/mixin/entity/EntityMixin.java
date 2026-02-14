package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
#if MC_VER_1_21_11
import net.minecraft.core.BlockPos;
#endif
import net.minecraft.world.entity.Entity;
#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_11
import net.minecraft.world.level.Level;
#endif
#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
import net.minecraft.world.level.block.Blocks;
#endif
import net.minecraft.world.level.block.state.BlockState;
#if MC_VER_1_21_11
import org.jetbrains.annotations.Nullable;
#endif
import org.spongepowered.asm.mixin.Mixin;
#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_11
import org.spongepowered.asm.mixin.Shadow;
#endif
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin {

#if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_11
    @Shadow
    public abstract Level level();
#endif
#if MC_VER_1_21_11
    @Shadow private @Nullable BlockState inBlockState;
    @Shadow public abstract BlockPos blockPosition();
#endif

    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "setRemoved")
    private void setRemoved(Entity.RemovalReason reason, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(reason);
        }
    }

    @WrapMethod(method = "getInBlockState")
    private BlockState wrapGetInBlockState(Operation<BlockState> original) {
#if MC_VER_1_21_11
        BlockState state = this.inBlockState;
        if (state != null) {
            return state;
        }
        state = this.level().getBlockState(this.blockPosition());
        this.inBlockState = state;
        return state;
#else
        BlockState blockState = original.call();
        return blockState != null ? blockState : Blocks.AIR.defaultBlockState();
#endif
    }

    @WrapMethod(method = "addPassenger")
    private void addPassenger(Entity passenger, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(passenger);
        }
    }

    @WrapMethod(method = "removePassenger")
    private void removePassenger(Entity passenger, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(passenger);
        }
    }

    @WrapMethod(method = "ejectPassengers")
    private void ejectPassengers(Operation<Void> original) {
        synchronized (async$lock) {
            original.call();
        }
    }
}
