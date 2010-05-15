package edu.ohsu.cslu.hash;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;


public class BasicHash implements ImmutableInt2IntHash {
    private final Int2IntOpenHashMap hash;

    public BasicHash(int[] keys) {
        hash = new Int2IntOpenHashMap();
        for (int key : keys) {
            hash.put(key, key);
        }
        hash.defaultReturnValue(-1);
    }

    @Override
    public boolean containsKey(int key) {
        return hash.containsKey(key);
    }

    @Override
    public int hashcode(int key) {
        return hash.get(key);
    }

}
