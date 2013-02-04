/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.datastructs.vectors;

import it.unimi.dsi.fastutil.longs.LongRBTreeSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.IOException;
import java.io.Writer;

import edu.ohsu.cslu.util.Math;

/**
 * Represents a vector of integer values of limited range The represented vector is packed into an int array for compact
 * storage.
 * 
 * Currently, only powers-of-2 are supported for bit-lengths (1, 2, 4, 8, 16, and 32 bits). Of course, bit-lengths of 8,
 * 16, and 32 would generally be better represented using the native byte, short, and int types.
 * 
 * @author Aaron Dunlop
 * @since Nov 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class PackedIntVector extends BaseNumericVector implements IntVector {

    private static final long serialVersionUID = 1L;

    // These 5 fields and constants must be modified to convert storage from int to long.
    // Preliminary profiling on a 32-bit machine indicates a speed hit of ~30-35%, but that might not be the
    // case on 64-bit hardware.

    /** Packed storage */
    private final int[] packedVector;

    /** Bit length of each element */
    private final int bits;

    /** Bit length of the data structure used to store packed elements */
    private final static int STORAGE_BIT_LENGTH = 32;

    /** log base 2 of STORAGE_BIT_LENGTH */
    private final static int ELEMENT_BIT_LENGTH_POWER = 5;

    /** In 2's-complement arithmetic, -1 will have all bits set */
    private final static int ALL_BITS_SET = -1;

    /** Maximum value allowed in a packed element */
    private final int maxValue;

    private final int indexShift;
    private final int indexMask;
    private final int shiftShift;

    public PackedIntVector(final long length, final int bits) {
        super(length);
        packedVector = new int[(int) (length * bits / STORAGE_BIT_LENGTH) + 1];

        this.bits = bits;
        this.maxValue = ~(ALL_BITS_SET << bits);

        // There must be a more efficient way to do this, but it's in the constructor, so this isn't
        // too bad
        switch (bits) {
        case 1:
            this.shiftShift = 0;
            break;
        case 2:
            this.shiftShift = 1;
            break;
        case 4:
            this.shiftShift = 2;
            break;
        case 8:
            this.shiftShift = 3;
            break;
        case 16:
            this.shiftShift = 4;
            break;
        case 32:
            this.shiftShift = 5;
            break;
        default:
            throw new IllegalArgumentException("Packed Arrays must consist of a power-of-2 <= 32 bits");
        }

        this.indexShift = ELEMENT_BIT_LENGTH_POWER - shiftShift;

        // Mask we'll shift around and & with specified values
        this.indexMask = STORAGE_BIT_LENGTH / bits - 1;
    }

    public PackedIntVector(final int[] vector, final int bits) {
        this(vector.length, bits);

        for (int i = 0; i < length; i++) {
            set(i, vector[i]);
        }
    }

    public PackedIntVector(final int[] vector) {
        this(vector, Math.nextPowerOf2(Math.logBase2(Math.nextPowerOf2(Math.max(vector) + 1))));
    }

    @Override
    public NumericVector add(final Vector v) {
        if (v instanceof PackedIntVector && v.length() == length && ((PackedIntVector) v).bits == bits) {
            final NumericVector newVector = new PackedIntVector(length, bits);

            for (int i = 0; i < length; i++) {
                newVector.set(i, getFloat(i) + v.getFloat(i));
            }

            return newVector;
        }

        return super.add(v);
    }

    /**
     * Sets a value at the specified location
     * 
     * @param index
     * @param value
     */
    public final void set(final int index, final int value) {
        if (value < 0 || value > maxValue) {
            throw new IllegalArgumentException("Illegal value: " + value);
        }

        if (index > length || index < 0) {
            throw new IllegalArgumentException("Illegal index: " + index);
        }

        final int arrayIndex = index >> indexShift;
        // index % (ELEMENT_BIT_LENGTH / bits) * # of bits
        final int shift = (index & indexMask) << shiftShift;
        packedVector[arrayIndex] = packedVector[arrayIndex] & ~(maxValue << shift) | ((value & maxValue) << shift);
    }

    /**
     * Returns the value stored at the specified location
     * 
     * @param index
     * @return value at the specified location
     */
    public final int getInt(final int index) {
        if (index > length || index < 0) {
            throw new IllegalArgumentException("Illegal index: " + index);
        }

        final int arrayIndex = index >> indexShift;
        final int shift = (index & indexMask) << shiftShift;
        return ((packedVector[arrayIndex] >> shift) & maxValue);
    }

    @Override
    public float getFloat(final int i) {
        return getInt(i);
    }

    @Override
    public void set(final int i, final float value) {
        set(i, java.lang.Math.round(value));
    }

    @Override
    public void set(final int i, final boolean value) {
        set(i, value ? 1 : 0);
    }

    @Override
    public void set(final int i, final String newValue) {
        set(i, Integer.parseInt(newValue));
    }

    @Override
    public LongSet populatedDimensions() {
        final LongSet d = new LongRBTreeSet();
        for (int i = 0; i < length; i++) {
            if (getBoolean(i)) {
                d.add(i);
            }
        }
        return d;
    }

    @Override
    public float infinity() {
        return maxValue;
    }

    @Override
    public float negativeInfinity() {
        return 0;
    }

    @Override
    public PackedIntVector scalarMultiply(final int multiplier) {
        final int[] newVector = new int[(int) length];
        for (int i = 0; i < length; i++) {
            newVector[i] = getInt(i) * multiplier;
        }
        return new PackedIntVector(newVector);
    }

    @Override
    public Vector elementwiseMultiply(final Vector v) {
        if (v instanceof FloatVector) {
            return super.elementwiseMultiply(v);
        }

        if (v.length() != length) {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        // Multiplying by an IntVector or BitVector should return a new PackedIntVector
        final int[] newVector = new int[(int) length];
        for (int i = 0; i < length; i++) {
            newVector[i] = getInt(i) * v.getInt(i);
        }
        return new PackedIntVector(newVector);
    }

    @Override
    public Vector subVector(final int i0, final int i1) {
        final int subVectorLength = i1 - i0 + 1;
        final PackedIntVector subVector = new PackedIntVector(subVectorLength, bits);
        for (int i = 0; i < subVectorLength; i++) {
            subVector.set(i, getInt(i0 + i));
        }
        return subVector;
    }

    @Override
    protected NumericVector createIntVector(final long newVectorLength) {
        return new PackedIntVector(newVectorLength, bits);
    }

    @Override
    protected NumericVector createFloatVector(final long newVectorLength) {
        return new DenseFloatVector(newVectorLength);
    }

    @Override
    public PackedIntVector clone() {
        final PackedIntVector v = new PackedIntVector(length, bits);
        System.arraycopy(packedVector, 0, v.packedVector, 0, packedVector.length);
        return v;
    }

    @Override
    public void write(final Writer writer) throws IOException {
        write(writer, String.format("vector type=packed-int length=%d bits=%d sparse=false\n", length, bits));
    }
}
