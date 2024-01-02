package io.github.mjcro.circular;

import java.util.Iterator;

/**
 * Utility iterator.
 *
 * @param <E> Iterator element type.
 */
final class ArrayIterator<E> implements Iterator<E> {
    private final E[] data;
    private int position, index;

    /**
     * Constructor.
     *
     * @param data     Data array.
     * @param position Position to start from, could be negative.
     * @param index    Max index to iterate until.
     */
    ArrayIterator(E[] data, int position, int index) {
        this.data = data;
        this.position = position;
        this.index = index;
    }

    @Override
    public boolean hasNext() {
        return position < index;
    }

    @Override
    public E next() {
        E e = data[position < 0 ? position + data.length : position];
        position++;
        return e;
    }
}
