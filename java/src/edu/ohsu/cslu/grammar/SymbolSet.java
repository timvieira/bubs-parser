package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;
import java.util.Iterator;

public class SymbolSet<E> implements Iterable<E> {

    private ArrayList<E> symbolVector;
    private Object2IntOpenHashMap<E> symbolHash;
    private boolean finalized;

    public SymbolSet() {
        symbolVector = new ArrayList<E>();
        symbolHash = new Object2IntOpenHashMap<E>();
        symbolHash.defaultReturnValue(-1);
        finalized = false;
    }

    // get integer index of label string. If it does not exist then
    // add it to the internal structures
    public int getIndex(final E label) {
        int index = symbolHash.getInt(label);
        if (index != -1) {
            return index;
        }

        if (finalized) {
            throw new RuntimeException("ERROR: SymbolSet is finalized but trying to add symbol: " + label);
        }

        index = symbolVector.size();
        symbolHash.put(label, index);
        symbolVector.add(label);

        return index;
    }

    public int addSymbol(final E symbol) throws Exception {
        return this.getIndex(symbol);
    }

    public boolean hasSymbol(final E label) {
        return symbolHash.containsKey(label);
    }

    public E getSymbol(final int index) {
        return symbolVector.get(index);
    }

    public int numSymbols() {
        return symbolVector.size();
    }

    @Override
    public Iterator<E> iterator() {
        return symbolVector.iterator();
    }

    public int size() {
        return symbolVector.size();
    }

    @Override
    public void finalize() {
        finalized = true;
    }

    public void unfinalize() {
        finalized = false;
    }
}
