package edu.berkeley.nlp.PCFGLA;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.berkeley.nlp.util.ArrayUtil;

/**
 * Based on Counter.
 * 
 * A map from objects to doubles. Includes convenience methods for getting, setting, and incrementing element counts.
 * Objects not in the counter will return a count of zero. The counter is backed by a HashMap (unless specified
 * otherwise with the MapFactory constructor).
 * 
 * TODO Replace with FastUtil implementation
 * 
 * @author Slav Petrov
 */
public class BinaryCounterTable implements Serializable {

    private static final long serialVersionUID = 1L;
    Map<BinaryRule, double[][][]> entries;
    short[] numSubStates;
    // TODO Remove?
    BinaryRule searchKey;

    /**
     * The elements in the counter.
     * 
     * @return set of keys
     */
    public Set<BinaryRule> keySet() {
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
        return size() == 0;
    }

    /**
     * Returns whether the counter contains the given key. Note that this is the way to distinguish keys which are in
     * the counter with count zero, and those which are not in the counter (and will therefore return count zero from
     * getCount().
     * 
     * @param key
     * @return whether the counter contains the key
     */
    public boolean containsKey(final BinaryRule key) {
        return entries.containsKey(key);
    }

    /**
     * @param key
     * @return the count of the specified element, or null if the element is not in the counter.
     */
    public double[][][] getCount(final BinaryRule key) {
        final double[][][] value = entries.get(key);
        return value;
    }

    public double[][][] getCount(final short pState, final short lState, final short rState) {
        searchKey.setNodes(pState, lState, rState);
        final double[][][] value = entries.get(searchKey);
        return value;
    }

    /**
     * Set the count for the given key, clobbering any previous count.
     * 
     * @param key
     * @param counts
     */
    public void setCount(final BinaryRule key, final double[][][] counts) {
        entries.put(key, counts);
    }

    /**
     * Increment a key's count by the given amount. Assumes for efficiency that the arrays have the same size.
     * 
     * @param key
     * @param increment
     */
    public void incrementCount(final BinaryRule key, final double[][][] increment) {
        final double[][][] current = getCount(key);
        if (current == null) {
            setCount(key, increment);
            return;
        }
        for (int i = 0; i < current.length; i++) {
            for (int j = 0; j < current[i].length; j++) {
                // test if increment[i][j] is null or zero, in which case
                // we needn't add it
                if (increment[i][j] == null)
                    continue;
                // allocate more space as needed
                if (current[i][j] == null)
                    current[i][j] = new double[increment[i][j].length];
                // if we've gotten here, then both current and increment
                // have correct arrays in index i
                for (int k = 0; k < current[i][j].length; k++) {
                    current[i][j][k] += increment[i][j][k];
                }
            }
        }
        setCount(key, current);
    }

    public void incrementCount(final BinaryRule key, final double increment) {
        double[][][] current = getCount(key);
        if (current == null) {
            final double[][][] tmp = key.getScores2();
            current = new double[tmp.length][tmp[0].length][tmp[0][0].length];
            ArrayUtil.fill(current, increment);
            setCount(key, current);
            return;
        }
        for (int i = 0; i < current.length; i++) {
            for (int j = 0; j < current[i].length; j++) {
                if (current[i][j] == null)
                    current[i][j] = new double[numSubStates[key.getParentState()]];
                for (int k = 0; k < current[i][j].length; k++) {
                    current[i][j][k] += increment;
                }
            }
        }
        setCount(key, current);
    }

    public BinaryCounterTable(final short[] numSubStates) {
        entries = new HashMap<BinaryRule, double[][][]>();
        searchKey = new BinaryRule((short) 0, (short) 0, (short) 0);
        this.numSubStates = numSubStates;
    }
}
