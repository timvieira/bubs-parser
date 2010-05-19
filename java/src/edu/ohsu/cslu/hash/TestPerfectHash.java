package edu.ohsu.cslu.hash;


public class TestPerfectHash extends TestHash {

    @Override
    protected ImmutableInt2IntHash hash(final int[] keys, final int modulus) {
        return modulus == 0 ? new PerfectHash(keys) : new PerfectHash(keys, modulus);
    }
}
