package edu.ohsu.cslu.math.linear;

import java.io.IOException;
import java.io.Writer;

/**
 * Represents a vector of integer values of limited range The represented vector is packed into an
 * int array for compact storage.
 * 
 * Currently, only powers-of-2 are supported for bit-lengths (1, 2, 4, 8, 16, and 32 bits). Of
 * course, bit-lengths of 8, 16, and 32 would generally be better represented using the native byte,
 * short, and int types.
 * 
 * @author Aaron Dunlop
 * @since Nov 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class PackedIntVector extends BaseVector implements Vector
{
    // These 5 fields and constants must be modified to convert storage from int to long.
    // Preliminary
    // profiling on a 32-bit machine indicates a speed hit of ~30-35%, but that might not be the
    // case on
    // 64-bit hardware.

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

    private final int capacity;
    private final int indexShift;
    private final int indexMask;
    private final int shiftShift;

    public PackedIntVector(final int length, final int bits)
    {
        super(length);
        packedVector = new int[(length * bits / STORAGE_BIT_LENGTH) + 1];

        this.capacity = length;
        this.bits = bits;
        this.maxValue = ~(ALL_BITS_SET << bits);

        // There must be a more efficient way to do this, but it's in the constructor, so this isn't
        // too bad
        switch (bits)
        {
            case 1 :
                this.shiftShift = 0;
                break;
            case 2 :
                this.shiftShift = 1;
                break;
            case 4 :
                this.shiftShift = 2;
                break;
            case 8 :
                this.shiftShift = 3;
                break;
            case 16 :
                this.shiftShift = 4;
                break;
            case 32 :
                this.shiftShift = 5;
                break;
            default :
                throw new IllegalArgumentException("Packed Arrays must consist of a power-of-2 <= 32 bits");
        }

        this.indexShift = ELEMENT_BIT_LENGTH_POWER - shiftShift;

        // Mask we'll shift around and & with specified values
        this.indexMask = STORAGE_BIT_LENGTH / bits - 1;
    }

    public PackedIntVector(final int[] vector, final int bits)
    {
        this(vector.length, bits);

        for (int i = 0; i < length; i++)
        {
            set(i, vector[i]);
        }
    }

    /**
     * Sets a value at the specified location
     * 
     * @param index
     * @param value
     */
    public final void set(int index, int value)
    {
        if (value < 0 || value > maxValue)
        {
            throw new IllegalArgumentException("Illegal value: " + value);
        }

        if (index > capacity || index < 0)
        {
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
    public final int getInt(int index)
    {
        if (index > capacity || index < 0)
        {
            throw new IllegalArgumentException("Illegal index: " + index);
        }

        final int arrayIndex = index >> indexShift;
        final int shift = (index & indexMask) << shiftShift;
        return ((packedVector[arrayIndex] >> shift) & maxValue);
    }

    @Override
    public float getFloat(int i)
    {
        return getInt(i);
    }

    @Override
    public void set(int i, float value)
    {
        set(i, Math.round(value));
    }

    @Override
    public void set(int i, boolean value)
    {
        set(i, value ? 1 : 0);
    }

    @Override
    public void set(int i, String newValue)
    {
        set(i, Integer.parseInt(newValue));
    }

    @Override
    public float infinity()
    {
        return maxValue;
    }

    @Override
    public float negativeInfinity()
    {
        return 0;
    }

    @Override
    public Vector subVector(int i0, int i1)
    {
        final int subVectorLength = i1 - i0 + 1;
        PackedIntVector subVector = new PackedIntVector(subVectorLength, bits);
        for (int i = 0; i < subVectorLength; i++)
        {
            subVector.set(i, getInt(i0 + i));
        }
        return subVector;
    }

    @Override
    public PackedIntVector clone()
    {
        PackedIntVector v = new PackedIntVector(length, bits);
        System.arraycopy(packedVector, 0, v.packedVector, 0, packedVector.length);
        return v;
    }

    @Override
    public void write(Writer writer) throws IOException
    {
        write(writer, String.format("vector type=packed-int length=%d bits=%d\n", length, bits));
    }
}
