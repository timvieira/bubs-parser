package edu.ohsu.cslu.hash;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class BasicInt2IntHash implements ImmutableInt2IntHash {
    private final Int2IntOpenHashMap hash;

    public BasicInt2IntHash(final int[] keys) {
        hash = new Int2IntOpenHashMap();
        for (final int key : keys) {
            hash.put(key, key);
        }
        hash.defaultReturnValue(Integer.MIN_VALUE);
    }

    @Override
    public int hashcode(final int key) {
        return hash.get(key);
    }
}
