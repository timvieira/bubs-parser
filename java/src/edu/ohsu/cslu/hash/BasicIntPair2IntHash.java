package edu.ohsu.cslu.hash;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class BasicIntPair2IntHash implements ImmutableIntPair2IntHash {

    // TODO Vary the shift

    private final Int2IntOpenHashMap hash;

    public BasicIntPair2IntHash(final int[][] keyPairs) {
        hash = new Int2IntOpenHashMap();
        for (int i = 0; i < keyPairs[0].length; i++) {
            final int packedKey = keyPairs[0][i] << 16 | keyPairs[1][i];
            hash.put(packedKey, packedKey);
        }
        hash.defaultReturnValue(Integer.MIN_VALUE);
    }

    @Override
    public int hashcode(final int key1, final int key2) {
        return unsafeHashcode(key1, key2);
    }

    @Override
    public int unsafeHashcode(final int key1, final int key2) {
        return hash.get(key1 << 16 | key2);
    }

    @Override
    public int key1(final int hashcode) {
        return unsafeKey1(hashcode);
    }

    @Override
    public int unsafeKey1(final int hashcode) {
        final int value = hash.get(hashcode);
        return value < 0 ? Integer.MIN_VALUE : value >> 16;
    }

    @Override
    public int key2(final int hashcode) {
        return unsafeKey2(hashcode);
    }

    @Override
    public int unsafeKey2(final int hashcode) {
        final int value = hash.get(hashcode);
        return value < 0 ? Integer.MIN_VALUE : value & 0xffff;
    }

}
