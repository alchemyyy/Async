package com.axalotl.async.common.platform;

import java.util.Collection;

public interface ModPlatform {
    void saveConfig();

    void reloadConfig();

    boolean isModLoaded(String id);

    /**
     * Returns whether {@code type} was loaded from the files of mod {@code modId}.
     */
    boolean isClassFromMod(Class<?> type, String modId);

    Collection<String> getModIds();

    boolean platformUsesRefmap();
}