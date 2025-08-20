package com.axalotl.async.common.parallelised.fastutil;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.LongFunction;

/**
 * Простая, надежная и эффективная thread-safe реализация Long2ObjectMap.
 *
 * Финальная версия после тщательного code review процесса.
 * Приоритет - корректность и простота через использование проверенного ConcurrentHashMap из JDK.
 *
 * Ключевые решения:
 * - НЕ изобретаем велосипед - используем ConcurrentHashMap
 * - Полагаемся на автобоксинг JVM (он уже оптимизирован для -128..127)
 * - ThreadLocal для Entry - единственная сложная оптимизация
 * - Minecraft-специфичные методы для удобства
 *
 * @param <V> тип значений
 */
public final class Long2ObjectConcurrentHashMap<V> implements Long2ObjectMap<V> {

    private final ConcurrentHashMap<Long, V> backing;
    private volatile V defaultReturnValue;

    private volatile FastEntrySet<V> entrySet;
    private volatile LongSet keySetView;
    private volatile ObjectCollection<V> valuesView;

    // ThreadLocal для переиспользования Entry - единственная сложная оптимизация
    // которая действительно дает эффект при интенсивной итерации
    private final ThreadLocal<MutableEntry> threadLocalEntry =
            ThreadLocal.withInitial(MutableEntry::new);

    // Конструкторы

    public Long2ObjectConcurrentHashMap() {
        this.backing = new ConcurrentHashMap<>();
    }

    public Long2ObjectConcurrentHashMap(int initialCapacity) {
        this.backing = new ConcurrentHashMap<>(initialCapacity);
    }

    public Long2ObjectConcurrentHashMap(int initialCapacity, float loadFactor) {
        this.backing = new ConcurrentHashMap<>(initialCapacity, loadFactor);
    }

