package com.axalotl.async.common.parallelised.fastutil;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.LongFunction;

/**
 * A thread-safe implementation of Long2LongMap using ConcurrentHashMap as backing storage.
 * This implementation provides concurrent access and high performance for long-to-long mappings.
 *
 * <p>This class is particularly useful in multithreaded environments where multiple threads
 * need to access and modify the same map concurrently without external synchronization.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Thread-safe operations using ConcurrentHashMap</li>
 *   <li>Support for default return values</li>
 *   <li>Atomic operations (putIfAbsent, replace, compute, etc.)</li>
 *   <li>Compatible with fastutil Long2LongMap interface</li>
 * </ul>
 */
public final class Long2LongConcurrentHashMap implements Long2LongMap {

    private final ConcurrentHashMap<Long, Long> backing;
    private volatile long defaultReturnValue;

    /**
     * Creates a new empty concurrent map with default initial capacity and default return value of 0
     */
    public Long2LongConcurrentHashMap() {
        this(0L);
    }

    /**
     * Creates a new empty concurrent map with specified default return value
     *
     * @param defaultReturnValue the default value to return when a key is not found
     */
    public Long2LongConcurrentHashMap(long defaultReturnValue) {
        this.backing = new ConcurrentHashMap<>();
        this.defaultReturnValue = defaultReturnValue;
    }

    /**
     * Creates a new concurrent map with specified initial capacity and default return value
     *
     * @param initialCapacity the initial capacity
     * @param defaultReturnValue the default value to return when a key is not found
     */
    public Long2LongConcurrentHashMap(int initialCapacity, long defaultReturnValue) {
        this.backing = new ConcurrentHashMap<>(initialCapacity);
        this.defaultReturnValue = defaultReturnValue;
    }

    @Override
    public long get(long key) {
        Long value = backing.get(key);
        return value != null ? value : defaultReturnValue;
    }

    @Override
    public long put(long key, long value) {
        Long previous = backing.put(key, value);
        return previous != null ? previous : defaultReturnValue;
    }

    @Override
    public Long put(Long key, Long value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        return backing.put(key, value);
    }

