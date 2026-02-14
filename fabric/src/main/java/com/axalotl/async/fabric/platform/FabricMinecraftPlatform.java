package com.axalotl.async.fabric.platform;

import com.axalotl.async.common.AsyncCommon;
import com.axalotl.async.common.platform.MinecraftPlatform;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
#if MC_VER_1_21_11
import net.minecraft.server.permissions.PermissionLevel;
#endif

public class FabricMinecraftPlatform implements MinecraftPlatform {

    @Override
    public boolean hasPermission(CommandSourceStack source, String node, int level) {
        String permission = String.format("%s.%s", AsyncCommon.MODID, node);
        #if MC_VER_1_21_11
        return Permissions.check(source, permission, PermissionLevel.byId(level));
        #else
        return Permissions.check(source, permission, level);
        #endif
    }
}
