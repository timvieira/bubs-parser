package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.ArrayList;

public class SymbolSet {

    private ArrayList<String> symbolVector;
    private Object2IntOpenHashMap<String> symbolHash;

    public SymbolSet() {
        symbolVector = new ArrayList<String>();
        symbolHash = new Object2IntOpenHashMap<String>();
        symbolHash.defaultReturnValue(-1);
    }

    // get integer index of label string. If it does not exist then
    // add it to the internal structures
    public int getIndex(final String label) {
        int index = symbolHash.getInt(label);
        if (index != -1) {
            return index;
        }

        index = symbolVector.size();
        symbolHash.put(label, index);
        symbolVector.add(label);
        return index;
    }

    public boolean hasLabel(final String label) {
        return symbolHash.containsKey(label);
    }

    public String getString(final int index) {
        return symbolVector.get(index);
    }

    public int numSymbols() {
        return symbolVector.size();
    }

}
