package com.axalotl.async.common.mixin.server;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
#if MC_VER_1_21_1 || MC_VER_1_21_4
import net.minecraft.core.BlockPos;
#endif
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
#if MC_VER_1_21_1 || MC_VER_1_21_4
    public ServerPlayerMixin(Level world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }
#elif MC_VER_1_21_8 || MC_VER_1_21_11
    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }
#endif

    @WrapMethod(method = "die")
    private synchronized void die(DamageSource damageSource, Operation<Void> original) {
        original.call(damageSource);
    }
}
