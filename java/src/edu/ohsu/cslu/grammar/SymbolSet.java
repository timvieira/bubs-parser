package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class SymbolSet<E> implements Iterable<E>, Serializable {

    private ArrayList<E> symbolVector;
    private Object2IntOpenHashMap<E> symbolHash;
    private boolean finalized;

    public SymbolSet() {
        symbolVector = new ArrayList<E>();
        symbolHash = new Object2IntOpenHashMap<E>();
        symbolHash.defaultReturnValue(-1);
        finalized = false;
    }

    // return index of symbol. If it does not exist, return -1
    public int getIndex(final E symbol) {
        return symbolHash.getInt(symbol);
    }

    // get integer index of label string. If it does not exist then
    // add it to the internal structures
    public int addSymbol(final E symbol) {
        // return this.getIndex(symbol);
        int index = symbolHash.getInt(symbol);
        if (index != -1) {
            return index;
        }

        if (finalized) {
            throw new RuntimeException("ERROR: SymbolSet is finalized but trying to add symbol: " + symbol);
        }

        index = symbolVector.size();
        symbolHash.put(symbol, index);
        symbolVector.add(symbol);

        return index;
    }

    public boolean hasSymbol(final E label) {
        return symbolHash.containsKey(label);
    }

    public E getSymbol(final int index) {
        return symbolVector.get(index);
    }

    public final int numSymbols() {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < symbolVector.size(); i++) {
            sb.append(i + " : " + symbolVector.get(i) + '\n');
        }
        return sb.toString();
    }
}
