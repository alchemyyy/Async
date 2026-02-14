package com.axalotl.async.neoforge.platform;

import com.axalotl.async.common.platform.ModPlatform;
import com.axalotl.async.neoforge.config.AsyncConfig;
import net.neoforged.fml.loading.FMLLoader;

public class NeoForgeModPlatform implements ModPlatform {

    @Override
    public void saveConfig() {
        AsyncConfig.saveConfig();
    }

    @Override
    public void reloadConfig() {
        AsyncConfig.loadConfig();
        com.axalotl.async.common.config.AsyncConfig.onConfigLoaded();
    }

    @Override
    public boolean isModLoaded(String id) {
        #if MC_VER_1_21_1 || MC_VER_1_21_4
        return FMLLoader.getLoadingModList().getModFileById(id) != null;
        #elif MC_VER_1_21_8
        try {
            Object modList;
            try {
                // 1.21.10+: FMLLoader.getCurrent().getLoadingModList()
                var current = FMLLoader.class.getMethod("getCurrent").invoke(null);
                modList = current.getClass().getMethod("getLoadingModList").invoke(current);
            } catch (NoSuchMethodException e) {
                // 1.21.8: FMLLoader.getLoadingModList() (static)
                modList = FMLLoader.class.getMethod("getLoadingModList").invoke(null);
            }
            return modList.getClass().getMethod("getModFileById", String.class).invoke(modList, id) != null;
        } catch (Exception e) {
            return false;
        }
        #elif MC_VER_1_21_11
        return FMLLoader.getCurrent().getLoadingModList().getModFileById(id) != null;
        #endif
    }

    @Override
    public boolean platformUsesRefmap() {
        return false;
    }
}
