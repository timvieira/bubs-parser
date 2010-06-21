package edu.ohsu.cslu.hash;

public interface ImmutableShortPair2IntHash {
    public int hashcode(short key1, short key2);

    public int unsafeHashcode(short key1, short key2);

    public short key1(final int hashcode);

    public short unsafeKey1(final int hashcode);

    public short key2(final int hashcode);

    public short unsafeKey2(final int hashcode);
}
