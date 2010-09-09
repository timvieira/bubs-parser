package edu.ohsu.cslu.hash;

import java.io.Serializable;

public interface ImmutableIntPair2IntHash extends Serializable {
    public int hashcode(int key1, int key2);

    public int unsafeHashcode(int key1, int key2);

    public int key1(final int hashcode);

    public int unsafeKey1(final int hashcode);

    public int key2(final int hashcode);

    public int unsafeKey2(final int hashcode);
}
