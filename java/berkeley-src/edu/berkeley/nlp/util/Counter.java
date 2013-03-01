package edu.berkeley.nlp.util;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A map from objects to doubles. Includes convenience methods for getting, setting, and incrementing element counts.
 * Objects not in the counter will return a count of zero. The counter is backed by a HashMap (unless specified
 * otherwise with the MapFactory constructor).
 * 
 * Replace with Object2DoubleOpenHashMap (or possibly with Object2FloatOpenHashMap)
 * 
 * @author Dan Klein
 */
public class Counter<E> implements Serializable {
    private static final long serialVersionUID = 1L;

    Object2DoubleOpenHashMap<E> entries = new Object2DoubleOpenHashMap<E>();
    double total = 0.0;

    public Counter() {
    }

    /**
     * The elements in the counter.
     * 
     * @return set of keys
     */
    public Set<E> keySet() {
        return entries.keySet();
    }

    /**
     * The number of entries in the counter (not the total count -- use totalCount() instead).
     */
    public int size() {
        return entries.size();
    }

    /**
     * True if there are no entries in the counter (false does not mean totalCount > 0)
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns whether the counter contains the given key. Note that this is the way to distinguish keys which are in
     * the counter with count zero, and those which are not in the counter (and will therefore return count zero from
     * getCount().
     * 
     * @param key
     * @return whether the counter contains the key
     */
    public boolean containsKey(final E key) {
        return entries.containsKey(key);
    }

    /**
     * @param key
     * @return the count of the specified <code>key</code>, or zero if <code>key</code> is not present in the counter.
     */
    public double getCount(final E key) {
        return entries.getDouble(key);
    }

    /**
     * Set the count for the given key, clobbering any previous count.
     * 
     * @param key
     * @param count
     */
    public void setCount(final E key, final double count) {
        final double prev = entries.put(key, count);
        total += (count - prev);
    }

    /**
     * Increment a key's count by the given amount.
     * 
     * @param key
     * @param increment
     */
    public void incrementCount(final E key, final double increment) {
        entries.add(key, increment);
        total += increment;
    }

    /**
     * Finds the total of all counts in the counter. This implementation iterates through the entire counter every time
     * this method is called.
     * 
     * @return the counter's total
     */
    public double totalCount() {
        return total;
    }

    private List<E> getSortedKeys() {
        final PriorityQueue<E> pq = this.asPriorityQueue();
        final List<E> keys = new ArrayList<E>();
        while (pq.hasNext()) {
            keys.add(pq.next());
        }
        return keys;
    }

    /**
     * Returns a string representation with the keys ordered by decreasing counts.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return toString(keySet().size());
    }

    /**
     * Returns a string representation which includes no more than the maxKeysToPrint elements with largest counts.
     * 
     * @param maxKeysToPrint
     * @return partial string representation
     */
    public String toString(final int maxKeysToPrint) {
        return asPriorityQueue().toString(maxKeysToPrint, false);
    }

    /**
     * Returns a string representation which includes no more than the maxKeysToPrint elements with largest counts and
     * optionally prints one element per line.
     * 
     * @param maxKeysToPrint
     * @return partial string representation
     */
    public String toString(final int maxKeysToPrint, final boolean multiline) {
        return asPriorityQueue().toString(maxKeysToPrint, multiline);
    }

    /**
     * Builds a priority queue whose elements are the counter's elements, and whose priorities are those elements'
     * counts in the counter.
     */
    public PriorityQueue<E> asPriorityQueue() {
        final PriorityQueue<E> pq = new PriorityQueue<E>(entries.size());
        for (final Map.Entry<E, Double> entry : entries.entrySet()) {
            pq.add(entry.getKey(), entry.getValue());
        }
        return pq;
    }

    public String toStringTabSeparated() {
        final StringBuilder sb = new StringBuilder();
        for (final E key : getSortedKeys()) {
            sb.append(key.toString() + "\t" + getCount(key) + "\n");
        }
        return sb.toString();
    }

}
