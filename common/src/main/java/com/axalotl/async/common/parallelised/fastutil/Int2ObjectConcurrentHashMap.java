package com.axalotl.async.common.parallelised.fastutil;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Высокопроизводительная thread-safe реализация Int2ObjectMap.
 * Финальная версия - результат совместного мозгового штурма.
 *
 * Ключевые особенности:
 * - Полностью самодостаточная (без внешних зависимостей от HackUtil)
 * - FastEntrySet с обычным и быстрым итератором
 * - ThreadLocal оптимизация для fastIterator
 * - Минимальные аллокации и boxing/unboxing
 *
 * @param <V> тип значений в мапе
 */
public final class Int2ObjectConcurrentHashMap<V> implements Int2ObjectMap<V> {

    private final ConcurrentHashMap<Integer, V> backing;
    private volatile V defaultReturnValue;

    // Ленивая инициализация view-коллекций
    private volatile IntSet keySetView;
    private volatile ObjectCollection<V> valuesView;
    private volatile FastEntrySet<V> entrySetView;

    /**
     * Создаёт пустую конкурентную мапу с дефолтной ёмкостью
     */
    public Int2ObjectConcurrentHashMap() {
        this.backing = new ConcurrentHashMap<>();
    }

    /**
     * Создаёт мапу с указанной начальной ёмкостью
     */
    public Int2ObjectConcurrentHashMap(int initialCapacity) {
        this.backing = new ConcurrentHashMap<>(initialCapacity);
    }

    /**
     * Создаёт мапу с указанной ёмкостью и фактором загрузки
     */
    public Int2ObjectConcurrentHashMap(int initialCapacity, float loadFactor) {
        this.backing = new ConcurrentHashMap<>(initialCapacity, loadFactor);
    }

    // ========== Основные операции ==========

    @Override
    public V get(int key) {
        V value = backing.get(key);
        return value != null ? value : defaultReturnValue;
    }

    @Override
    public V put(int key, V value) {
        Objects.requireNonNull(value, "Null values are not supported");
        V prev = backing.put(key, value);
        return prev != null ? prev : defaultReturnValue;
    }

    @Override
    public V remove(int key) {
        V prev = backing.remove(key);
        return prev != null ? prev : defaultReturnValue;
    }

