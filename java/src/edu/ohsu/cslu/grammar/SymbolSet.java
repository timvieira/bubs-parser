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
package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

/**
 * Maps objects to integer indices and vice-versa. Wraps an {@link Object2IntMap}, with convenience methods to add
 * mappings and the ability to {@link #finalize()} a map after all symbols have been added.
 */
public class SymbolSet<E> implements Object2IntSortedMap<E>, Iterable<E>, Serializable {

    private static final long serialVersionUID = 1L;

    protected ArrayList<E> list;
    private Object2IntOpenHashMap<E> map;
    private boolean finalized;
    private int defaultReturnValue = -1;

    private Comparator<? super E> comparator;

    public SymbolSet() {
        list = new ArrayList<E>();
        map = new Object2IntOpenHashMap<E>();
        map.defaultReturnValue(defaultReturnValue);
        finalized = false;
    }

    public SymbolSet(final Collection<E> symbols) {
        this();
        int i = 0;
        for (final E symbol : symbols) {
            list.add(symbol);
            map.put(symbol, i++);
        }
    }

    public SymbolSet(final E[] symbols) {
        this();
        for (int i = 0; i < symbols.length; i++) {
            list.add(symbols[i]);
            map.put(symbols[i], i);
        }
    }

    /**
     * Returns the integer index of <code>symbol</code>; if the symbol is not already mapped, it is created and added to
     * the {@link SymbolSet}.
     * 
     * @param symbol
     * @return the integer index of <code>symbol</code>
     */
    public int addSymbol(final E symbol) {
        int index = map.getInt(symbol);
        if (index != defaultReturnValue) {
            return index;
        }

        if (finalized) {
            throw new RuntimeException("Cannot modify a finalized SymbolSet");
        }

        index = list.size();
        map.put(symbol, index);
        list.add(symbol);

        return index;
    }

    @Override
    public int put(final E symbol, final int index) {
        if (finalized) {
            throw new RuntimeException("Cannot modify a finalized SymbolSet");
        }

        if (index >= list.size()) {
            throw new IndexOutOfBoundsException();
        }

        map.remove(symbol);
        list.set(index, symbol);
        return map.put(symbol, index);
    }

    @Override
    public Integer put(final E symbol, final Integer index) {
        return put(symbol, index.intValue());
    }

    @Override
    public void putAll(final Map<? extends E, ? extends Integer> m) {
        for (final E key : m.keySet()) {
            put(key, m.get(key));
        }
    }

    // return index of symbol. If it does not exist, return default return value (generally -1)
    public int getIndex(final E symbol) {
        return map.getInt(symbol);
    }

    @Override
    public Integer get(final Object symbol) {
        return map.get(symbol);
    }

    @Override
    public int getInt(final Object symbol) {
        return map.getInt(symbol);
    }

    public E getSymbol(final int index) {
        return list.get(index);
    }

    @Override
    public boolean containsKey(final Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(final int value) {
        return value < list.size();
    }

    @Override
    public boolean containsValue(final Object value) {
        return ((Integer) value).intValue() < list.size();
    }

    @Override
    public Integer remove(final Object symbol) {
        if (finalized) {
            throw new RuntimeException("Cannot modify a finalized SymbolSet");
        }
        return removeInt(symbol);
    }

    @Override
    public int removeInt(final Object symbol) {
        if (finalized) {
            throw new RuntimeException("Cannot modify a finalized SymbolSet");
        }

        final int index = map.getInt(symbol);
        if (index < 0) {
            return index;
        }

        // Remove the symbol from the list and the map
        map.remove(symbol);
        list.remove(index);

        // Shift mapped indices
        for (int i = index; i < list.size(); i++) {
            map.put(list.get(i), i);
        }
        return index;
    }

    public E removeIndex(final int index) {
        if (finalized) {
            throw new RuntimeException("Cannot modify a finalized SymbolSet");
        }

        if (index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException();
        }

        final E symbol = list.get(index);

        // Remove the symbol from the list and the map
        map.remove(symbol);
        list.remove(index);

        // Shift mapped indices
        for (int i = index; i < list.size(); i++) {
            map.put(list.get(i), i);
        }
        return symbol;
    }

    public void removeIndices(final int[] indices) {
        int j = 0, k = 0;
        for (int i = 0; i < list.size(); i++) {
            if (j < indices.length && i == indices[j]) {
                j++;
            } else {
                list.set(k++, list.get(i));
            }
        }
        for (int i = 0; i < indices.length; i++) {
            list.remove(list.size() - 1);
        }
        map.clear();
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i), i);
        }
    }

    @Override
    public void clear() {
        if (finalized) {
            throw new RuntimeException("Cannot modify a finalized SymbolSet");
        }

        list.clear();
        map.clear();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    public int size() {
        return list.size();
    }

    @Override
    public Iterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public void finalize() {
        finalized = true;
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void unfinalize() {
        finalized = false;
    }

    @Override
    public void defaultReturnValue(final int rv) {
        map.defaultReturnValue(rv);
        this.defaultReturnValue = rv;
    }

    /**
     * Sets the default return value of this {@link SymbolSet} to the mapped integer value of the specified symbol. If
     * the symbol is currently unmapped and the {@link SymbolSet} is not finalized, the symbol is added.
     * 
     * @param symbol
     * @throws IllegalArgumentException if the symbol is unmapped and the {@link SymbolSet} is finalized
     */
    public void defaultReturnValue(final E symbol) {
        if (map.containsKey(symbol)) {
            this.defaultReturnValue = map.getInt(symbol);
            map.defaultReturnValue(defaultReturnValue);
        } else if (!finalized) {
            this.defaultReturnValue = addSymbol(symbol);
            map.defaultReturnValue(defaultReturnValue);
        } else {
            throw new IllegalArgumentException(symbol + " not found in finalized SymbolSet");
        }
    }

    @Override
    public int defaultReturnValue() {
        return defaultReturnValue;
    }

    @Override
    public E firstKey() {
        return list.get(0);
    }

    @Override
    public E lastKey() {
        return list.get(list.size() - 1);
    }

    @Override
    public ObjectSortedSet<java.util.Map.Entry<E, Integer>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectSortedSet<it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<E>> object2IntEntrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectSortedSet<E> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntCollection values() {
        final IntArrayList intList = new IntArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            intList.add(i);
        }
        return intList;
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    public void setComparator(final Comparator<? super E> comparator) {
        this.comparator = comparator;
        resort();
    }

    private void resort() {
        Collections.sort(list, comparator);
        for (int i = 0; i < list.size(); i++) {
            map.put(list.get(i), i);
        }
    }

    @Override
    public Object2IntSortedMap<E> subMap(final E fromKey, final E toKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object2IntSortedMap<E> headMap(final E toKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object2IntSortedMap<E> tailMap(final E fromKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(i + " : " + list.get(i) + '\n');
        }
        return sb.toString();
    }

}
