package edu.ohsu.cslu.hash;

public class TestBasicHash extends TestHash {

    @Override
    protected ImmutableInt2IntHash hash(final int[] keys, final int modulus) {
        return new BasicHash(keys);
    }
}
