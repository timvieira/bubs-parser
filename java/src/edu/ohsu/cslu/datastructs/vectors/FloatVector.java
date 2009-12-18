package edu.ohsu.cslu.datastructs.vectors;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Random;

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

    /**
     * Initializes all elements to a default value.
     * 
     * @param length The size of the vector
     * @param defaultValue The value assigned to all elements
     */
    public FloatVector(final int length, final float defaultValue)
    {
        super(length);
        this.vector = new float[length];
        Arrays.fill(vector, defaultValue);
    }

    /**
     * Initializes all elements to random values between minValue and maxValue
     * 
     * @param length The size of the vector
     * @param minValue The lowest value in the initialization range (inclusive)
     * @param maxValue The highest value in the initialization range (exclusive)
     */
    public FloatVector(final int length, final float minValue, final float maxValue)
    {
        super(length);
        this.vector = new float[length];
        final Random r = new Random();
        for (int i = 0; i < length; i++)
        {
            vector[i] = minValue + r.nextFloat() * (maxValue - minValue);
        }
    }

    public FloatVector(final float[] vector)
    {
        super(vector.length);
        this.vector = vector;
    }

    /** Type-strengthen return-type */
    @Override
    public FloatVector add(final Vector v)
    {
        return (FloatVector) super.add(v);
    }

    /** Type-strengthen return-type */
    @Override
    public FloatVector elementwiseMultiply(final Vector v)
    {
        return (FloatVector) super.elementwiseMultiply(v);
    }

    /**
     * Adds a {@link Vector} to this vector, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param v addend, a vector of the same length.
     * @return a reference to this vector.
     */
    public FloatVector inPlaceAdd(final Vector v)
    {
        // Special-case for SparseBitVector
        if (v instanceof SparseBitVector)
        {
            if (v.length() > length)
            {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final int i : ((SparseBitVector) v).values())
            {
                vector[i] += 1;
            }
            return this;
        }

        // Special-case for MutableSparseBitVector
        if (v instanceof MutableSparseBitVector)
        {
            if (v.length() > length)
            {
                throw new IllegalArgumentException("Vector length mismatch");
            }

            for (final int i : ((MutableSparseBitVector) v).intSet())
            {
                vector[i] += 1;
            }
            return this;
        }

        if (v.length() != length)
        {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        for (int i = 0; i < length; i++)
        {
            vector[i] += v.getFloat(i);
        }
        return this;
    }

    /**
     * Multiplies this {@link Vector} by another {@link Vector}, returning a reference to this
     * vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param v multiplicand, a vector of the same length.
     * @return a reference to this vector.
     */
    public FloatVector inPlaceElementwiseMultiply(final Vector v)
    {
        if (v.length() != length && !(v instanceof SparseBitVector && v.length() <= length))
        {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        // TODO: This could be more efficient for SparseBitVectors

        for (int i = 0; i < length; i++)
        {
            vector[i] *= v.getFloat(i);
        }
        return this;
    }

    /**
     * Divides this {@link Vector} by another {@link Vector}, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param v divisor, a vector of the same length.
     * @return a reference to this vector.
     */
    public FloatVector inPlaceElementwiseDivide(final Vector v)
    {
        if (v.length() != length)
        {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        for (int i = 0; i < length; i++)
        {
            vector[i] /= v.getFloat(i);
        }
        return this;
    }

    /**
     * Adds the provided scalar to this {@link Vector}, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param addend Scalar value.
     * @return a reference to this vector.
     */
    public FloatVector inPlaceScalarAdd(final float addend)
    {
        for (int i = 0; i < length; i++)
        {
            vector[i] += addend;
        }
        return this;
    }

    /**
     * Multiplies this {@link Vector} by the provided scalar, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param multiplicand Scalar value.
     * @return a reference to this vector.
     */
    public FloatVector inPlaceScalarMultiply(final float multiplicand)
    {
        for (int i = 0; i < length; i++)
        {
            vector[i] *= multiplicand;
        }
        return this;
    }

    /**
     * Takes the log of this {@link Vector}, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @return a reference to this vector.
     */
    public FloatVector inPlaceElementwiseLog()
    {
        for (int i = 0; i < length; i++)
        {
            vector[i] = (float) Math.log(vector[i]);
        }
        return this;
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
    public void set(final int i, final boolean value)
    {
        set(i, value ? 1f : 0f);
    }

    @Override
    public void set(final int i, final String newValue)
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
    public FloatVector createIntVector(final int newVectorLength)
    {
        return new FloatVector(newVectorLength);
    }

    @Override
    public FloatVector createFloatVector(final int newVectorLength)
    {
        return new FloatVector(newVectorLength);
    }

    @Override
    public Vector subVector(final int i0, final int i1)
    {
        final float[] newVector = new float[i1 - i0 + 1];
        System.arraycopy(vector, i0, newVector, 0, newVector.length);
        return new FloatVector(newVector);
    }

    /**
     * Performs the standard learning algorithm on the weight vector (w) of a perceptron.
     * 
     * @param x Input vector
     * @param y Expected output
     * @param alpha Learning Rate
     */
    public void perceptronUpdate(final Vector x, final float y, final float alpha)
    {
        // For each j:
        // w(j) = w(j) + alpha(y - f(x))x(j)
        final float f = dotProduct(x);
        for (int j = 0; j < length; j++)
        {
            vector[j] = vector[j] + alpha * (y - f) * x.getFloat(j);
        }
        // TODO Separate training for SparseBitVector, since it can be more efficient
    }

    /**
     * Performs the standard learning algorithm on the weight vector (w) of a perceptron.
     * 
     * TODO More generic version for classes other than SparseBitVector
     * 
     * @param example Input vector
     * @param alpha Learning Rate (positive for positive examples, negative for negative examples)
     */
    public void perceptronUpdate(final SparseBitVector example, final float alpha)
    {
        // For each element j in example:
        // w(j) = w(j) + alpha
        final int[] elements = example.elements;
        for (int j = 0; j < elements.length; j++)
        {
            vector[elements[j]] = vector[elements[j]] - alpha;
        }
    }

    @Override
    public FloatVector clone()
    {
        final float[] newVector = new float[length];
        System.arraycopy(vector, 0, newVector, 0, length);
        return new FloatVector(newVector);
    }

    @Override
    public void write(final Writer writer) throws IOException
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
