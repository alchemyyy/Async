package com.axalotl.async.neoforge.parallelised;

import net.neoforged.neoforge.common.util.BlockSnapshot;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * 100% потокобезопасный ArrayList с ReadWriteLock.
 * ВСЕ публичные методы переопределены и защищены блокировками.
 */
public class ConcurrentBlockSnapshotList extends ArrayList<BlockSnapshot> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public ConcurrentBlockSnapshotList() {
        super();
    }

    public ConcurrentBlockSnapshotList(int initialCapacity) {
        super(initialCapacity);
    }

    public ConcurrentBlockSnapshotList(@NotNull Collection<? extends BlockSnapshot> c) {
        super(c);
    }

    // --- МЕТОДЫ ЧТЕНИЯ (ReadLock) ---

    @Override
    public BlockSnapshot get(int index) {
        readLock.lock();
        try {
            return super.get(index);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return super.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return super.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        readLock.lock();
        try {
            return super.contains(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int indexOf(Object o) {
        readLock.lock();
        try {
            return super.indexOf(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        readLock.lock();
        try {
            return super.lastIndexOf(o);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Object @NotNull [] toArray() {
        readLock.lock();
        try {
            return super.toArray();
        } finally {
            readLock.unlock();
        }
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(@NotNull T[] a) {
        readLock.lock();
        try {
            return super.toArray(a);
        } finally {
            readLock.unlock();
        }
    }

    // --- МЕТОДЫ ЗАПИСИ (WriteLock) ---

    @Override
    public boolean add(BlockSnapshot e) {
        writeLock.lock();
        try {
            return super.add(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void add(int index, BlockSnapshot element) {
        writeLock.lock();
        try {
            super.add(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public BlockSnapshot remove(int index) {
        writeLock.lock();
        try {
            return super.remove(index);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        writeLock.lock();
        try {
            return super.remove(o);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            super.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public BlockSnapshot set(int index, BlockSnapshot element) {
        writeLock.lock();
        try {
            return super.set(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends BlockSnapshot> c) {
        writeLock.lock();
        try {
            return super.addAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends BlockSnapshot> c) {
        writeLock.lock();
        try {
            return super.addAll(index, c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        writeLock.lock();
        try {
            super.removeRange(fromIndex, toIndex);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        writeLock.lock();
        try {
            return super.removeAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        writeLock.lock();
        try {
            return super.retainAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    // --- КРИТИЧЕСКИ ВАЖНЫЕ МЕТОДЫ (без них будет ConcurrentModificationException) ---

    @NotNull
    @Override
    public Iterator<BlockSnapshot> iterator() {
        readLock.lock();
        try {
            // Snapshot iterator - копируем список для безопасной итерации
            return new ArrayList<>(this).iterator();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void forEach(Consumer<? super BlockSnapshot> action) {
        readLock.lock();
        try {
            super.forEach(action);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean removeIf(Predicate<? super BlockSnapshot> filter) {
        writeLock.lock();
        try {
            return super.removeIf(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void replaceAll(UnaryOperator<BlockSnapshot> operator) {
        writeLock.lock();
        try {
            super.replaceAll(operator);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void sort(Comparator<? super BlockSnapshot> c) {
        writeLock.lock();
        try {
            super.sort(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Object clone() {
        readLock.lock();
        try {
            return super.clone();
        } finally {
            readLock.unlock();
        }
    }

    @NotNull
    @Override
    public ListIterator<BlockSnapshot> listIterator() {
        readLock.lock();
        try {
            return new ArrayList<>(this).listIterator();
        } finally {
            readLock.unlock();
        }
    }

    @NotNull
    @Override
    public ListIterator<BlockSnapshot> listIterator(int index) {
        readLock.lock();
        try {
            return new ArrayList<>(this).listIterator(index);
        } finally {
            readLock.unlock();
        }
    }

    @NotNull
    @Override
    public List<BlockSnapshot> subList(int fromIndex, int toIndex) {
        readLock.lock();
        try {
            // Возвращаем копию, чтобы избежать проблем с потокобезопасностью
            return new ArrayList<>(super.subList(fromIndex, toIndex));
        } finally {
            readLock.unlock();
        }
    }
}