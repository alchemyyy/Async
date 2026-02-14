#if !MC_VER_1_21_11
package com.axalotl.async.common.parallelised.utils;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.ChunkAccess;

public class ChunkLoadTricks {

    public static ChunkAccess tryRetrieveCurrentlyLoading(ChunkHolder holder) {
        return null; //Overwritten by ChunkLoadTricksMixin, NeoForge only
    }
}
#else
package com.axalotl.async.common.parallelised.utils;
// Stub — only exists in pre-1.21.11
public class ChunkLoadTricks {}
#endif
