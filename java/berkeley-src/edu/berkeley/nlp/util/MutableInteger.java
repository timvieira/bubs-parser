package edu.berkeley.nlp.util;

/**
 * A class for Integer objects that you can change.
 * 
 * TODO This will be obsoleted when we replace Indexer with SymbolSet
 * 
 * @author Dan Klein
 */
public final class MutableInteger extends Number implements Comparable<MutableInteger> {

    private int i;

    // Mutable
    public void set(final int i) {
        this.i = i;
    }

    @Override
    public int hashCode() {
        return i;
    }

    /**
     * Compares this object to the specified object. The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is an <code>MutableInteger</code> object that contains the same <code>int</code> value as
     * this object. Note that a MutableInteger isn't and can't be equal to an Integer.
     * 
     * @param obj the object to compare with.
     * @return <code>true</code> if the objects are the same; <code>false</code> otherwise.
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof MutableInteger) {
            return i == ((MutableInteger) obj).i;
        }
        return false;
    }

    @Override
    public String toString() {
        return Integer.toString(i);
    }

    // Comparable interface

    /**
     * Compares two <code>MutableInteger</code> objects numerically.
     * 
     * @param anotherMutableInteger the <code>MutableInteger</code> to be compared.
     * @return Tthe value <code>0</code> if this <code>MutableInteger</code> is equal to the argument
     *         <code>MutableInteger</code>; a value less than <code>0</code> if this <code>MutableInteger</code> is
     *         numerically less than the argument <code>MutableInteger</code>; and a value greater than <code>0</code>
     *         if this <code>MutableInteger</code> is numerically greater than the argument <code>MutableInteger</code>
     *         (signed comparison).
     */
    public int compareTo(final MutableInteger anotherMutableInteger) {
        final int thisVal = this.i;
        final int anotherVal = anotherMutableInteger.i;
        return (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
    }

    // Number interface
    @Override
    public int intValue() {
        return i;
    }

    @Override
    public long longValue() {
        return i;
    }

    @Override
    public short shortValue() {
        return (short) i;
    }

    @Override
    public byte byteValue() {
        return (byte) i;
    }

    @Override
    public float floatValue() {
        return i;
    }

    @Override
    public double doubleValue() {
        return i;
    }

    public MutableInteger() {
        this(0);
    }

    public MutableInteger(final int i) {
        this.i = i;
    }

    private static final long serialVersionUID = 624465615824626762L;
}
