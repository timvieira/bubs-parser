package edu.ohsu.cslu.math.linear;

public abstract class BaseNumericVector extends BaseVector implements NumericVector
{
    BaseNumericVector(final int length)
    {
        super(length);
    }

    @Override
    public FloatVector elementwiseDivide(NumericVector v)
    {
        // TODO: The return-types of various Vector class's elementwiseMultiply methods are somewhat
        // inconsistent. Think through the appropriate return-types for add(Vector),
        // elementwiseMultiply, and elementwiseDivide.

        if (v.length() != length)
        {
            throw new IllegalArgumentException("Vector length mismatch");
        }

        FloatVector newVector = new FloatVector(length);
        for (int i = 0; i < length; i++)
        {
            newVector.set(i, getFloat(i) / v.getFloat(i));
        }

        return newVector;
    }

    public FloatVector elementwiseLog()
    {
        FloatVector newVector = new FloatVector(length);
        for (int i = 0; i < length; i++)
        {
            newVector.set(i, (float) Math.log(getFloat(i)));
        }

        return newVector;
    }
}
