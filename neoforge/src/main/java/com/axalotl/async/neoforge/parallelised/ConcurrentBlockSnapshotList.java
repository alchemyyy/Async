package com.axalotl.async.neoforge.parallelised;

import net.neoforged.neoforge.common.util.BlockSnapshot;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConcurrentBlockSnapshotList extends ArrayList<BlockSnapshot> {
    private final CopyOnWriteArrayList<BlockSnapshot> backing = new CopyOnWriteArrayList<>();

    @Override
    public boolean add(BlockSnapshot e) {
        return backing.add(e);
    }

    @Override
    public void add(int index, BlockSnapshot element) {
        backing.add(index, element);
    }

    @Override
    public BlockSnapshot get(int index) {
        return backing.get(index);
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
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public BlockSnapshot remove(int index) {
        return backing.remove(index);
    }

    @Override
    public boolean remove(Object o) {
        return backing.remove(o);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends BlockSnapshot> c) {
        return backing.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends BlockSnapshot> c) {
        return backing.addAll(index, c);
    }

    @NotNull
    @Override
    public Iterator<BlockSnapshot> iterator() {
        return backing.iterator();
    }

    @Override
    public Object[] toArray() {
        return backing.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return backing.toArray(a);
    }

    @Override
    public boolean contains(Object o) {
        return backing.contains(o);
    }
}