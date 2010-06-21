package edu.ohsu.cslu.hash;

public class TestPerfectShortPair2IntHash extends ImmutableShortPair2IntHashTestCase {

    @Override
    protected ImmutableShortPair2IntHash hash(final short[][] keyPairs, final int modulus) {
        return new PerfectShortPair2IntHash(keyPairs, modulus);
    }

}