    @Override
    public boolean containsKey(int key) {
        return backing.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return value != null && backing.containsValue(value);
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

    // ========== Расширенные операции ==========

    @Override
    public V getOrDefault(int key, V defaultValue) {
        return backing.getOrDefault(key, defaultValue);
    }

    @Override
    public V putIfAbsent(int key, V value) {
        Objects.requireNonNull(value, "Null values are not supported");
        V prev = backing.putIfAbsent(key, value);
        return prev != null ? prev : defaultReturnValue;
    }

    @Override
    public boolean remove(int key, Object value) {
        return backing.remove(key, value);
    }

    @Override
    public boolean replace(int key, V oldValue, V newValue) {
        Objects.requireNonNull(newValue, "New value cannot be null");
        return backing.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(int key, V value) {
        Objects.requireNonNull(value, "Value cannot be null");
        V prev = backing.replace(key, value);
        return prev != null ? prev : defaultReturnValue;
    }

    @Override
    public V compute(int key, BiFunction<? super Integer, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction, "Remapping function cannot be null");
        V newValue = backing.compute(key, remappingFunction);
        return newValue != null ? newValue : defaultReturnValue;
    }

    // ========== Default Return Value ==========

    @Override
    public void defaultReturnValue(V rv) {
        this.defaultReturnValue = rv;
    }

    @Override
    public V defaultReturnValue() {
        return defaultReturnValue;
    }

    // ========== View Collections ==========

    @Override
    public @NotNull IntSet keySet() {
        IntSet ks = keySetView;
        if (ks == null) {
            synchronized (this) {
                ks = keySetView;
                if (ks == null) {
                    keySetView = ks = new IntSetView(backing.keySet());
                }
            }
        }
        return ks;
    }

    @Override
    public @NotNull ObjectCollection<V> values() {
        ObjectCollection<V> vals = valuesView;
        if (vals == null) {
            synchronized (this) {
                vals = valuesView;
                if (vals == null) {
                    valuesView = vals = new ValuesView<>(backing.values());
                }
            }
        }
        return vals;
    }

    @Override
    public FastEntrySet<V> int2ObjectEntrySet() {
        FastEntrySet<V> es = entrySetView;
        if (es == null) {
            synchronized (this) {
                es = entrySetView;
                if (es == null) {
                    entrySetView = es = new FastEntrySetImpl(backing.entrySet());
                }
            }
        }
        return es;
    }

    // ========== Bulk Operations ==========

    @Override
    public void putAll(@NotNull Map<? extends Integer, ? extends V> m) {
        Objects.requireNonNull(m, "Source map cannot be null");
        backing.putAll(m);
    }

    // ========== Object Methods ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
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

    // ========== Внутренние классы-обёртки ==========

    /**
     * Оптимизированная обёртка для IntSet над Set<Integer>
     */
    private static final class IntSetView implements IntSet {
        private final Set<Integer> backing;

        IntSetView(Set<Integer> backing) {
            this.backing = backing;
        }

        @Override
        public @NotNull IntIterator iterator() {
            return new IntIterator() {
                private final Iterator<Integer> it = backing.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public int nextInt() {
                    return it.next(); // Единственный unboxing
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
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
        public boolean contains(int key) {
            return backing.contains(key);
        }

        @Override
        public boolean add(int key) {
            return backing.add(key);
        }

        @Override
        public boolean remove(int key) {
            return backing.remove(key);
        }

        @Override
        public void clear() {
            backing.clear();
        }

        @Override
        public int[] toIntArray() {
            int[] array = new int[backing.size()];
            int i = 0;
            for (Integer val : backing) {
                array[i++] = val;
            }
            return array;
        }

        @Override
        public int[] toArray(int[] a) {
            int size = backing.size();
            int[] result = a.length >= size ? a : new int[size];

            int i = 0;
            for (Integer val : backing) {
                result[i++] = val;
            }

            if (result.length > size) {
                result[size] = 0;
            }
            return result;
        }

        @Override
        public boolean addAll(IntCollection c) {
            if (c.isEmpty()) return false;

            boolean modified = false;
            IntIterator it = c.iterator();
            while (it.hasNext()) {
                if (backing.add(it.nextInt())) {
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean containsAll(IntCollection c) {
            IntIterator it = c.iterator();
            while (it.hasNext()) {
                if (!backing.contains(it.nextInt())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean removeAll(IntCollection c) {
            if (c.isEmpty()) return false;

            boolean modified = false;
            IntIterator it = c.iterator();
            while (it.hasNext()) {
                if (backing.remove(it.nextInt())) {
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean retainAll(IntCollection c) {
            return backing.retainAll(c);
        }

        @Override
        public Object @NotNull [] toArray() {
            return backing.toArray();
        }

        @Override
        public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
            return backing.toArray(a);
        }

        @Override
        public boolean containsAll(@NotNull java.util.Collection<?> c) {
            return backing.containsAll(c);
        }

        @Override
        public boolean addAll(@NotNull java.util.Collection<? extends Integer> c) {
            return backing.addAll(c);
        }

        @Override
        public boolean removeAll(@NotNull java.util.Collection<?> c) {
            return backing.removeAll(c);
        }

        @Override
        public boolean retainAll(@NotNull java.util.Collection<?> c) {
            return backing.retainAll(c);
        }
    }

    /**
     * Оптимизированная обёртка для values коллекции
     */
    private static final class ValuesView<V> implements ObjectCollection<V> {
        private final java.util.Collection<V> backing;

        ValuesView(java.util.Collection<V> backing) {
            this.backing = backing;
        }

        @Override
        public @NotNull ObjectIterator<V> iterator() {
            return new ObjectIterator<V>() {
                private final Iterator<V> it = backing.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public V next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
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
        public boolean contains(Object o) {
            return backing.contains(o);
        }

        @Override
        public Object @NotNull [] toArray() {
            return backing.toArray();
        }

        @Override
        public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
            return backing.toArray(a);
        }

        @Override
        public boolean add(V v) {
            throw new UnsupportedOperationException("Cannot add to values collection");
        }

        @Override
        public boolean remove(Object o) {
            return backing.remove(o);
        }

        @Override
        public boolean containsAll(@NotNull java.util.Collection<?> c) {
            return backing.containsAll(c);
        }

        @Override
        public boolean addAll(@NotNull java.util.Collection<? extends V> c) {
            throw new UnsupportedOperationException("Cannot add to values collection");
        }

        @Override
        public boolean removeAll(@NotNull java.util.Collection<?> c) {
            return backing.removeAll(c);
        }

        @Override
        public boolean retainAll(@NotNull java.util.Collection<?> c) {
            return backing.retainAll(c);
        }

        @Override
        public void clear() {
            backing.clear();
        }
    }

    /**
     * FastEntrySet интерфейс для поддержки обычного и быстрого итераторов
     */
    public interface FastEntrySet<V> extends ObjectSet<Entry<V>> {
        ObjectIterator<Entry<V>> fastIterator();
        void fastForEach(Consumer<? super Entry<V>> consumer);
    }

    /**
     * Оптимизированная реализация FastEntrySet
     */
    private final class FastEntrySetImpl implements FastEntrySet<V> {
        private final Set<Map.Entry<Integer, V>> backingSet;
        private final ThreadLocal<MutableEntry> threadLocalEntry =
                ThreadLocal.withInitial(MutableEntry::new);

        FastEntrySetImpl(Set<Map.Entry<Integer, V>> backingSet) {
            this.backingSet = backingSet;
        }

        @Override
        public @NotNull ObjectIterator<Entry<V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public ObjectIterator<Entry<V>> fastIterator() {
            return new FastEntryIterator(threadLocalEntry.get());
        }

        @Override
        public void fastForEach(Consumer<? super Entry<V>> consumer) {
            final MutableEntry entry = threadLocalEntry.get();
            backingSet.forEach(e -> {
                entry.mapEntry = e;
                consumer.accept(entry);
            });
        }

        @Override
        public int size() {
            return backingSet.size();
        }

        @Override
        public boolean isEmpty() {
            return backingSet.isEmpty();
        }

        @Override
        public void clear() {
            backingSet.clear();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry)) return false;
            Entry<?> e = (Entry<?>) o;
            int key = e.getIntKey();
            V value = Int2ObjectConcurrentHashMap.this.get(key);
            return value != null && value.equals(e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry)) return false;
            Entry<?> e = (Entry<?>) o;
            return Int2ObjectConcurrentHashMap.this.remove(e.getIntKey(), e.getValue());
        }

        @Override
        public boolean add(Entry<V> e) {
            V oldValue = Int2ObjectConcurrentHashMap.this.put(e.getIntKey(), e.getValue());
            return !Objects.equals(oldValue, e.getValue());
        }

        @Override
        public Object @NotNull [] toArray() {
            Object[] array = new Object[backingSet.size()];
            int i = 0;
            for (Map.Entry<Integer, V> e : backingSet) {
                array[i++] = new ImmutableEntry<>(e);
            }
            return array;
        }

        @Override
        public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
            int size = backingSet.size();
            @SuppressWarnings("unchecked")
            T[] result = a.length >= size ? a : (T[]) java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(), size);

            int i = 0;
            for (Map.Entry<Integer, V> e : backingSet) {
                @SuppressWarnings("unchecked")
                T entry = (T) new ImmutableEntry<>(e);
                result[i++] = entry;
            }

            if (result.length > size) {
                result[size] = null;
            }
            return result;
        }

        @Override
        public boolean containsAll(@NotNull java.util.Collection<?> c) {
            for (Object o : c) {
                if (!contains(o)) return false;
            }
            return true;
        }

        @Override
        public boolean addAll(@NotNull java.util.Collection<? extends Entry<V>> c) {
            boolean modified = false;
            for (Entry<V> e : c) {
                if (add(e)) modified = true;
            }
            return modified;
        }

        @Override
        public boolean removeAll(@NotNull java.util.Collection<?> c) {
            boolean modified = false;
            for (Object o : c) {
                if (remove(o)) modified = true;
            }
            return modified;
        }

        @Override
        public boolean retainAll(@NotNull java.util.Collection<?> c) {
            Objects.requireNonNull(c);
            boolean modified = false;
            Iterator<Entry<V>> it = iterator();
            while (it.hasNext()) {
                if (!c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }
    }

    /**
     * Безопасный итератор с иммутабельными Entry
     */
    private final class EntryIterator implements ObjectIterator<Entry<V>> {
        private final Iterator<Map.Entry<Integer, V>> iterator = backing.entrySet().iterator();

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Entry<V> next() {
            return new ImmutableEntry<>(iterator.next());
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    /**
     * Быстрый итератор с переиспользуемым MutableEntry
     */
    private final class FastEntryIterator implements ObjectIterator<Entry<V>> {
        private final Iterator<Map.Entry<Integer, V>> iterator = backing.entrySet().iterator();
        private final MutableEntry entry;

        FastEntryIterator(MutableEntry entry) {
            this.entry = entry;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Entry<V> next() {
            entry.mapEntry = iterator.next();
            return entry;
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    /**
     * Переиспользуемый мутабельный Entry для fastIterator
     */
    private final class MutableEntry implements Entry<V> {
        private Map.Entry<Integer, V> mapEntry;

        @Override
        public int getIntKey() {
            return mapEntry.getKey();
        }

        @Override
        public Integer getKey() {
            return mapEntry.getKey();
        }

        @Override
        public V getValue() {
            return mapEntry.getValue();
        }

        @Override
        public V setValue(V value) {
            Objects.requireNonNull(value, "Null values are not supported");
            return mapEntry.setValue(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return Objects.equals(getKey(), e.getKey()) &&
                    Objects.equals(getValue(), e.getValue());
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(getIntKey()) ^
                    (getValue() == null ? 0 : getValue().hashCode());
        }

        @Override
        public String toString() {
            return getIntKey() + "=" + getValue();
        }
    }

    /**
     * Иммутабельный Entry для безопасного итератора и toArray
     */
    private static final class ImmutableEntry<V> implements Entry<V> {
        private final int key;
        private final V value;

        ImmutableEntry(Map.Entry<Integer, V> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        @Override
        public int getIntKey() {
            return key;
        }

        @Override
        public Integer getKey() {
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
            if (this == o) return true;
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue());
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(key) ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
}