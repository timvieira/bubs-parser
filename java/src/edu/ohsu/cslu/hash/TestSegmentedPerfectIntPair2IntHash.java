package edu.ohsu.cslu.hash;

public class TestSegmentedPerfectIntPair2IntHash extends ImmutableIntPair2IntHashTestCase {

    @Override
    protected ImmutableIntPair2IntHash hash(final int[][] keyPairs, final int modulus) {
        return new SegmentedPerfectIntPair2IntHash(keyPairs, modulus);
    }

}
