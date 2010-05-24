package edu.ohsu.cslu.hash;

public class TestPerfectIntPair2IntHash extends ImmutableIntPair2IntHashTestCase {

    @Override
    protected ImmutableIntPair2IntHash hash(final int[][] keyPairs, final int modulus) {
        return new PerfectIntPair2IntHash(keyPairs, modulus);
    }

}
