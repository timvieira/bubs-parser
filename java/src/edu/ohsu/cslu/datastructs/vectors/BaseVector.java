package edu.ohsu.cslu.datastructs.vectors;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Implements functionality common to all {@link Vector} implementations.
 * 
 * @author Aaron Dunlop
 * @since Apr 1, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class BaseVector implements Vector, Serializable
{
    protected int length;

    BaseVector(final int length)
    {
        this.length = length;
    }

    @Override
    public boolean getBoolean(int i)
    {
        return getInt(i) != 0;
    }

    @Override
    public int length()
    {
        return length;
    }

    @Override
    public float max()
    {
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < length; i++)
        {
            final float x = getFloat(i);
            if (x > max)
            {
                max = x;
            }
        }
        return max;
    }

    @Override
    public int intMax()
    {
        return Math.round(max());
    }

    @Override
    public float min()
    {
        float min = Float.POSITIVE_INFINITY;

        for (int i = 0; i < length; i++)
        {
            final float x = getFloat(i);
            if (x < min)
            {
                min = x;
            }
        }
        return min;
    }

    @Override
    public int intMin()
    {
        return Math.round(min());
    }

    @Override
    public int argMax()
    {
        int maxI = 0;
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < length; i++)
        {
            final float x = getFloat(i);
            if (x > max)
            {
                max = x;
                maxI = i;
            }
        }
        return maxI;
    }

    @Override
    public int argMin()
    {
        int minI = 0;
        float min = Float.POSITIVE_INFINITY;

        for (int i = 0; i < length; i++)
        {
            final float x = getFloat(i);
            if (x < min)
            {
                min = x;
                minI = i;
            }
        }
        return minI;
    }

    public Vector add(Vector v)
    {
        if (v.length() != length && !(v instanceof SparseBitVector && v.length() < length))
        {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        Vector newVector;
        Class<?> vClass = v.getClass();
        if (vClass == IntVector.class || vClass == PackedIntVector.class || v instanceof BitVector)
        {
            newVector = createIntVector();
        }
        else
        {
            newVector = new FloatVector(length);
        }

        for (int i = 0; i < length; i++)
        {
            newVector.set(i, getFloat(i) + v.getFloat(i));
        }

        return newVector;
    }

    @Override
    public Vector scalarAdd(float addend)
    {
        Vector newVector = new FloatVector(length);
        for (int i = 0; i < newVector.length(); i++)
        {
            newVector.set(i, getFloat(i) + addend);
        }
        return newVector;
    }

    @Override
    public Vector scalarAdd(int addend)
    {
        Vector newVector = createIntVector();
        for (int i = 0; i < newVector.length(); i++)
        {
            newVector.set(i, getFloat(i) + addend);
        }
        return newVector;
    }

    public Vector elementwiseMultiply(Vector v)
    {
        // TODO: The return-types of various Vector class's elementwiseMultiply methods are somewhat
        // inconsistent. Think through the appropriate return-types for add(Vector),
        // elementwiseMultiply, and elementwiseDivide.

        if (v.length() != length && !(v instanceof SparseBitVector && v.length() < length))
        {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        Vector newVector;
        Class<?> vClass = v.getClass();
        if (vClass == IntVector.class || vClass == PackedIntVector.class || v instanceof BitVector)
        {
            newVector = createIntVector();
        }
        else
        {
            newVector = new FloatVector(length);
        }

        for (int i = 0; i < length; i++)
        {
            newVector.set(i, getFloat(i) * v.getFloat(i));
        }

        return newVector;
    }

    @Override
    public Vector scalarMultiply(float multiplier)
    {
        Vector newVector = new FloatVector(length);
        for (int i = 0; i < newVector.length(); i++)
        {
            newVector.set(i, getFloat(i) * multiplier);
        }
        return newVector;
    }

    @Override
    public Vector scalarMultiply(int multiplier)
    {
        Vector newVector = createIntVector();
        for (int i = 0; i < newVector.length(); i++)
        {
            newVector.set(i, getFloat(i) * multiplier);
        }
        return newVector;
    }

    /**
     * Creates a new {@link Vector} of the same length and of a type appropriate to return from
     * integer operations ({@link #scalarAdd(int)}, {@link #scalarMultiply(int)}, etc.) This method
     * will be overridden by floating-point {@link Vector} implementations and by
     * {@link PackedIntVector}.
     * 
     * TODO: A better name?
     * 
     * @return Vector
     */
    protected Vector createIntVector()
    {
        return new IntVector(length);
    }

    @Override
    public float dotProduct(Vector v)
    {
        // SparseBitVector dotProduct() implementation is more efficient.
        if (v instanceof SparseBitVector)
        {
            return v.dotProduct(this);
        }

        if (v.length() != length)
        {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        float dotProduct = 0f;
        for (int i = 0; i < length; i++)
        {
            dotProduct += getFloat(i) * v.getFloat(i);
        }
        return dotProduct;
    }

    @Override
    public float sum()
    {
        float sum = 0f;
        for (int i = 0; i < length; i++)
        {
            sum += getFloat(i);
        }
        return sum;
    }

    public void write(Writer writer, String headerLine) throws IOException
    {
        writer.write(headerLine);

        // Write Vector contents
        for (int i = 0; i < length - 1; i++)
        {
            writer.write(String.format("%d ", getInt(i)));
        }
        writer.write(String.format("%d\n", getInt(length - 1)));
        writer.flush();
    }

    /**
     * Type-strengthen return-type
     * 
     * @return a copy of this vector
     */
    @Override
    public abstract Vector clone();

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o.getClass() != this.getClass())
        {
            return false;
        }

        BaseVector other = (BaseVector) o;

        if (other.length != length)
        {
            return false;
        }

        for (int i = 0; i < length; i++)
        {
            // TODO: Should this use an epsilon comparison instead of an exact float comparison?
            if (getFloat(i) != other.getFloat(i))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString()
    {
        try
        {
            Writer writer = new StringWriter(length * 10);
            write(writer);
            return writer.toString();
        }
        catch (IOException e)
        {
            return "Caught IOException in StringWriter";
        }
    }

}
