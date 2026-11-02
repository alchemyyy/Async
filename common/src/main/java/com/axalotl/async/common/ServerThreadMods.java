package com.axalotl.async.common;

import com.axalotl.async.common.platform.PlatformUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.BlockableEventLoop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

/**
 * Lets mods that only run their server logic on the server thread keep working for entities ticked by
 * Async.
 *
 * <p>Many mods guard their server logic with {@code MinecraftServer#isSameThread()} and silently skip it
 * anywhere else, so their protections and hooks stop applying to entities ticked on worker threads. For
 * every loaded mod listed in {@code serverThreadMods}:
 * <ul>
 *     <li>{@code isSameThread()} called from that mod's code on a worker thread returns {@code true};</li>
 *     <li>that mod's server-thread code runs on one thread at a time. Its first thread check in a unit of
 *     work (an entity tick, a pumped chunk task, or a pool task) takes a per-mod lock held until the unit
 *     ends, which also keeps the mod's paired before/after hooks atomic when they share static state. While
 *     workers tick entities, the server thread takes the same lock for its own checks.</li>
 * </ul>
 */
public final class ServerThreadMods {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerThreadMods.class);
    private static final int NO_SCOPE = -1;
    private static final long WAIT_POLL_MICROSECONDS = 200L;
    private static final long CYCLE_CHECK_INTERVAL_NANOSECONDS = TimeUnit.MILLISECONDS.toNanos(1L);
    private static final long MAXIMUM_WAIT_NANOSECONDS = TimeUnit.SECONDS.toNanos(5L);
    private static final int MAXIMUM_WAIT_CHAIN_LENGTH = 16;
    private static final int MAXIMUM_NESTED_WAITS = 4;
    private static final StackWalker STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static final Map<String, ModLock> LOCKS = new ConcurrentHashMap<>();
    private static final Set<String> REPORTED_FALLBACKS = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Scope> SCOPES = ThreadLocal.withInitial(Scope::new);
    private static final ClassValue<ClassOwner> CLASS_OWNERS = new ClassValue<>() {
        @Override
        protected ClassOwner computeValue(Class<?> type) {
            return new ClassOwner(generation, findOwner(type));
        }
    };

    private static volatile Set<String> activeMods = Set.of();
    private static volatile int generation;
    private static volatile boolean parallelPhase;

    private ServerThreadMods() {
    }

    /**
     * Applies the configured mod ids. Mods that are not loaded are ignored.
     */
    public static synchronized void configure(Collection<String> modIds) {
        Set<String> loadedMods = new LinkedHashSet<>();
        for (String modId : modIds) {
            if (PlatformUtils.isModLoaded(modId)) {
                loadedMods.add(modId);
            }
        }
        if (loadedMods.equals(activeMods)) {
            return;
        }

        generation++;
        activeMods = Set.copyOf(loadedMods);
        if (!loadedMods.isEmpty()) {
            LOGGER.info("Worker threads count as the server thread for: {}", String.join(", ", loadedMods));
        }
    }

    /**
     * Applies {@code serverThreadMods} to a {@code BlockableEventLoop#isSameThread()} result.
     */
    public static boolean isSameThread(BlockableEventLoop<?> eventLoop, boolean sameThread) {
        if (activeMods.isEmpty() || !(eventLoop instanceof MinecraftServer)) {
            return sameThread;
        }
        if (sameThread) {
            if (parallelPhase) {
                serializeCaller();
            }
            return true;
        }
        return ParallelProcessor.isServerExecutionThread() && serializeCaller();
    }

    /**
     * Marks the window in which workers tick entities; the server thread serializes listed mods only then.
     */
    public static void beginParallelPhase() {
        parallelPhase = true;
    }

    /**
     * Ends the window opened by {@link #beginParallelPhase()}.
     */
    public static void endParallelPhase() {
        parallelPhase = false;
    }

    /**
     * Opens a unit of work. Mod locks taken inside it are released by {@link #exit(int)}.
     */
    public static int enter() {
        if (activeMods.isEmpty()) {
            return NO_SCOPE;
        }
        Scope scope = SCOPES.get();
        scope.open++;
        return scope.held.size();
    }

    /**
     * Closes a unit of work opened by {@link #enter()} and releases the mod locks taken inside it.
     */
    public static void exit(int mark) {
        if (mark == NO_SCOPE) {
            return;
        }
        Scope scope = SCOPES.get();
        scope.open--;
        ArrayList<ModLock> held = scope.held;
        for (int index = held.size() - 1; index >= mark; index--) {
            held.remove(index).unlock();
        }
    }

    private static boolean serializeCaller() {
        String modId = STACK_WALKER.walk(ServerThreadMods::callerMod);
        if (modId == null) {
            return false;
        }
        lock(modId);
        return true;
    }

    private static String callerMod(Stream<StackWalker.StackFrame> frames) {
        return frames
                .filter(frame -> frame.getDeclaringClass() != ServerThreadMods.class
                        && frame.getDeclaringClass() != BlockableEventLoop.class)
                .findFirst()
                .map(ServerThreadMods::ownerOf)
                .orElse(null);
    }

    private static String ownerOf(StackWalker.StackFrame frame) {
        Class<?> type = frame.getDeclaringClass();
        ClassOwner owner = CLASS_OWNERS.get(type);
        if (owner.generation != generation) {
            CLASS_OWNERS.remove(type);
            owner = CLASS_OWNERS.get(type);
        }
        return owner.modId != null ? owner.modId : mixinHandlerOwner(frame.getMethodName());
    }

    private static String findOwner(Class<?> type) {
        for (String modId : activeMods) {
            if (PlatformUtils.isClassFromMod(type, modId)) {
                return modId;
            }
        }
        return null;
    }

    /**
     * Mixin merges injector handlers into the target class under names like
     * {@code handler$zza000$modid$method}, so checks written directly in a mod's mixin are attributed by name.
     */
    private static String mixinHandlerOwner(String methodName) {
        int first = methodName.indexOf('$');
        if (first < 0) {
            return null;
        }
        int second = methodName.indexOf('$', first + 1);
        if (second < 0) {
            return null;
        }
        int third = methodName.indexOf('$', second + 1);
        if (third < 0) {
            return null;
        }
        String modId = methodName.substring(second + 1, third);
        return activeMods.contains(modId) ? modId : null;
    }

    private static void lock(String modId) {
        Scope scope = SCOPES.get();
        if (scope.open == 0) {
            // No unit of work would release the lock.
            return;
        }
        ModLock lock = LOCKS.computeIfAbsent(modId, ModLock::new);
        if (lock.isHeldByCurrentThread()) {
            return;
        }
        if (lock.tryLock() || waitFor(lock, scope)) {
            scope.held.add(lock);
        }
    }

    /**
     * Waits for a mod lock. The server thread keeps servicing chunk tasks meanwhile, since the holder may be
     * a worker waiting for a chunk; a serviced task may wait again, up to {@link #MAXIMUM_NESTED_WAITS} deep.
     * Gives up, leaving the caller unserialized, when the holder waits on a lock this thread holds or after
     * {@link #MAXIMUM_WAIT_NANOSECONDS}.
     */
    private static boolean waitFor(ModLock lock, Scope scope) {
        if (scope.waits >= MAXIMUM_NESTED_WAITS) {
            reportFallback(lock, "this thread already waits for it in " + scope.waits + " enclosing chunk tasks");
            return false;
        }
        MinecraftServer server = ParallelProcessor.getServer();
        boolean serverThread = server != null && Thread.currentThread() == server.getRunningThread();
        long start = System.nanoTime();
        long nextCycleCheck = start;
        boolean interrupted = false;
        scope.waits++;
        try {
            while (true) {
                try {
                    if (lock.tryLock(WAIT_POLL_MICROSECONDS, TimeUnit.MICROSECONDS)) {
                        return true;
                    }
                } catch (InterruptedException exception) {
                    interrupted = true;
                }
                if (serverThread) {
                    ParallelProcessor.pumpChunkTasks();
                }

                long now = System.nanoTime();
                Thread holder = lock.holder();
                if (now >= nextCycleCheck) {
                    if (holder != null && waitsOnCurrentThread(holder)) {
                        reportFallback(lock, "its holder " + holder.getName() + " waits on this thread");
                        return false;
                    }
                    nextCycleCheck = now + CYCLE_CHECK_INTERVAL_NANOSECONDS;
                }
                if (now - start >= MAXIMUM_WAIT_NANOSECONDS) {
                    reportFallback(lock, "it was held for over 5 seconds by "
                            + (holder != null ? holder.getName() : "another thread"));
                    return false;
                }
            }
        } finally {
            scope.waits--;
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static boolean waitsOnCurrentThread(Thread holder) {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long currentThreadId = Thread.currentThread().threadId();
        long threadId = holder.threadId();
        for (int step = 0; step < MAXIMUM_WAIT_CHAIN_LENGTH && threadId != -1L; step++) {
            if (threadId == currentThreadId) {
                return true;
            }
            ThreadInfo threadInfo = threads.getThreadInfo(threadId);
            if (threadInfo == null) {
                return false;
            }
            threadId = threadInfo.getLockOwnerId();
        }
        return false;
    }

    private static void reportFallback(ModLock lock, String reason) {
        if (REPORTED_FALLBACKS.add(lock.modId)) {
            LOGGER.warn(
                    "Running {} code on {} without serialization because {}. Further occurrences are logged at debug level.",
                    lock.modId,
                    Thread.currentThread().getName(),
                    reason
            );
        } else {
            LOGGER.debug("Running {} code on {} without serialization because {}",
                    lock.modId, Thread.currentThread().getName(), reason);
        }
    }

    private record ClassOwner(int generation, String modId) {
    }

    private static final class Scope {
        private final ArrayList<ModLock> held = new ArrayList<>(2);
        private int open;
        private int waits;
    }

    private static final class ModLock extends ReentrantLock {
        private final String modId;

        private ModLock(String modId) {
            this.modId = modId;
        }

        private Thread holder() {
            return getOwner();
        }
    }
}
