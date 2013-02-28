package edu.berkeley.nlp.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maintains counts of (key, value) pairs. The map is structured so that for every key, one can get a counter over
 * values. Example usage: keys might be words with values being POS tags, and the count being the number of occurences
 * of that word/tag pair. The sub-counters returned by getCounter(word) would be count distributions over tags for that
 * word.
 * 
 * @author Dan Klein
 */
public class CounterMap<K, V> implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    HashMap<K, Counter<V>> counterMap;
    double defltVal = 0.0;

    public CounterMap() {
        counterMap = new HashMap<K, Counter<V>>();
    }

    protected Counter<V> ensureCounter(final K key) {
        Counter<V> valueCounter = counterMap.get(key);
        if (valueCounter == null) {
            valueCounter = new Counter<V>();
            valueCounter.setDeflt(defltVal);
            counterMap.put(key, valueCounter);
        }
        return valueCounter;
    }

    /**
     * Returns the keys that have been inserted into this CounterMap.
     */
    public Set<K> keySet() {
        return counterMap.keySet();
    }

    /**
     * Increments the count for a particular (key, value) pair.
     */
    public void incrementCount(final K key, final V value, final double count) {
        final Counter<V> valueCounter = ensureCounter(key);
        valueCounter.incrementCount(value, count);
    }

    /**
     * Gets the count of the given (key, value) entry, or zero if that entry is not present. Does not create any
     * objects.
     */
    public double getCount(final K key, final V value) {
        final Counter<V> valueCounter = counterMap.get(key);
        if (valueCounter == null)
            return defltVal;
        return valueCounter.getCount(value);
    }

    /**
     * Gets the total count of the given key, or zero if that key is not present. Does not create any objects.
     */
    public double getCount(final K key) {
        final Counter<V> valueCounter = counterMap.get(key);
        if (valueCounter == null)
            return 0.0;
        return valueCounter.totalCount();
    }

    /**
     * Returns the total of all counts in sub-counters. This implementation is linear; it recalculates the total each
     * time.
     */
    public double totalCount() {
        double total = 0.0;
        for (final Map.Entry<K, Counter<V>> entry : counterMap.entrySet()) {
            final Counter<V> counter = entry.getValue();
            total += counter.totalCount();
        }
        return total;
    }

    /**
     * Returns the total number of (key, value) entries in the CounterMap (not their total counts).
     */
    public int totalSize() {
        int total = 0;
        for (final Map.Entry<K, Counter<V>> entry : counterMap.entrySet()) {
            final Counter<V> counter = entry.getValue();
            total += counter.size();
        }
        return total;
    }

    /**
     * The number of keys in this CounterMap (not the number of key-value entries -- use totalSize() for that)
     */
    public int size() {
        return counterMap.size();
    }

    /**
     * True if there are no entries in the CounterMap (false does not mean totalCount > 0)
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    public String toString(final int maxValsPerKey) {
        final StringBuilder sb = new StringBuilder("[\n");
        for (final Map.Entry<K, Counter<V>> entry : counterMap.entrySet()) {
            sb.append("  ");
            sb.append(entry.getKey());
            sb.append(" -> ");
            sb.append(entry.getValue().toString(maxValsPerKey));
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean containsKey(final K key) {
        return counterMap.containsKey(key);
    }

    public void removeKey(final K oldIndex) {
        counterMap.remove(oldIndex);
    }

    @Override
    public String toString() {
        return toString(20);
    }

    public String toString(final Collection<String> keyFilter) {
        final StringBuilder sb = new StringBuilder("[\n");
        for (final Map.Entry<K, Counter<V>> entry : counterMap.entrySet()) {
            if (keyFilter != null && !keyFilter.contains(entry.getKey())) {
                continue;
            }
            sb.append("  ");
            sb.append(entry.getKey());
            sb.append(" -> ");
            sb.append(entry.getValue().toString(20));
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
}
