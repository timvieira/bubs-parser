package edu.ohsu.cslu.datastructs.vectors;

import java.io.IOException;
import java.io.Writer;

public class SparseFloatVector extends BaseNumericVector
{
    private float defaultValue = 0;

    public SparseFloatVector(final int length)
    {
        super(length);
    }

    /**
     * Initializes all elements to a default value.
     * 
     * @param length The size of the vector
     * @param defaultValue The value assigned to all elements
     */
    public SparseFloatVector(final int length, final float defaultValue)
    {
        super(length);
        this.defaultValue = defaultValue;
    }

    @Override
    public Vector clone()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public float getFloat(final int i)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getInt(final int i)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float infinity()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public float negativeInfinity()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void set(final int i, final int value)
    {
    // TODO Auto-generated method stub

    }

    @Override
    public void set(final int i, final float value)
    {
    // TODO Auto-generated method stub

    }

    @Override
    public void set(final int i, final boolean value)
    {
    // TODO Auto-generated method stub

    }

    @Override
    public void set(final int i, final String newValue)
    {
    // TODO Auto-generated method stub

    }

    @Override
    public Vector subVector(final int i0, final int i1)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void write(final Writer writer) throws IOException
    {
    // TODO Auto-generated method stub

    }

}
