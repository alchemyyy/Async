package com.axalotl.async.fabric.platform;

import com.axalotl.async.common.platform.ModPlatform;
import com.axalotl.async.fabric.config.AsyncConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Collection;

public class FabricModPlatform implements ModPlatform {

    @Override
    public void saveConfig() {
        AsyncConfig.saveConfig();
    }

    @Override
    public void reloadConfig() {
        AsyncConfig.init();
    }

    @Override
    public boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    @Override
    public boolean isClassFromMod(Class<?> type, String modId) {
        String classFile = type.getName().replace('.', '/') + ".class";
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .flatMap(container -> container.findPath(classFile))
                .isPresent();
    }

    @Override
    public Collection<String> getModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(container -> container.getMetadata().getId())
                .toList();
    }

    @Override
    public boolean platformUsesRefmap() {
        return true;
    }
}