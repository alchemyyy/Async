package com.axalotl.async.neoforge.platform;

import com.axalotl.async.common.platform.ModPlatform;
import com.axalotl.async.neoforge.config.AsyncConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class NeoForgeModPlatform implements ModPlatform {
    private final Map<String, Optional<String>> moduleNames = new ConcurrentHashMap<>();

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
        return FMLLoader.getLoadingModList().getModFileById(id) != null;
    }

    @Override
    public boolean isClassFromMod(Class<?> type, String modId) {
        String moduleName = type.getModule().getName();
        return moduleName != null && moduleNames
                .computeIfAbsent(modId, NeoForgeModPlatform::findModuleName)
                .map(moduleName::equals)
                .orElse(false);
    }

    private static Optional<String> findModuleName(String modId) {
        return Optional.ofNullable(FMLLoader.getLoadingModList().getModFileById(modId))
                .map(IModFileInfo::moduleName);
    }

    @Override
    public Collection<String> getModIds() {
        return FMLLoader.getLoadingModList().getMods().stream().map(IModInfo::getModId).toList();
    }

    @Override
    public boolean platformUsesRefmap() {
        return false;
    }
}
