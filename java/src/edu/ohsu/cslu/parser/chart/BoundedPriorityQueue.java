/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser.chart;

import java.util.Arrays;

import edu.ohsu.cslu.grammar.Grammar;

/**
 * Represents a one-shot bounded priority queue of non-terminal cell entries, ordered by a figure-of-merit. Stored as a
 * double-ended queue so we can simultaneously pop the max-priority entries and replace the min-priority entries.
 * 
 * One-shot behavior: We know that we'll never pop more than the initial maxSize edges, so once we start popping, we
 * reduce maxSize with each pop, reducing the number of edges subsequently added which will never make it to the head of
 * the queue.
 * 
 * 
 * We could replace the bubble-sort with a min-max heap or a tree, but in our (limited) experiments, we've found
 * bubble-sort to be faster than other data structures at small beam widths (we often use a beam of 30 for pruned search
 * with the Berkeley grammar).
 * 
 * @author Aaron Dunlop
 * @since Sep 10, 2010
 */
public class BoundedPriorityQueue {

    /**
     * Parallel array storing a bounded cell population (parents and a figure-of-merit for each). Analagous to
     * {@link ParallelArrayChart#insideProbabilities} and {@link PackedArrayChart#nonTerminalIndices}. The most probable
     * entry should be stored in index 0.
     */
    public final short[] parentIndices;
    public final float[] foms;

    /** The array index of the head (maximum-probability) entry. */
    private int head = -1;

    /** The array index of the tail (minimum-probability) entry. */
    private int tail = -1;

    /** The maximum tail index (as determined by the current size bound) */
    private int maxTail;

    /** Optional reference to a {@link Grammar} instance. Used in {@link #toString()}. */
    private final Grammar grammar;

    public BoundedPriorityQueue(final int maxSize, final Grammar grammar) {
        foms = new float[maxSize];
        Arrays.fill(foms, Float.NEGATIVE_INFINITY);
        parentIndices = new short[maxSize];
        this.grammar = grammar;
        this.maxTail = maxSize - 1;
    }

    public BoundedPriorityQueue(final int maxSize) {
        this(maxSize, null);
    }

    public int maxSize() {
        return maxTail + 1;
    }

    public void setMaxSize(final int maxSize) {
        final int currentMaxSize = maxTail - head + 1;
        if (maxSize != currentMaxSize) {
            if (maxSize > parentIndices.length) {
                throw new IllegalArgumentException("Specified size (" + maxSize + ") exceeds storage capacity ("
                        + parentIndices.length + ")");
            }
            if (maxSize < 0) {
                throw new IllegalArgumentException("Negative size specified (" + maxSize + ")");
            }
            this.maxTail = maxSize - 1;

            final int size = size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    parentIndices[i] = parentIndices[i + head];
                    foms[i] = foms[i + head];
                }
                head = 0;
                tail = maxSize - 1;
                Arrays.fill(foms, tail, foms.length, Float.NEGATIVE_INFINITY);
            } else {
                Arrays.fill(foms, 0, foms.length, Float.NEGATIVE_INFINITY);
                head = -1;
                tail = 0;
            }
        }
    }

    public void clear(final int maxSize) {
        head = -1;
        tail = -1;
        maxTail = maxSize - 1;
        Arrays.fill(foms, Float.NEGATIVE_INFINITY);
        Arrays.fill(parentIndices, (short) 0);
    }

    /**
     * Returns the array index of the head (maximum-probability) entry.
     * 
     * @return the array index of the head (maximum-probability) entry.
     */
    public int headIndex() {
        return head;
    }

    /**
     * Removes the head (maximum-probability) entry.
     */
    public boolean popHead() {
        if (tail < 0 || tail < head) {
            return false;
        }

        head++;
        return true;
    }

    /**
     * Inserts an entry in the priority queue, if its figure-of-merit meets the current threshold (ejecting the lowest
     * item in the queue if the size bound is exceeded). Returns true if the parent was inserted into the queue and
     * false if the entry did not fit into the queue (i.e., the queue is full and the figure-of-merit was less than the
     * lowest queue entry).
     * 
     * @param parentIndex
     * @param fom
     * @return true if the parent was inserted into the queue
     */
    public boolean insert(final short parentIndex, final float fom) {

        if (tail == maxTail) {
            // Ignore entries which are less probable than the minimum-priority entry
            if (fom <= foms[tail]) {
                return false;
            }
        } else {
            tail++;
        }

        if (head < 0) {
            head = 0;
        }

        foms[tail] = fom;
        parentIndices[tail] = parentIndex;

        // Bubble-sort the new entry into the queue
        for (int i = tail; i > head && foms[i - 1] < foms[i]; i--) {
            swap(i - 1, i);
        }

        return true;
    }

    /**
     * Replaces the figure-of-merit for a parent if the new FOM is greater than the current FOM. Returns true if the
     * parent was found and replaced.
     * 
     * @param parentIndex
     * @param fom
     * @return True if the parent was found and replaced.
     */
    public boolean replace(final short parentIndex, final float fom) {
        if (fom <= foms[maxTail]) {
            return false;
        }

        for (int i = head; i <= tail; i++) {
            if (parentIndices[i] == parentIndex) {
                if (fom > foms[i]) {
                    foms[i] = fom;
                    return true;
                }
                return false;
            }
        }
        return insert(parentIndex, fom);
    }

    private void swap(final int i1, final int i2) {
        final float t1 = foms[i1];
        foms[i1] = foms[i2];
        foms[i2] = t1;

        final short t2 = parentIndices[i1];
        parentIndices[i1] = parentIndices[i2];
        parentIndices[i2] = t2;
    }

    public int size() {
        return head >= 0 ? tail - head + 1 : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(1024);
        if (head >= 0) {
            for (int i = head; i < tail; i++) {
                if (grammar != null) {
                    sb.append(String.format("%s %.3f\n", grammar.mapNonterminal(parentIndices[i]), foms[i]));
                } else {
                    sb.append(String.format("%d %.3f\n", parentIndices[i], foms[i]));
                }
            }
        }
        return sb.toString();
    }
}