    @Override
    public long remove(long key) {
        Long previous = backing.remove(key);
        return previous != null ? previous : defaultReturnValue;
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public void clear() {
        backing.clear();
    }

    @Override
    public boolean containsKey(long key) {
        return backing.containsKey(key);
    }

    @Override
    public boolean containsValue(long value) {
        return backing.containsValue(value);
    }

    @Override
    public void defaultReturnValue(long rv) {
        this.defaultReturnValue = rv;
    }

    @Override
    public long defaultReturnValue() {
        return defaultReturnValue;
    }

    @Override
    public ObjectSet<Entry> long2LongEntrySet() {
        return FastUtilHackUtil.entrySetLongLongWrap(backing);
    }

    @Override
    public @NotNull LongSet keySet() {
        return FastUtilHackUtil.wrapLongSet(backing.keySet());
    }

    @Override
    public @NotNull LongCollection values() {
        return FastUtilHackUtil.wrapLongs(backing.values());
    }

    @Override
    public void putAll(@NotNull Map<? extends Long, ? extends Long> m) {
        Objects.requireNonNull(m, "Source map cannot be null");
        backing.putAll(m);
    }

    /**
     * Returns the value to which the specified key is mapped, or defaultValue if
     * this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default mapping of the key
     * @return the value to which the specified key is mapped, or defaultValue
     */
    public long getOrDefault(long key, long defaultValue) {
        Long value = backing.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Associates the specified value with the specified key if no value is present.
     * This operation is atomic.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or defaultReturnValue if there was no mapping
     */
    public long putIfAbsent(long key, long value) {
        Long previous = backing.putIfAbsent(key, value);
        return previous != null ? previous : defaultReturnValue;
    }

    /**
     * Removes the entry for the specified key only if it is currently mapped to the specified value.
     * This operation is atomic.
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return true if the value was removed
     */
    public boolean remove(long key, long value) {
        return backing.remove(key, value);
    }

    /**
     * Replaces the entry for the specified key only if it is currently mapped to the specified value.
     * This operation is atomic.
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return true if the value was replaced
     */
    public boolean replace(long key, long oldValue, long newValue) {
        return backing.replace(key, oldValue, newValue);
    }

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     * This operation is atomic.
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or defaultReturnValue if there was no mapping
     */
    public long replace(long key, long value) {
        Long previous = backing.replace(key, value);
        return previous != null ? previous : defaultReturnValue;
    }

    /**
     * Attempts to compute a mapping for the specified key and its current mapped value
     * (or null if there is no current mapping). This operation is atomic.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or defaultReturnValue if none
     */
    @Override
    public long compute(long key, BiFunction<? super Long, ? super Long, ? extends Long> remappingFunction) {
        Objects.requireNonNull(remappingFunction, "Remapping function cannot be null");
        Long newValue = backing.compute(key, remappingFunction);
        return newValue != null ? newValue : defaultReturnValue;
    }

    /**
     * If the specified key is not already associated with a value, attempts to compute
     * its value using the given mapping function and enters it into this map.
     * This operation is atomic.
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with the specified key,
     *         or defaultReturnValue if the computed value is null
     */
    public long computeIfAbsent(long key, LongFunction<? extends Long> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "Mapping function cannot be null");
        Long newValue = backing.computeIfAbsent(key, mappingFunction::apply);
        return newValue != null ? newValue : defaultReturnValue;
    }

    /**
     * If the value for the specified key is present, attempts to compute a new mapping
     * given the key and its current mapped value. This operation is atomic.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or defaultReturnValue if none
     */
    public long computeIfPresent(long key, BiFunction<? super Long, ? super Long, ? extends Long> remappingFunction) {
        Objects.requireNonNull(remappingFunction, "Remapping function cannot be null");
        Long newValue = backing.computeIfPresent(key, remappingFunction);
        return newValue != null ? newValue : defaultReturnValue;
    }

    /**
     * If the specified key is not already associated with a value (or is mapped to null),
     * associates it with the given value. Otherwise, replaces the associated value with
     * the results of the given remapping function. This operation is atomic.
     *
     * @param key key with which the resulting value is to be associated
     * @param value the value to be merged with the existing value
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or defaultReturnValue if none
     */
    public long merge(long key, long value, LongBinaryOperator remappingFunction) {
        Objects.requireNonNull(remappingFunction, "Remapping function cannot be null");
        Long newValue = backing.merge(key, value, (v1, v2) -> remappingFunction.applyAsLong(v1, v2));
        return newValue != null ? newValue : defaultReturnValue;
    }

    /**
     * Performs the given action for each entry in this map until all entries
     * have been processed or the action throws an exception.
     *
     * @param action the action to be performed for each entry
     */
    public void forEach(java.util.function.BiConsumer<? super Long, ? super Long> action) {
        Objects.requireNonNull(action, "Action cannot be null");
        backing.forEach(action);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Long2LongMap that)) return false;

        if (size() != that.size()) return false;

        try {
            for (Entry entry : long2LongEntrySet()) {
                long key = entry.getLongKey();
                long value = entry.getLongValue();
                if (!that.containsKey(key) || that.get(key) != value) {
                    return false;
                }
            }
        } catch (ClassCastException | NullPointerException e) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return backing.hashCode();
    }

    @Override
    public String toString() {
        return backing.toString();
    }

    /**
     * Returns the underlying ConcurrentHashMap for advanced operations.
     * Use with caution as direct modifications may bypass Long2LongMap semantics.
     *
     * @return the backing ConcurrentHashMap
     */
    public ConcurrentHashMap<Long, Long> getBackingMap() {
        return backing;
    }
}