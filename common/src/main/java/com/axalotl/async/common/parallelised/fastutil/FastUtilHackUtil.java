package com.axalotl.async.common.parallelised.fastutil;

import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 * Optimized utility for creating FastUtil-compatible views over standard Java collections.
 *
 * Design principles:
 * - Simplicity over complexity - no caching, no ThreadLocal
 * - Direct delegation where possible
 * - Optimized iterators for hot paths
 * - Full interface compliance
 *
 * This version combines the best ideas from both Claude and Gemini:
 * - Simple, direct views (Gemini's approach)
 * - Complete interface implementation (addressing Claude's concern)
 * - No dangerous optimizations (learning from mistakes)
 */
public final class FastUtilHackUtil {

    private FastUtilHackUtil() {
        throw new AssertionError("No instances");
    }

    /**
     * Creates a FastUtil-compatible LongSet view over a standard Set<Long>.
     * Optimized for iteration performance in Minecraft entity processing.
     */
    public static LongSet wrapLongSet(Set<Long> backingSet) {
        return new DirectLongSetView(backingSet);
    }

    /**
     * Creates a FastUtil-compatible ObjectCollection view over a standard Collection<V>.
     * Provides efficient iteration without intermediate wrappers.
     */
    public static <V> ObjectCollection<V> wrap(Collection<V> backingCollection) {
        return new DirectObjectCollectionView<>(backingCollection);
    }

    /**
     * Direct, efficient LongSet implementation.
     * Key optimization: primitive iterator avoids creating Long objects during iteration.
     */
    private static final class DirectLongSetView implements LongSet {
        private final Set<Long> backing;

        DirectLongSetView(Set<Long> backing) {
            this.backing = Objects.requireNonNull(backing, "Backing set cannot be null");
        }

        // --- Hot path: Optimized iterator ---
        @Override
        public @NotNull LongIterator iterator() {
            return new LongIterator() {
                private final Iterator<Long> it = backing.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public long nextLong() {
                    return it.next(); // Single unboxing, unavoidable with ConcurrentHashMap
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        // --- Primitive array methods (required by LongSet) ---
        @Override
        public long[] toLongArray() {
            final int size = backing.size();
            final long[] array = new long[size];
            int i = 0;
            for (Long value : backing) {
                array[i++] = value; // Unboxing during array creation
            }
            return array;
        }

        @Override
        public long[] toArray(long[] a) {
            final int size = backing.size();
            final long[] result = a.length >= size ? a : new long[size];

            int i = 0;
            for (Long value : backing) {
                result[i++] = value;
            }

            // FastUtil contract: set element after last to 0
            if (result.length > size) {
                result[size] = 0L;
            }
            return result;
        }

        // --- Primitive collection operations ---
        @Override
        public boolean addAll(LongCollection c) {
            // Special optimization for primitive collections
            if (c.isEmpty()) return false;

            boolean modified = false;
            LongIterator it = c.iterator();
            while (it.hasNext()) {
                if (backing.add(it.nextLong())) { // Auto-boxing here
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean containsAll(LongCollection c) {
            LongIterator it = c.iterator();
            while (it.hasNext()) {
                if (!backing.contains(it.nextLong())) { // Auto-boxing here
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean removeAll(LongCollection c) {
            if (c.isEmpty()) return false;

            boolean modified = false;
            LongIterator it = c.iterator();
            while (it.hasNext()) {
                if (backing.remove(it.nextLong())) { // Auto-boxing here
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean retainAll(LongCollection c) {
            // This is complex to optimize, delegate to standard implementation
            return backing.retainAll(c);
        }

        // --- Simple delegating methods (not performance critical) ---
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
        public boolean contains(long key) {
            return backing.contains(key); // Auto-boxing
        }

        @Override
        public boolean remove(long key) {
            return backing.remove(key); // Auto-boxing
        }

        @Override
        public boolean add(long key) {
            return backing.add(key); // Auto-boxing
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
    }

    /**
     * Direct, efficient ObjectCollection implementation.
     * Provides fast iteration without intermediate object creation.
     */
    private static final class DirectObjectCollectionView<V> implements ObjectCollection<V> {
        private final Collection<V> backing;

        DirectObjectCollectionView(Collection<V> backing) {
            this.backing = Objects.requireNonNull(backing, "Backing collection cannot be null");
        }

        // --- Hot path: Direct iterator ---
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

        // --- Simple delegating methods ---
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
            return backing.contains(o);
        }

        @Override
        public boolean remove(Object o) {
            return backing.remove(o);
        }

        @Override
        public boolean add(V v) {
            return backing.add(v);
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
        public boolean containsAll(@NotNull Collection<?> c) {
            return backing.containsAll(c);
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends V> c) {
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
    }
}