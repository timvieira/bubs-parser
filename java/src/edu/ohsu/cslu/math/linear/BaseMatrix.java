package edu.ohsu.cslu.math.linear;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Basic functionality required by all classes implementing the {@link Matrix} interface.
 * 
 * @author Aaron Dunlop
 * @since Sep 30, 2008
 * 
 *        $Id$
 */
public abstract class BaseMatrix implements Matrix, Serializable
{
    /** Number of rows */
    protected final int m;

    /** Number of columns */
    protected final int n;

    /**
     * Is this matrix symmetric? (Symmetric matrices are reflected across the diagonal, and can thus
     * be stored in 1/2 the space)
     */
    protected final boolean symmetric;

    BaseMatrix(final int m, final int n, final boolean symmetric)
    {
        this.m = m;
        this.n = n;
        this.symmetric = symmetric;
    }

    @Override
    public final int rows()
    {
        return m;
    }

    @Override
    public final int columns()
    {
        return n;
    }

    @Override
    public boolean isSquare()
    {
        return m == n;
    }

    @Override
    public boolean isSymmetric()
    {
        return symmetric;
    }

    @Override
    public float[] getRow(final int i)
    {
        float[] row = new float[n];
        for (int j = 0; j < n; j++)
        {
            row[j] = getFloat(i, j);
        }
        return row;
    }

    @Override
    public int[] getIntRow(final int i)
    {
        int[] row = new int[n];
        for (int j = 0; j < n; j++)
        {
            row[j] = getInt(i, j);
        }
        return row;
    }

    @Override
    public float[] getColumn(final int j)
    {
        float[] column = new float[m];
        for (int i = 0; i < m; i++)
        {
            column[i] = getFloat(i, j);
        }
        return column;
    }

    @Override
    public int[] getIntColumn(final int j)
    {
        int[] column = new int[m];
        for (int i = 0; i < m; i++)
        {
            column[i] = getInt(i, j);
        }
        return column;
    }

    @Override
    public void setRow(final int i, final float[] newRow)
    {
        for (int j = 0; j < n; j++)
        {
            set(i, j, newRow[j]);
        }
    }

    @Override
    public void setRow(final int i, final float value)
    {
        for (int j = 0; j < n; j++)
        {
            set(i, j, value);
        }
    }

    @Override
    public void setRow(final int i, final int[] newRow)
    {
        for (int j = 0; j < n; j++)
        {
            set(i, j, newRow[j]);
        }
    }

    @Override
    public void setRow(final int i, final int value)
    {
        for (int j = 0; j < n; j++)
        {
            set(i, j, value);
        }
    }

    @Override
    public void setColumn(final int j, final float[] newColumn)
    {
        for (int i = 0; i < m; i++)
        {
            set(i, j, newColumn[i]);
        }
    }

    @Override
    public void setColumn(final int j, final float value)
    {
        for (int i = 0; i < m; i++)
        {
            set(i, j, value);
        }
    }

    @Override
    public void setColumn(final int j, final int[] newColumn)
    {
        for (int i = 0; i < m; i++)
        {
            set(i, j, newColumn[i]);
        }
    }

    @Override
    public void setColumn(final int j, final int value)
    {
        for (int i = 0; i < m; i++)
        {
            set(i, j, value);
        }
    }

