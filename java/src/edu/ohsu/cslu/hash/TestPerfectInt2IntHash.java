package edu.ohsu.cslu.hash;


public class TestPerfectInt2IntHash extends ImmutableInt2IntHashTestCase {

    @Override
    protected ImmutableInt2IntHash hash(final int[] keys, final int modulus) {
        return modulus == 0 ? new PerfectInt2IntHash(keys) : new PerfectInt2IntHash(keys, modulus);
    }
}
