package edu.ohsu.cslu.hash;

public class TestBasicIntPair2IntHash extends ImmutableIntPair2IntHashTestCase {

    @Override
    protected ImmutableIntPair2IntHash hash(final int[][] keyPairs, final int modulus) {
        return new BasicIntPair2IntHash(keyPairs);
    }

}