    @Override
    public float max()
    {
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < m; i++)
        {
            final int jBound = symmetric ? i + 1 : n;
            for (int j = 0; j < jBound; j++)
            {
                final float x = getFloat(i, j);
                if (x > max)
                {
                    max = x;
                }
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
    public int[] argMax()
    {
        int maxI = 0, maxJ = 0;
        float max = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < m; i++)
        {
            final int jBound = symmetric ? i + 1 : n;
            for (int j = 0; j < jBound; j++)
            {
                final float x = getFloat(i, j);
                if (x > max)
                {
                    max = x;
                    maxI = i;
                    maxJ = j;
                }
            }
        }
        return new int[] {maxI, maxJ};
    }

    @Override
    public int rowArgMax(int i)
    {
        int maxJ = 0;
        float max = Float.NEGATIVE_INFINITY;

        for (int j = 0; j < n; j++)
        {
            final float x = getFloat(i, j);
            if (x > max)
            {
                max = x;
                maxJ = j;
            }
        }
        return maxJ;
    }

    @Override
    public float min()
    {
        float min = Float.POSITIVE_INFINITY;

        for (int i = 0; i < m; i++)
        {
            final int jBound = symmetric ? i + 1 : n;
            for (int j = 0; j < jBound; j++)
            {
                final float x = getFloat(i, j);
                if (x < min)
                {
                    min = x;
                }
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
    public int[] argMin()
    {
        int minI = 0, minJ = 0;
        float min = Float.POSITIVE_INFINITY;

        for (int i = 0; i < m; i++)
        {
            final int jBound = symmetric ? i + 1 : n;
            for (int j = 0; j < jBound; j++)
            {
                final float x = getFloat(i, j);
                if (x < min)
                {
                    min = x;
                    minI = i;
                    minJ = j;
                }
            }
        }
        return new int[] {minI, minJ};
    }

    @Override
    public int rowArgMin(int i)
    {
        int minJ = 0;
        float min = Float.POSITIVE_INFINITY;

        for (int j = 0; j < n; j++)
        {
            final float x = getFloat(i, j);
            if (x < min)
            {
                min = x;
                minJ = j;
            }
        }
        return minJ;
    }

    @Override
    public Matrix scalarAdd(float addend)
    {
        // Relatively inefficient implementation. Could be overridden in subclasses
        Matrix newMatrix = clone();
        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                newMatrix.set(i, j, getFloat(i, j) + addend);
            }
        }
        return newMatrix;
    }

    @Override
    public Matrix scalarAdd(int addend)
    {
        // Relatively inefficient implementation. Could be overridden in subclasses
        Matrix newMatrix = clone();
        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                newMatrix.set(i, j, getFloat(i, j) + addend);
            }
        }
        return newMatrix;
    }

    @Override
    public Matrix scalarMultiply(float multiplier)
    {
        // Relatively inefficient implementation. Could be overridden in subclasses
        Matrix newMatrix = clone();
        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                newMatrix.set(i, j, getFloat(i, j) * multiplier);
            }
        }
        return newMatrix;
    }

    @Override
    public Matrix scalarMultiply(int multiplier)
    {
        // Relatively inefficient implementation. Could be overridden in subclasses
        Matrix newMatrix = clone();
        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                newMatrix.set(i, j, getFloat(i, j) * multiplier);
            }
        }
        return newMatrix;
    }

    @Override
    public float sum()
    {
        float sum = 0f;
        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                sum += getFloat(i, j);
            }
        }
        return sum;
    }

    /**
     * Type-strengthen return-type
     * 
     * @return a copy of this matrix
     */
    @Override
    public abstract Matrix clone();

    protected void write(final Writer writer, final String format) throws IOException
    {
        // Write Matrix contents
        for (int i = 0; i < m; i++)
        {
            final int maxJ = symmetric ? i : n - 1;
            for (int j = 0; j < maxJ; j++)
            {
                writer.write(String.format(format, getFloat(i, j)));
                writer.write(' ');
            }

            // Handle the end-of-line string separately
            String eolString = String.format(format, getFloat(i, maxJ));

            // Trim spaces from the end of the string
            while (eolString.charAt(eolString.length() - 1) == ' ')
            {
                eolString = eolString.substring(0, eolString.length() - 1);
            }

            writer.write(eolString);
            writer.write('\n');
        }
        writer.flush();
    }

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

        Matrix other = (Matrix) o;

        for (int i = 0; i < m; i++)
        {
            final int maxJ = symmetric ? i + 1 : n;
            for (int j = 0; j < maxJ; j++)
            {
                // TODO: Should this use an epsilon comparison instead of an exact float comparison?
                if (getFloat(i, j) != other.getFloat(i, j))
                {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public String toString()
    {
        try
        {
            Writer writer = new StringWriter(m * n * 10);
            write(writer);
            return writer.toString();
        }
        catch (IOException e)
        {
            return "Caught IOException in StringWriter";
        }
    }
}
