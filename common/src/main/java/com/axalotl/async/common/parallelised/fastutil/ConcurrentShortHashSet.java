package com.axalotl.async.common.parallelised.fastutil;

import it.unimi.dsi.fastutil.shorts.ShortCollection;
import it.unimi.dsi.fastutil.shorts.ShortIterator;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Высокопроизводительная потокобезопасная реализация ShortSet.
 * Самодостаточная, безопасная и прагматично оптимизированная для Minecraft.
 *
 * Версия 3.0 - после код-ревью от Gemini
 */
public final class ConcurrentShortHashSet implements ShortSet {

    private final ConcurrentHashMap.KeySetView<Short, Boolean> backing;

    public ConcurrentShortHashSet() {
        this.backing = ConcurrentHashMap.newKeySet();
    }

    public ConcurrentShortHashSet(int initialCapacity) {
        this.backing = ConcurrentHashMap.newKeySet(initialCapacity);
    }

    public ConcurrentShortHashSet(@NotNull ShortCollection collection) {
        this(collection.size());
        this.addAll(collection);
    }

    // --- Основные методы ---

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
    public boolean add(short key) {
        return backing.add(key); // Автобоксинг неизбежен
    }

    @Override
    public boolean contains(short key) {
        return backing.contains(key); // Автобоксинг неизбежен
    }

    @Override
    public boolean remove(short k) {
        return backing.remove(k); // Автобоксинг неизбежен
    }

    /**
     * Ключевая оптимизация: самодостаточный примитивный итератор.
     * Использую статический внутренний класс для лучшей производительности.
     */
    @Override
    public @NotNull ShortIterator iterator() {
        return new PrimitiveShortIterator(backing.iterator());
    }

    // --- Методы преобразования в массивы (Упрощённые и безопасные) ---

    @Override
    public short[] toShortArray() {
        // Самый безопасный способ: используем потокобезопасный toArray() из backing set
        Object[] objectArray = backing.toArray();
        short[] shortArray = new short[objectArray.length];
        for (int i = 0; i < objectArray.length; i++) {
            shortArray[i] = (Short) objectArray[i]; // Анбоксинг необходим
        }
        return shortArray;
    }

    @Override
    public short[] toArray(short[] a) {
        Objects.requireNonNull(a, "Array cannot be null");

        // ИСПРАВЛЕНА ОШИБКА GEMINI: был вызов несуществующего toLongArray()
        short[] result = toShortArray();

        if (a.length < result.length) {
            return result; // Возвращаем новый массив правильного размера
        }

        // Копируем в предоставленный массив
        System.arraycopy(result, 0, a, 0, result.length);

        // По контракту FastUtil: элемент после последнего должен быть 0
        if (a.length > result.length) {
            a[result.length] = 0;
        }

        return a;
    }

    // --- Массовые операции (Упрощённые и прагматичные) ---

    @Override
    public boolean addAll(ShortCollection c) {
        Objects.requireNonNull(c, "Collection cannot be null");
        if (c.isEmpty()) return false;

        boolean modified = false;
        ShortIterator it = c.iterator();
        while (it.hasNext()) {
            if (add(it.nextShort())) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean containsAll(ShortCollection c) {
        Objects.requireNonNull(c, "Collection cannot be null");
        if (c.isEmpty()) return true;
        if (isEmpty()) return false; // Дополнительная оптимизация

        ShortIterator it = c.iterator();
        while (it.hasNext()) {
            if (!contains(it.nextShort())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean removeAll(ShortCollection c) {
        Objects.requireNonNull(c, "Collection cannot be null");
        if (c.isEmpty() || isEmpty()) return false; // Быстрые пути

        boolean modified = false;
        ShortIterator it = c.iterator();
        while (it.hasNext()) {
            if (remove(it.nextShort())) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(ShortCollection c) {
        Objects.requireNonNull(c, "Collection cannot be null");
        if (isEmpty()) return false; // Быстрый путь

        // Простая, безопасная и достаточно быстрая реализация
        return backing.removeIf(element -> !c.contains(element.shortValue()));
    }

    // --- Стандартные методы Collection ---
    // Простые делегирования, уже корректные

    @Override
    public boolean addAll(@NotNull Collection<? extends Short> collection) {
        return backing.addAll(collection);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        if (collection.isEmpty()) return true; // Быстрый путь
        return backing.containsAll(collection);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        if (collection.isEmpty() || isEmpty()) return false; // Быстрые пути
        return backing.removeAll(collection);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        return backing.retainAll(collection);
    }

    // --- Object методы ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Set)) return false;
        // Напрямую делегируем backing.equals - он корректен и сам проверяет размер
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

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        return backing.toArray();
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] array) {
        return backing.toArray(array);
    }

    /**
     * Приватный статический внутренний класс для высокопроизводительного итератора.
     * Статический класс не держит ссылку на внешний объект - меньше памяти.
     */
    private static final class PrimitiveShortIterator implements ShortIterator {
        private final Iterator<Short> backingIterator;

        PrimitiveShortIterator(Iterator<Short> backingIterator) {
            this.backingIterator = Objects.requireNonNull(backingIterator);
        }

        @Override
        public boolean hasNext() {
            return backingIterator.hasNext();
        }

        @Override
        public short nextShort() {
            return backingIterator.next(); // Единственный анбоксинг
        }

        @Override
        public void remove() {
            backingIterator.remove();
        }
    }
}