package com.axalotl.async.neoforge.platform;

import com.axalotl.async.common.platform.MinecraftPlatform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
#if MC_VER_1_21_11
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
#endif
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

public class NeoForgeMinecraftPlatform implements MinecraftPlatform {
    @Override
    public boolean hasPermission(CommandSourceStack source, String node, int level) {
        #if MC_VER_1_21_1 || MC_VER_1_21_4 || MC_VER_1_21_8
        if (source.hasPermission(level)) {
        #elif MC_VER_1_21_11
        if (source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)))) {
        #endif
            return true;
        }

        ServerPlayer player = source.getPlayer();
        PermissionNode<Boolean> permission = NeoForgePermissions.getPermissionNode(node);
        if (player == null || permission == null) {
            return false;
        }

        return PermissionAPI.getPermission(player, permission);
    }
}
