package edu.ohsu.cslu.hash;



public class TestBasicHash extends TestHash {

    @Override
    protected ImmutableInt2IntHash hash(int[] keys) {
        return new BasicHash(keys);
    }
}
