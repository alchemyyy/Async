package com.axalotl.async.common.parallelised;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Factory methods for creating thread-safe collection instances.
 */
public class ConcurrentCollections {

    /**
     * Creates a new thread-safe set
     */
    public static <T> Set<T> newHashSet() {
        return Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    /**
     * Creates a new thread-safe map
     */
    public static <T, U> Map<T, U> newHashMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Creates a new thread-safe linked list
     */
    public static <T> List<T> newLinkedList() {
        return new ConcurrentLinkedList<>();
    }

    /**
     * Creates a collector that accumulates elements into a thread-safe list
     */
    public static <T> java.util.stream.Collector<T, ?, List<T>> toList() {
        return Collectors.toCollection(CopyOnWriteArrayList::new);
    }
}