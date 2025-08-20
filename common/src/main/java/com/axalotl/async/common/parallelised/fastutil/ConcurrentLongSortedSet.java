package com.axalotl.async.common.parallelised.fastutil;

import it.unimi.dsi.fastutil.longs.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Простая, корректная и надёжная реализация thread-safe LongSortedSet.
 * Никаких "умных" оптимизаций - только прямая делегация к ConcurrentSkipListSet.
 */
public final class ConcurrentLongSortedSet implements LongSortedSet {

    private final NavigableSet<Long> backing;

    public ConcurrentLongSortedSet() {
        this.backing = new ConcurrentSkipListSet<>();
    }

    public ConcurrentLongSortedSet(Collection<Long> initialCollection) {
        this();
        addAll(Objects.requireNonNull(initialCollection));
    }

    // Приватный конструктор для View
    private ConcurrentLongSortedSet(NavigableSet<Long> backingView) {
        this.backing = backingView;
    }

    // Простая делегация без "оптимизаций"
    @Override
    public boolean add(long key) {
        return backing.add(key);
    }

    @Override
    public boolean contains(long key) {
        return backing.contains(key);
    }

    @Override
    public boolean remove(long key) {
        return backing.remove(key);
    }

    @Override
    public @NotNull LongBidirectionalIterator iterator() {
        return new SimpleLongIterator(backing.iterator());
    }

    @Override
    public LongBidirectionalIterator iterator(long fromElement) {
        return new SimpleLongIterator(backing.tailSet(fromElement, true).iterator());
    }

    // Правильные View (единственное, что было хорошо в предыдущей версии)
    @Override
    public LongSortedSet subSet(long fromElement, long toElement) {
        return new ConcurrentLongSortedSet(backing.subSet(fromElement, toElement));
    }

    @Override
    public LongSortedSet headSet(long toElement) {
        return new ConcurrentLongSortedSet(backing.headSet(toElement));
    }

    @Override
    public LongSortedSet tailSet(long fromElement) {
        return new ConcurrentLongSortedSet(backing.tailSet(fromElement));
    }

    // Простые методы для массивов
    @Override
    public long[] toLongArray() {
        Object[] objects = backing.toArray();
        long[] result = new long[objects.length];
        for (int i = 0; i < objects.length; i++) {
            result[i] = (Long) objects[i];
        }
        return result;
    }

    @Override
    public long[] toArray(long[] array) {
        Objects.requireNonNull(array);
        int size = backing.size();
        long[] result = array.length >= size ? array : new long[size];

        int i = 0;
        for (Long value : backing) {
            result[i++] = value;
        }

        if (result.length > size) {
            result[size] = 0L;
        }
        return result;
    }

    // Простая итерация для примитивных коллекций
    @Override
    public boolean addAll(LongCollection c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        LongIterator it = c.iterator();
        while (it.hasNext()) {
            if (add(it.nextLong())) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean containsAll(LongCollection c) {
        Objects.requireNonNull(c);
        LongIterator it = c.iterator();
        while (it.hasNext()) {
            if (!contains(it.nextLong())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(LongCollection c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        LongIterator it = c.iterator();
        while (it.hasNext()) {
            if (remove(it.nextLong())) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(LongCollection c) {
        Objects.requireNonNull(c);
        return backing.removeIf(element -> !c.contains(element.longValue()));
    }

    // Остальные методы - простая делегация
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
    public LongComparator comparator() {
        return null;
    }

    @Override
    public long firstLong() {
        return backing.first();
    }

    @Override
    public long lastLong() {
        return backing.last();
    }

    @Override
    public Object @NotNull [] toArray() {
        return backing.toArray();
    }

    @Override
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] array) {
        return backing.toArray(array);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return backing.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Long> c) {
        return backing.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return backing.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return backing.retainAll(c);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof java.util.Set)) return false;
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

    // Простой итератор без "оптимизаций"
    private static final class SimpleLongIterator implements LongBidirectionalIterator {
        private final java.util.Iterator<Long> it;

        SimpleLongIterator(java.util.Iterator<Long> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public long nextLong() {
            return it.next();
        }

        @Override
        public void remove() {
            it.remove();
        }

        @Override
        public long previousLong() {
            throw new UnsupportedOperationException("Backward iteration not supported");
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }
    }
}