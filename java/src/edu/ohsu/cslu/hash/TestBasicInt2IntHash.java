package edu.ohsu.cslu.hash;

public class TestBasicInt2IntHash extends ImmutableInt2IntHashTestCase {

    @Override
    protected ImmutableInt2IntHash hash(final int[] keys, final int modulus) {
        return new BasicInt2IntHash(keys);
    }
}
