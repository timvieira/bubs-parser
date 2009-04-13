package edu.ohsu.cslu.datastructs.vectors;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * A {@link Vector} implementation which stores 32-bit ints.
 * 
 * @author Aaron Dunlop
 * @since Dec 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class IntVector extends BaseNumericVector
{
    private final static long serialVersionUID = 379752896212698724L;

    private final int[] vector;

    public IntVector(int length)
    {
        super(length);
        this.vector = new int[length];
    }

    public IntVector(int length, int defaultValue)
    {
        super(length);
        this.vector = new int[length];
        Arrays.fill(this.vector, defaultValue);
    }

    public IntVector(int[] vector)
    {
        super(vector.length);
        this.vector = vector;
    }

    @Override
    public final float getFloat(final int i)
    {
        return vector[i];
    }

    @Override
    public final int getInt(final int i)
    {
        return vector[i];
    }

    @Override
    public void set(final int i, final int value)
    {
        vector[i] = value;
    }

    @Override
    public void set(final int i, final float value)
    {
        set(i, Math.round(value));
    }

    @Override
    public void set(final int i, boolean value)
    {
        set(i, value ? 1 : 0);
    }

    @Override
    public void set(final int i, String newValue)
    {
        set(i, Float.parseFloat(newValue));
    }

    @Override
    public final float infinity()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public final float negativeInfinity()
    {
        return Integer.MIN_VALUE;
    }

    @Override
    public Vector subVector(final int i0, final int i1)
    {
        final int[] newVector = new int[i1 - i0 + 1];
        System.arraycopy(vector, i0, newVector, 0, newVector.length);
        return new IntVector(newVector);
    }

    @Override
    public Vector clone()
    {
        final int[] newVector = new int[length];
        System.arraycopy(vector, 0, newVector, 0, length);
        return new IntVector(newVector);
    }

    @Override
    public void write(Writer writer) throws IOException
    {
        write(writer, String.format("vector type=int length=%d\n", length));
    }
}
