package edu.ohsu.cslu.datastructs.vectors;

import edu.ohsu.cslu.datastructs.matrices.Matrix;

public abstract class BaseNumericVector extends BaseVector implements NumericVector
{
    BaseNumericVector(final int length)
    {
        super(length);
    }

    @Override
    public FloatVector elementwiseDivide(final NumericVector v)
    {
        // TODO: The return-types of various Vector class's elementwiseMultiply methods are somewhat
        // inconsistent. Think through the appropriate return-types for add(Vector),
        // elementwiseMultiply, and elementwiseDivide.

        if (v.length() != length)
        {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        final FloatVector newVector = new FloatVector(length);
        for (int i = 0; i < length; i++)
        {
            newVector.set(i, getFloat(i) / v.getFloat(i));
        }

        return newVector;
    }

    public FloatVector elementwiseLog()
    {
        final FloatVector newVector = new FloatVector(length);
        for (int i = 0; i < length; i++)
        {
            newVector.set(i, (float) Math.log(getFloat(i)));
        }

        return newVector;
    }

    public NumericVector multiply(final Matrix m)
    {
        // Relatively inefficient implementation. Could be overridden more efficiently in child
        // classes.

        final int columns = m.columns();
        if (columns != length)
        {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        final int rows = m.rows();

        NumericVector newVector;
        final Class<?> vClass = getClass();
        if (vClass == IntVector.class || vClass == PackedIntVector.class)
        {
            newVector = createIntVector(rows);
        }
        else
        {
            newVector = createFloatVector(rows);
        }

        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < columns; j++)
            {
                newVector.set(i, newVector.getFloat(i) + getFloat(j) * m.getFloat(i, j));
            }
        }

        return newVector;
    }
}