    public Long2ObjectConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this.backing = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
    }

    // Основные операции - максимально простые, полагаемся на автобоксинг JVM

    @Override
    public V get(long key) {
        // Простой автобоксинг. JVM кэширует Long от -128 до 127 автоматически
        V value = backing.get(key);
        return value == null ? defaultReturnValue : value;
    }

    @Override
    public V put(long key, V value) {
        Objects.requireNonNull(value, "Null values are not supported");
        V previous = backing.put(key, value);
        return previous == null ? defaultReturnValue : previous;
    }

    @Override
    public V remove(long key) {
        V previous = backing.remove(key);
        return previous == null ? defaultReturnValue : previous;
    }

    @Override
    public boolean containsKey(long key) {
        return backing.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return value != null && backing.containsValue(value);
    }

    @Override
    public void clear() {
        backing.clear();
        // Сбрасываем кэшированные views
        entrySet = null;
        keySetView = null;
        valuesView = null;
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public void defaultReturnValue(V rv) {
        this.defaultReturnValue = rv;
    }

    @Override
    public V defaultReturnValue() {
        return defaultReturnValue;
    }

    // Оптимизированные операции

    @Override
    public V getOrDefault(long key, V defaultValue) {
        return backing.getOrDefault(key, defaultValue);
    }

    @Override
    public V putIfAbsent(long key, V value) {
        Objects.requireNonNull(value, "Null values are not supported");

        // Используем атомарный putIfAbsent напрямую
        V previous = backing.putIfAbsent(key, value);

        // Если previous == null, значит мы вставили новое значение
        // и должны вернуть defaultReturnValue (что было раньше)
        // Если previous != null, значит элемент уже существовал
        // и мы возвращаем его
        return previous == null ? defaultReturnValue : previous;
    }

    @Override
    public boolean remove(long key, Object value) {
        return backing.remove(key, value);
    }

    @Override
    public boolean replace(long key, V oldValue, V newValue) {
        Objects.requireNonNull(newValue, "New value cannot be null");
        return backing.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(long key, V value) {
        Objects.requireNonNull(value, "Value cannot be null");
        V previous = backing.replace(key, value);
        return previous == null ? defaultReturnValue : previous;
    }

    @Override
    public V compute(long key, BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction, "Remapping function cannot be null");
        V newValue = backing.compute(key, remappingFunction);
        return newValue == null ? defaultReturnValue : newValue;
    }

    @Override
    public V computeIfAbsent(long key, LongFunction<? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "Mapping function cannot be null");
        // Адаптируем LongFunction к Function<Long, V>
        V value = backing.computeIfAbsent(key, k -> mappingFunction.apply(k));
        return value == null ? defaultReturnValue : value;
    }

    @Override
    public V computeIfPresent(long key, BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction, "Remapping function cannot be null");
        V newValue = backing.computeIfPresent(key, remappingFunction);
        return newValue == null ? defaultReturnValue : newValue;
    }

    @Override
    public V merge(long key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(value, "Value cannot be null");
        Objects.requireNonNull(remappingFunction, "Remapping function cannot be null");
        V merged = backing.merge(key, value, remappingFunction);
        return merged == null ? defaultReturnValue : merged;
    }

    @Override
    public void putAll(@NotNull Map<? extends Long, ? extends V> m) {
        Objects.requireNonNull(m, "Source map cannot be null");
        // Валидируем все значения перед вставкой
        for (V value : m.values()) {
            Objects.requireNonNull(value, "Null values are not supported in the source map");
        }
        backing.putAll(m);
    }

    // Batch операции - простые и эффективные

    /**
     * Batch get - простая итерация, без излишних аллокаций.
     */
    public void batchGet(long[] keys, V[] results) {
        for (int i = 0; i < keys.length; i++) {
            results[i] = get(keys[i]);
        }
    }

    /**
     * Batch contains - проверка существования множества ключей.
     */
    public void batchContainsKey(long[] keys, boolean[] results) {
        for (int i = 0; i < keys.length; i++) {
            results[i] = containsKey(keys[i]);
        }
    }

    /**
     * Batch remove - удаление множества ключей с подсчетом.
     */
    public int batchRemove(long[] keys) {
        int removed = 0;
        for (long key : keys) {
            if (backing.remove(key) != null) {
                removed++;
            }
        }
        return removed;
    }

    /**
     * Условное удаление по предикату.
     */
    public int removeIf(java.util.function.Predicate<Entry<V>> predicate) {
        int sizeBefore = backing.size();
        backing.entrySet().removeIf(entry ->
                predicate.test(new ImmutableEntry<>(entry.getKey(), entry.getValue())));
        return sizeBefore - backing.size();
    }

    // Views - интеграция с FastUtilHackUtil

    @Override
    public FastEntrySet<V> long2ObjectEntrySet() {
        FastEntrySet<V> es = entrySet;
        if (es == null) {
            synchronized (this) {
                es = entrySet;
                if (es == null) {
                    entrySet = es = new FastEntrySetImpl();
                }
            }
        }
        return es;
    }

    @Override
    public @NotNull LongSet keySet() {
        LongSet ks = keySetView;
        if (ks == null) {
            synchronized (this) {
                ks = keySetView;
                if (ks == null) {
                    keySetView = ks = FastUtilHackUtil.wrapLongSet(backing.keySet());
                }
            }
        }
        return ks;
    }

    @Override
    public @NotNull ObjectCollection<V> values() {
        ObjectCollection<V> vs = valuesView;
        if (vs == null) {
            synchronized (this) {
                vs = valuesView;
                if (vs == null) {
                    valuesView = vs = FastUtilHackUtil.wrap(backing.values());
                }
            }
        }
        return vs;
    }

    // FastEntrySet реализация

    private final class FastEntrySetImpl implements FastEntrySet<V> {

        @Override
        public @NotNull ObjectIterator<Entry<V>> iterator() {
            return new ObjectIterator<Entry<V>>() {
                private final Iterator<Map.Entry<Long, V>> it = backing.entrySet().iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Entry<V> next() {
                    Map.Entry<Long, V> entry = it.next();
                    return new ImmutableEntry<>(entry.getKey(), entry.getValue());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public ObjectIterator<Entry<V>> fastIterator() {
            // Использует ThreadLocal для минимизации аллокаций
            return new ObjectIterator<Entry<V>>() {
                private final Iterator<Map.Entry<Long, V>> it = backing.entrySet().iterator();
                private final MutableEntry entry = threadLocalEntry.get();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Entry<V> next() {
                    Map.Entry<Long, V> e = it.next();
                    entry.key = e.getKey();
                    entry.value = e.getValue();
                    return entry;
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public void fastForEach(Consumer<? super Entry<V>> consumer) {
            final MutableEntry entry = threadLocalEntry.get();
            backing.forEach((k, v) -> {
                entry.key = k;
                entry.value = v;
                consumer.accept(entry);
            });
        }

        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public boolean isEmpty() {
            return backing.isEmpty();
        }

        @Override
        public void clear() {
            backing.clear();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry)) return false;
            Entry<?> entry = (Entry<?>) o;
            V value = backing.get(entry.getLongKey());
            return value != null && value.equals(entry.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry)) return false;
            Entry<?> entry = (Entry<?>) o;
            return backing.remove(entry.getLongKey(), entry.getValue());
        }

        @Override
        public boolean add(Entry<V> entry) {
            Objects.requireNonNull(entry.getValue(), "Null values are not supported");
            V previousValue = backing.put(entry.getLongKey(), entry.getValue());
            return !Objects.equals(previousValue, entry.getValue());
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            for (Object o : c) {
                if (!contains(o)) return false;
            }
            return true;
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends Entry<V>> c) {
            boolean modified = false;
            for (Entry<V> entry : c) {
                if (add(entry)) modified = true;
            }
            return modified;
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            Objects.requireNonNull(c);
            boolean modified = false;

            // Оптимизация для больших коллекций
            if (c.size() > 10 && backing.size() > 100) {
                Set<?> removeSet = (c instanceof Set) ? (Set<?>) c : new HashSet<>(c);
                return backing.entrySet().removeIf(entry ->
                        removeSet.contains(new ImmutableEntry<>(entry.getKey(), entry.getValue())));
            }

            // Для маленьких - простая итерация
            for (Object o : c) {
                if (remove(o)) modified = true;
            }
            return modified;
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            Objects.requireNonNull(c);
            Set<?> retainSet = (c instanceof Set) ? (Set<?>) c : new HashSet<>(c);
            return backing.entrySet().removeIf(entry ->
                    !retainSet.contains(new ImmutableEntry<>(entry.getKey(), entry.getValue())));
        }

        @Override
        public Object @NotNull [] toArray() {
            return backing.entrySet().stream()
                    .map(e -> new ImmutableEntry<>(e.getKey(), e.getValue()))
                    .toArray();
        }

        @Override
        public <T> T @NotNull [] toArray( T @NotNull [] a) {
            return backing.entrySet().stream()
                    .map(e -> new ImmutableEntry<>(e.getKey(), e.getValue()))
                    .toArray(size -> a.length >= size ? a : Arrays.copyOf(a, size));
        }
    }

    // Entry классы

    /**
     * Изменяемый Entry для переиспользования в fastIterator.
     * Важная оптимизация для уменьшения GC pressure при интенсивной итерации.
     */
    private final class MutableEntry implements Entry<V> {
        long key;
        V value;

        @Override
        public long getLongKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            Objects.requireNonNull(value, "Null values are not supported");
            this.value = value;
            return Long2ObjectConcurrentHashMap.this.put(key, value);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            if (!(e.getKey() instanceof Long)) return false;
            return key == (Long) e.getKey() && Objects.equals(value, e.getValue());
        }

        @Override
        public int hashCode() {
            return Long.hashCode(key) ^ Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    /**
     * Неизменяемый Entry для безопасной передачи наружу.
     */
    private static final class ImmutableEntry<V> implements Entry<V> {
        private final long key;
        private final V value;

        ImmutableEntry(long key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public long getLongKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("Entry is immutable");
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            if (!(e.getKey() instanceof Long)) return false;
            return key == (Long) e.getKey() && Objects.equals(value, e.getValue());
        }

        @Override
        public int hashCode() {
            return Long.hashCode(key) ^ Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    // Методы Object

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Map)) return false;
        return backing.equals(o);
    }

    @Override
    public int hashCode() {
        return backing.hashCode();
    }

    @Override
    public String toString() {
        return backing.toString();
    }

    // Minecraft-специфичные оптимизации

    /**
     * Получение чанка по координатам.
     * Использует стандартную упаковку координат Minecraft.
     */
    public V getChunk(int x, int z) {
        long key = ((long)x & 0xFFFFFFFFL) | (((long)z & 0xFFFFFFFFL) << 32);
        return get(key);
    }

    /**
     * Сохранение чанка.
     */
    public V putChunk(int x, int z, V chunk) {
        long key = ((long)x & 0xFFFFFFFFL) | (((long)z & 0xFFFFFFFFL) << 32);
        return put(key, chunk);
    }

    /**
     * Удаление чанка.
     */
    public V removeChunk(int x, int z) {
        long key = ((long)x & 0xFFFFFFFFL) | (((long)z & 0xFFFFFFFFL) << 32);
        return remove(key);
    }

    /**
     * Проверка существования чанка.
     */
    public boolean containsChunk(int x, int z) {
        long key = ((long)x & 0xFFFFFFFFL) | (((long)z & 0xFFFFFFFFL) << 32);
        return containsKey(key);
    }

    /**
     * Получение всех чанков в радиусе.
     * Оптимизировано для последовательного доступа.
     */
    public List<V> getChunksInRadius(int centerX, int centerZ, int radius) {
        List<V> chunks = new ArrayList<>((2 * radius + 1) * (2 * radius + 1));

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                V chunk = getChunk(x, z);
                if (chunk != null && chunk != defaultReturnValue) {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    /**
     * Получение всех чанков в прямоугольной области.
     */
    public List<V> getChunksInArea(int minX, int minZ, int maxX, int maxZ) {
        List<V> chunks = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                V chunk = getChunk(x, z);
                if (chunk != null && chunk != defaultReturnValue) {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    /**
     * Атомарное обновление чанка если он существует.
     */
    public V computeChunkIfPresent(int x, int z, BiFunction<? super Long, ? super V, ? extends V> remappingFunction) {
        long key = ((long)x & 0xFFFFFFFFL) | (((long)z & 0xFFFFFFFFL) << 32);
        return computeIfPresent(key, remappingFunction);
    }

    /**
     * Информация о внутреннем состоянии для отладки.
     */
    public String getDebugInfo() {
        return String.format(
                "Long2ObjectConcurrentHashMap[size=%d, capacity=%d, concurrency=%d]",
                size(),
                16, // ConcurrentHashMap default initial capacity
                16  // ConcurrentHashMap default concurrency level
        );
    }

    /**
     * Метод для тестирования производительности.
     * Возвращает среднее время операции в наносекундах.
     */
    public long benchmarkGet(long[] keys, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (long key : keys) {
                get(key);
            }
        }
        long end = System.nanoTime();
        return (end - start) / (iterations * keys.length);
    }
}