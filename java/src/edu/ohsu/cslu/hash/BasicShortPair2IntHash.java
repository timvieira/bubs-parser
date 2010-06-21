package edu.ohsu.cslu.hash;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class BasicShortPair2IntHash implements ImmutableShortPair2IntHash {

    // TODO Vary the shift

    private final Int2IntOpenHashMap hash;

    public BasicShortPair2IntHash(final short[][] keyPairs) {
        hash = new Int2IntOpenHashMap();
        for (int i = 0; i < keyPairs[0].length; i++) {
            final int packedKey = keyPairs[0][i] << 16 | keyPairs[1][i];
            hash.put(packedKey, packedKey);
        }
        hash.defaultReturnValue(Integer.MIN_VALUE);
    }

    @Override
    public int hashcode(final short key1, final short key2) {
        return unsafeHashcode(key1, key2);
    }

    @Override
    public int unsafeHashcode(final short key1, final short key2) {
        return hash.get(key1 << 16 | key2);
    }

    @Override
    public short key1(final int hashcode) {
        return unsafeKey1(hashcode);
    }

    @Override
    public short unsafeKey1(final int hashcode) {
        final int value = hash.get(hashcode);
        return value < 0 ? Short.MIN_VALUE : (short) (value >> 16);
    }

    @Override
    public short key2(final int hashcode) {
        return unsafeKey2(hashcode);
    }

    @Override
    public short unsafeKey2(final int hashcode) {
        final int value = hash.get(hashcode);
        return value < 0 ? Short.MIN_VALUE : (short) (value & 0xffff);
    }

}
