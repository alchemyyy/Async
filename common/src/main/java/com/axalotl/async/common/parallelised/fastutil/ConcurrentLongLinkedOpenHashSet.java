package com.axalotl.async.common.parallelised.fastutil;

import it.unimi.dsi.fastutil.longs.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.StampedLock;

/**
 * Высокопроизводительная потокобезопасная реализация LongLinkedOpenHashSet.
 *
 * Архитектура:
 * - Наследуется от LongLinkedOpenHashSet для совместимости с Minecraft Mixins
 * - Использует StampedLock для оптимистичного чтения
 * - Поддерживает полноценный изменяемый итератор
 *
 * @author Синтез идей Claude и Gemini
 */
public final class ConcurrentLongLinkedOpenHashSet extends LongLinkedOpenHashSet {

    private final StampedLock lock = new StampedLock();

    // --- Конструкторы ---

    public ConcurrentLongLinkedOpenHashSet() {
        super();
    }

    public ConcurrentLongLinkedOpenHashSet(int expected, float loadFactor) {
        super(expected, loadFactor);
    }

    public ConcurrentLongLinkedOpenHashSet(Collection<? extends Long> c) {
        super(c);
    }

    // --- Операции чтения с оптимистичной блокировкой ---

    @Override
    public boolean contains(final long k) {
        long stamp = lock.tryOptimisticRead();
        boolean result = super.contains(k);

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                result = super.contains(k);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return result;
    }

    @Override
    public int size() {
        long stamp = lock.tryOptimisticRead();
        int size = super.size();

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                size = super.size();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        long stamp = lock.tryOptimisticRead();
        boolean empty = super.isEmpty();

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                empty = super.isEmpty();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return empty;
    }

    // --- Операции записи с эксклюзивной блокировкой ---

    @Override
    public boolean add(final long k) {
        long stamp = lock.writeLock();
        try {
            return super.add(k);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean remove(final long k) {
        long stamp = lock.writeLock();
        try {
            return super.remove(k);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public void clear() {
        long stamp = lock.writeLock();
        try {
            super.clear();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // --- Batch операции ---

    @Override
    public boolean addAll(Collection<? extends Long> c) {
        if (c.isEmpty()) return false;

        long stamp = lock.writeLock();
        try {
            return super.addAll(c);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean addAll(LongCollection c) {
        if (c.isEmpty()) return false;

        long stamp = lock.writeLock();
        try {
            return super.addAll(c);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        if (c.isEmpty()) return false;

        long stamp = lock.writeLock();
        try {
            return super.removeAll(c);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean removeAll(LongCollection c) {
        if (c.isEmpty()) return false;

        long stamp = lock.writeLock();
        try {
            return super.removeAll(c);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        long stamp = lock.writeLock();
        try {
            return super.retainAll(c);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean retainAll(LongCollection c) {
        long stamp = lock.writeLock();
        try {
            return super.retainAll(c);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        if (c.isEmpty()) return true;

        long stamp = lock.readLock();
        try {
            return super.containsAll(c);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public boolean containsAll(LongCollection c) {
        if (c.isEmpty()) return true;

        long stamp = lock.readLock();
        try {
            return super.containsAll(c);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    // --- Операции с порядком элементов ---

    @Override
    public long firstLong() {
        long stamp = lock.readLock();
        try {
            return super.firstLong();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public long lastLong() {
        long stamp = lock.readLock();
        try {
            return super.lastLong();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public long removeFirstLong() {
        long stamp = lock.writeLock();
        try {
            return super.removeFirstLong();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public long removeLastLong() {
        long stamp = lock.writeLock();
        try {
            return super.removeLastLong();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // --- Преобразование в массивы ---

    @Override
    public long[] toLongArray() {
        long stamp = lock.readLock();
        try {
            return super.toLongArray();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public long[] toArray(long[] a) {
        long stamp = lock.readLock();
        try {
            return super.toArray(a);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public Object @NotNull [] toArray() {
        long stamp = lock.readLock();
        try {
            return super.toArray();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public <T> T @NotNull [] toArray(@NotNull T @NotNull [] a) {
        long stamp = lock.readLock();
        try {
            return super.toArray(a);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    // --- Итератор с поддержкой удаления ---

    /**
     * Возвращает потокобезопасный итератор с поддержкой remove().
     *
     * Важно: Итератор берёт блокировку на каждую операцию.
     * Это компромисс между производительностью и безопасностью.
     */
    @Override
    public @NotNull LongListIterator iterator() {
        return new StampedLockIterator(super.iterator(), lock);
    }

    // --- Служебные методы ---

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        long stamp = lock.readLock();
        try {
            return super.equals(obj);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public int hashCode() {
        long stamp = lock.readLock();
        try {
            return super.hashCode();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public String toString() {
        long stamp = lock.readLock();
        try {
            return super.toString();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public ConcurrentLongLinkedOpenHashSet clone() {
        long stamp = lock.readLock();
        try {
            return (ConcurrentLongLinkedOpenHashSet) super.clone();
        } finally {
            lock.unlockRead(stamp);
        }
    }

    /**
     * Потокобезопасный итератор с прямым использованием StampedLock.
     *
     * Стратегия блокировок:
     * - hasNext/next/previous используют readLock (могут выполняться параллельно)
     * - remove использует writeLock (эксклюзивный доступ)
     *
     * Это позволяет нескольким потокам итерировать одновременно,
     * но гарантирует безопасность при удалении.
     */
    private static final class StampedLockIterator implements LongListIterator {
        private final LongListIterator delegate;
        private final StampedLock lock;
        private long lastReturned = Long.MIN_VALUE; // Для отслеживания последнего элемента
        private boolean canRemove = false;

        StampedLockIterator(LongListIterator delegate, StampedLock lock) {
            this.delegate = delegate;
            this.lock = lock;
        }

        @Override
        public boolean hasNext() {
            long stamp = lock.readLock();
            try {
                return delegate.hasNext();
            } finally {
                lock.unlockRead(stamp);
            }
        }

        @Override
        public long nextLong() {
            long stamp = lock.readLock();
            try {
                lastReturned = delegate.nextLong();
                canRemove = true;
                return lastReturned;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        @Override
        public boolean hasPrevious() {
            long stamp = lock.readLock();
            try {
                return delegate.hasPrevious();
            } finally {
                lock.unlockRead(stamp);
            }
        }

        @Override
        public long previousLong() {
            long stamp = lock.readLock();
            try {
                lastReturned = delegate.previousLong();
                canRemove = true;
                return lastReturned;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        @Override
        public void remove() {
            if (!canRemove) {
                throw new IllegalStateException("remove() can only be called after next() or previous()");
            }

            long stamp = lock.writeLock();
            try {
                delegate.remove();
                canRemove = false;
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        @Override
        public int nextIndex() {
            long stamp = lock.readLock();
            try {
                return delegate.nextIndex();
            } finally {
                lock.unlockRead(stamp);
            }
        }

        @Override
        public int previousIndex() {
            long stamp = lock.readLock();
            try {
                return delegate.previousIndex();
            } finally {
                lock.unlockRead(stamp);
            }
        }
    }
}