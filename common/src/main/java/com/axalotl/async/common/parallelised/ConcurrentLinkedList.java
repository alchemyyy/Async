// common/src/main/java/com/axalotl/async/common/parallelised/ConcurrentLinkedList.java
package com.axalotl.async.common.parallelised;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Thread-safe LinkedList с поддержкой iterator.remove() для Create BeltInventory
 */
public class ConcurrentLinkedList<E> extends LinkedList<E> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public ConcurrentLinkedList() {
        super();
    }

    public ConcurrentLinkedList(@NotNull Collection<? extends E> c) {
        super(c);
    }

    // --- READ OPERATIONS ---

    @Override
    public E get(int index) {
        readLock.lock();
        try {
            return super.get(index);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public E getFirst() {
        readLock.lock();
        try {
            return super.getFirst();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public E getLast() {
        readLock.lock();
        try {
            return super.getLast();
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

    @Override
    public E peek() {
        readLock.lock();
        try {
            return super.peek();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public E element() {
        readLock.lock();
        try {
            return super.element();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public E peekFirst() {
        readLock.lock();
        try {
            return super.peekFirst();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public E peekLast() {
        readLock.lock();
        try {
            return super.peekLast();
        } finally {
            readLock.unlock();
        }
    }

    // --- WRITE OPERATIONS ---

    @Override
    public boolean add(E e) {
        writeLock.lock();
        try {
            return super.add(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void add(int index, E element) {
        writeLock.lock();
        try {
            super.add(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void addFirst(E e) {
        writeLock.lock();
        try {
            super.addFirst(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void addLast(E e) {
        writeLock.lock();
        try {
            super.addLast(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E remove(int index) {
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
    public E removeFirst() {
        writeLock.lock();
        try {
            return super.removeFirst();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E removeLast() {
        writeLock.lock();
        try {
            return super.removeLast();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E poll() {
        writeLock.lock();
        try {
            return super.poll();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E remove() {
        writeLock.lock();
        try {
            return super.remove();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E pollFirst() {
        writeLock.lock();
        try {
            return super.pollFirst();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E pollLast() {
        writeLock.lock();
        try {
            return super.pollLast();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean offer(E e) {
        writeLock.lock();
        try {
            return super.offer(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean offerFirst(E e) {
        writeLock.lock();
        try {
            return super.offerFirst(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean offerLast(E e) {
        writeLock.lock();
        try {
            return super.offerLast(e);
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
    public E set(int index, E element) {
        writeLock.lock();
        try {
            return super.set(index, element);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        writeLock.lock();
        try {
            return super.addAll(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends E> c) {
        writeLock.lock();
        try {
            return super.addAll(index, c);
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

    @Override
    public boolean removeFirstOccurrence(Object o) {
        writeLock.lock();
        try {
            return super.removeFirstOccurrence(o);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        writeLock.lock();
        try {
            return super.removeLastOccurrence(o);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void push(E e) {
        writeLock.lock();
        try {
            super.push(e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public E pop() {
        writeLock.lock();
        try {
            return super.pop();
        } finally {
            writeLock.unlock();
        }
    }

    // === КРИТИЧЕСКИЕ МЕТОДЫ - ПОДДЕРЖКА iterator.remove() ===

    /**
     * Thread-safe iterator с поддержкой remove()
     * Использует snapshot + синхронизированное удаление
     */
    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new ConcurrentIterator();
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        return new ConcurrentListIterator(0);
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(int index) {
        return new ConcurrentListIterator(index);
    }

    @NotNull
    @Override
    public Iterator<E> descendingIterator() {
        readLock.lock();
        try {
            return new LinkedList<>(this).descendingIterator();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        readLock.lock();
        try {
            super.forEach(action);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean removeIf(@NotNull Predicate<? super E> filter) {
        writeLock.lock();
        try {
            return super.removeIf(filter);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void replaceAll(@NotNull UnaryOperator<E> operator) {
        writeLock.lock();
        try {
            super.replaceAll(operator);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void sort(Comparator<? super E> c) {
        writeLock.lock();
        try {
            super.sort(c);
        } finally {
            writeLock.unlock();
        }
    }

    @NotNull
    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        readLock.lock();
        try {
            return new ArrayList<>(super.subList(fromIndex, toIndex));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Object clone() {
        readLock.lock();
        try {
            return new ConcurrentLinkedList<>(this);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public @NotNull Spliterator<E> spliterator() {
        readLock.lock();
        try {
            return new ArrayList<>(this).spliterator();
        } finally {
            readLock.unlock();
        }
    }

    // === ВНУТРЕННИЕ КЛАССЫ ===

    /**
     * Iterator с поддержкой remove() - делает snapshot но remove() работает с оригиналом
     */
    private class ConcurrentIterator implements Iterator<E> {
        private final List<E> snapshot;
        private final Iterator<E> snapshotIterator;
        private E lastReturned;
        private boolean canRemove;

        ConcurrentIterator() {
            readLock.lock();
            try {
                this.snapshot = new ArrayList<>(ConcurrentLinkedList.this);
            } finally {
                readLock.unlock();
            }
            this.snapshotIterator = snapshot.iterator();
            this.canRemove = false;
        }

        @Override
        public boolean hasNext() {
            return snapshotIterator.hasNext();
        }

        @Override
        public E next() {
            lastReturned = snapshotIterator.next();
            canRemove = true;
            return lastReturned;
        }

        @Override
        public void remove() {
            if (!canRemove) {
                throw new IllegalStateException("next() not called or remove() already called");
            }
            // Удаляем из оригинального списка
            ConcurrentLinkedList.this.remove(lastReturned);
            canRemove = false;
        }
    }

    /**
     * ListIterator с поддержкой remove/set/add
     */
    private class ConcurrentListIterator implements ListIterator<E> {
        private final List<E> snapshot;
        private final ListIterator<E> snapshotIterator;
        private E lastReturned;
        private boolean canModify;

        ConcurrentListIterator(int index) {
            readLock.lock();
            try {
                this.snapshot = new ArrayList<>(ConcurrentLinkedList.this);
            } finally {
                readLock.unlock();
            }
            this.snapshotIterator = snapshot.listIterator(index);
            this.canModify = false;
        }

        @Override
        public boolean hasNext() {
            return snapshotIterator.hasNext();
        }

        @Override
        public E next() {
            lastReturned = snapshotIterator.next();
            canModify = true;
            return lastReturned;
        }

        @Override
        public boolean hasPrevious() {
            return snapshotIterator.hasPrevious();
        }

        @Override
        public E previous() {
            lastReturned = snapshotIterator.previous();
            canModify = true;
            return lastReturned;
        }

        @Override
        public int nextIndex() {
            return snapshotIterator.nextIndex();
        }

        @Override
        public int previousIndex() {
            return snapshotIterator.previousIndex();
        }

        @Override
        public void remove() {
            if (!canModify) {
                throw new IllegalStateException();
            }
            ConcurrentLinkedList.this.remove(lastReturned);
            canModify = false;
        }

        @Override
        public void set(E e) {
            if (!canModify) {
                throw new IllegalStateException();
            }
            writeLock.lock();
            try {
                int index = ConcurrentLinkedList.super.indexOf(lastReturned);
                if (index >= 0) {
                    ConcurrentLinkedList.super.set(index, e);
                }
            } finally {
                writeLock.unlock();
            }
        }

        @Override
        public void add(E e) {
            ConcurrentLinkedList.this.add(e);
            canModify = false;
        }
    }
}