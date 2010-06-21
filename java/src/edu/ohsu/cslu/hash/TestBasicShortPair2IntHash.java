package edu.ohsu.cslu.hash;

public class TestBasicShortPair2IntHash extends ImmutableShortPair2IntHashTestCase {

    @Override
    protected ImmutableShortPair2IntHash hash(final short[][] keyPairs, final int modulus) {
        return new BasicShortPair2IntHash(keyPairs);
    }

}
