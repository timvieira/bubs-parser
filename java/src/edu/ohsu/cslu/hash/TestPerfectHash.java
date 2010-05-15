package edu.ohsu.cslu.hash;

import static org.junit.Assert.fail;

import org.junit.experimental.theories.Theory;

public class TestPerfectHash extends TestHash {

    @Override
    protected ImmutableInt2IntHash hash(final int[] keys) {
        return new PerfectHash(keys);
    }

    @Theory
    public void testHashcode(final int[] keys) throws Exception {
        fail("Not Implemented");
    }
}
