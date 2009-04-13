package edu.ohsu.cslu.datastructs.vectors;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * A {@link Vector} implementation which stores 32-bit floats. For most NLP tasks requiring
 * floating-point numbers, a 32-bit float is sufficient, and requires half the memory of a 64-bit
 * double.
 * 
 * @author Aaron Dunlop
 * @since Dec 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class FloatVector extends BaseNumericVector
{
    private final static long serialVersionUID = 379752896212698724L;

    private final float[] vector;

    public FloatVector(final int length)
    {
        super(length);
        this.vector = new float[length];
    }

    public FloatVector(final int length, float defaultValue)
    {
        super(length);
        this.vector = new float[length];
        Arrays.fill(vector, defaultValue);
    }

    public FloatVector(final float[] vector)
    {
        super(vector.length);
        this.vector = vector;
    }

    /** Type-strengthen return-type */
    @Override
    public FloatVector add(Vector v)
    {
        return (FloatVector) super.add(v);
    }

    /** Type-strengthen return-type */
    @Override
    public FloatVector elementwiseMultiply(Vector v)
    {
        return (FloatVector) super.elementwiseMultiply(v);
    }

    @Override
    public final float getFloat(final int i)
    {
        return vector[i];
    }

    @Override
    public final int getInt(final int i)
    {
        return Math.round(vector[i]);
    }

    @Override
    public void set(final int i, final int value)
    {
        vector[i] = value;
    }

    @Override
    public void set(final int i, final float value)
    {
        vector[i] = value;
    }

    @Override
    public void set(final int i, boolean value)
    {
        set(i, value ? 1f : 0f);
    }

    @Override
    public void set(final int i, String newValue)
    {
        set(i, Float.parseFloat(newValue));
    }

    @Override
    public final float infinity()
    {
        return Float.POSITIVE_INFINITY;
    }

    @Override
    public final float negativeInfinity()
    {
        return Float.NEGATIVE_INFINITY;
    }

    @Override
    public Vector createIntVector()
    {
        return new FloatVector(length);
    }

    @Override
    public Vector subVector(final int i0, final int i1)
    {
        final float[] newVector = new float[i1 - i0 + 1];
        System.arraycopy(vector, i0, newVector, 0, newVector.length);
        return new FloatVector(newVector);
    }

    @Override
    public Vector clone()
    {
        final float[] newVector = new float[length];
        System.arraycopy(vector, 0, newVector, 0, length);
        return new FloatVector(newVector);
    }

    @Override
    public void write(Writer writer) throws IOException
    {
        writer.write(String.format("vector type=float length=%d\n", length));

        // Write Vector contents
        for (int i = 0; i < length - 1; i++)
        {
            writer.write(String.format("%f ", getFloat(i)));
        }
        writer.write(String.format("%f\n", getFloat(length - 1)));
        writer.flush();
    }
}
