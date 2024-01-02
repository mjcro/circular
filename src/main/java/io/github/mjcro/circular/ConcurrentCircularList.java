package io.github.mjcro.circular;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Simple implementation of circular, thread-safe collection with fixed capacity.
 * Whenever a new element is added when collection is full, it overwrites the oldest one.
 * <p>
 * Implementation via array instead of classic {@link java.util.LinkedList} made intentionally
 * to achieve minimal memory and GC footprint (the closest distance to GC roots)
 * <p>
 * This component can be useful to hold small amount of not-critical diagnostics like logs .
 * <p>
 * Thread safety is achieved using naive synchronization approach.
 *
 * @param <E> Element type.
 */
public class ConcurrentCircularList<E> implements Collection<E> {
    // Data array
    private final E[] elements;
    // Index to write next element being added
    private int index = 0;
    // Count of overflow cycles already being made
    private long cycles = 0;

    /**
     * Constructs component with given capacity.
     *
     * @param capacity Capacity.
     */
    public ConcurrentCircularList(int capacity) {
        this(capacity, null);
    }

    /**
     * Constructs component with given capacity and prefills it with
     * provided initial data.
     *
     * @param capacity Capacity.
     * @param data     Initial data, optional, nullable.
     */
    public ConcurrentCircularList(int capacity, Collection<? extends E> data) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Illegal capacity " + capacity);
        }
        this.elements = (E[]) new Object[capacity];
        this.addAll(data);
    }

    /**
     * Internal method (for testing purposes) to obtain element by
     * it's index.
     *
     * @param i Element index.
     * @return Element.
     */
    synchronized E get(int i) {
        return elements[i];
    }

    /**
     * @return Amount of items was added to this collection.
     */
    public long getCount() {
        return cycles * elements.length + index;
    }

    @Override
    public int size() {
        return cycles > 0 ? elements.length : index;
    }

    @Override
    public boolean isEmpty() {
        return index == 0 && cycles == 0;
    }

    @Override
    public synchronized boolean contains(Object o) {
        int max = size();
        for (int i = 0; i < max; i++) {
            if (Objects.equals(o, elements[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized Iterator<E> iterator() {
        E[] data = (E[]) toArray();
        return new ArrayIterator<>(data, 0, data.length);
    }

    @Override
    public synchronized Object[] toArray() {
        if (cycles == 0 || index == 0) {
            return Arrays.copyOf(this.elements, size());
        }
        Object[] response = new Object[size()];
        System.arraycopy(this.elements, index, response, 0, this.elements.length - index);
        System.arraycopy(this.elements, 0, response, this.elements.length - index, index);
        return response;
    }

    @Override
    public synchronized <T> T[] toArray(T[] a) {
        if (a.length < size()) {
            return (T[]) Arrays.copyOf(toArray(), size(), a.getClass());
        } else {
            System.arraycopy(toArray(), 0, a, 0, size());
            if (a.length > size()) {
                a[size()] = null;
            }

            return a;
        }
    }

    @Override
    public synchronized boolean add(E e) {
        elements[index] = e;
        if (++index == elements.length) {
            index = 0;
            cycles++;
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public synchronized boolean containsAll(Collection<?> collection) {
        for (Object o : collection) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }
        for (E e : collection) {
            add(e);
        }
        return true;
    }

    /**
     * Returns iterator containing only N last elements.
     *
     * @param n Number of elements.
     * @return Iterator.
     */
    public synchronized Iterator<E> tailIterator(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("Invalid tail size " + n);
        } else if (n >= size()) {
            return iterator();
        }

        E[] data = (E[]) toArray();
        return new ArrayIterator<>(data, data.length - n, data.length);
    }

    /**
     * Returns stream containing only N last elements.
     *
     * @param n Number of elements.
     * @return Stream.
     */
    public Stream<E> tailStream(int n) {
        return StreamSupport.stream(Spliterators.spliterator(tailIterator(n), n > size() ? size() : n, 0), false);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("removeAll");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("retainAll");
    }

    @Override
    public synchronized void clear() {
        Arrays.fill(elements, null);
        index = 0;
        cycles = 0;
    }
}
