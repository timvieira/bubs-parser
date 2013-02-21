package edu.berkeley.nlp.util;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A priority queue based on a binary heap. Note that this implementation does not efficiently support containment,
 * removal, or element promotion (decreaseKey) -- these methods are therefore not yet implemented. It is a maximum
 * priority queue, so next() gives the highest-priority object.
 * 
 * TODO Replace with a FastUtil version?
 * 
 * @author Dan Klein
 */
public class PriorityQueue<E> implements Iterator<E>, Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    private int size;
    private int capacity;
    private List<E> elements;
    private double[] priorities;

    public PriorityQueue() {
        this(15);
    }

    public PriorityQueue(final int capacity) {
        int legalCapacity = 0;
        while (legalCapacity < capacity) {
            legalCapacity = 2 * legalCapacity + 1;
        }
        grow(legalCapacity);
    }

    /**
     * @return true if the priority queue is non-empty
     */
    public boolean hasNext() {
        return !isEmpty();
    }

    /**
     * Pops and returns the head of the queue
     * 
     * @return the head of the queue
     */
    public E next() {
        final E first = peek();
        removeFirst();
        return first;
    }

    /**
     * Unsupported - use {@link #next()}
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the highest-priority element in the queue without removing it.
     * 
     * @return The head of the queue (without removing it).
     * @throws NoSuchElementException if the queue is empty
     */
    public E peek() {
        if (size() > 0) {
            return elements.get(0);
        }
        throw new NoSuchElementException();
    }

    /**
     * @return the priority of the head element in the queue.
     * 
     * @throws NoSuchElementException if the queue is empty
     */
    public double getPriority() {
        if (size() > 0) {
            return priorities[0];
        }
        throw new NoSuchElementException();
    }

    /**
     * @return The size of the queue
     */
    public int size() {
        return size;
    }

    /**
     * @return true if the queue is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Enqueues an element.
     * 
     * @param key
     * @param priority
     */
    public void add(final E key, final double priority) {
        if (size == capacity) {
            grow(2 * capacity + 1);
        }
        elements.add(key);
        priorities[size] = priority;
        heapifyUp(size);
        size++;
    }

    private void grow(final int newCapacity) {
        final List<E> newElements = new ArrayList<E>(newCapacity);
        final double[] newPriorities = new double[newCapacity];
        if (size > 0) {
            newElements.addAll(elements);
            System.arraycopy(priorities, 0, newPriorities, 0, priorities.length);
        }
        elements = newElements;
        priorities = newPriorities;
        capacity = newCapacity;
    }

    private int parent(final int loc) {
        return (loc - 1) / 2;
    }

    private int leftChild(final int loc) {
        return 2 * loc + 1;
    }

    private int rightChild(final int loc) {
        return 2 * loc + 2;
    }

    private void heapifyUp(final int loc) {
        if (loc == 0)
            return;
        final int parent = parent(loc);
        if (priorities[loc] > priorities[parent]) {
            swap(loc, parent);
            heapifyUp(parent);
        }
    }

    private void heapifyDown(final int loc) {
        int max = loc;
        final int leftChild = leftChild(loc);
        if (leftChild < size()) {
            final double priority = priorities[loc];
            final double leftChildPriority = priorities[leftChild];
            if (leftChildPriority > priority)
                max = leftChild;
            final int rightChild = rightChild(loc);
            if (rightChild < size()) {
                final double rightChildPriority = priorities[rightChild(loc)];
                if (rightChildPriority > priority && rightChildPriority > leftChildPriority)
                    max = rightChild;
            }
        }
        if (max == loc)
            return;
        swap(loc, max);
        heapifyDown(max);
    }

    private void swap(final int loc1, final int loc2) {
        final double tempPriority = priorities[loc1];
        final E tempElement = elements.get(loc1);
        priorities[loc1] = priorities[loc2];
        elements.set(loc1, elements.get(loc2));
        priorities[loc2] = tempPriority;
        elements.set(loc2, tempElement);
    }

    private void removeFirst() {
        if (size < 1)
            return;
        swap(0, size - 1);
        size--;
        elements.remove(size);
        heapifyDown(0);
    }

    /**
     * Returns a representation of the queue in decreasing priority order.
     */
    @Override
    public String toString() {
        return toString(size(), false);
    }

    /**
     * Returns a representation of the queue in decreasing priority order, displaying at most maxKeysToPrint elements
     * and optionally printing one element per line.
     * 
     * @param maxKeysToPrint
     * @param multiline TODO
     */
    public String toString(final int maxKeysToPrint, final boolean multiline) {
        final PriorityQueue<E> pq = clone();
        final StringBuilder sb = new StringBuilder(multiline ? "" : "[");
        int numKeysPrinted = 0;
        final NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(5);
        while (numKeysPrinted < maxKeysToPrint && pq.hasNext()) {
            final double priority = pq.getPriority();
            final E element = pq.next();
            sb.append(element == null ? "null" : element.toString());
            sb.append(" : ");
            sb.append(f.format(priority));
            if (numKeysPrinted < size() - 1)
                sb.append(multiline ? "\n" : ", ");
            numKeysPrinted++;
        }
        if (numKeysPrinted < size())
            sb.append("...");
        if (!multiline)
            sb.append("]");
        return sb.toString();
    }

    /**
     * Returns a clone of this priority queue. Modifications to one will not affect modifications to the other.
     */
    @Override
    public PriorityQueue<E> clone() {
        final PriorityQueue<E> clonePQ = new PriorityQueue<E>();
        clonePQ.size = size;
        clonePQ.capacity = capacity;
        clonePQ.elements = new ArrayList<E>(capacity);
        clonePQ.priorities = new double[capacity];
        if (size() > 0) {
            clonePQ.elements.addAll(elements);
            System.arraycopy(priorities, 0, clonePQ.priorities, 0, size());
        }
        return clonePQ;
    }
}
