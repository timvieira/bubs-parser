package edu.ohsu.cslu.datastructs.narytree;

import java.util.Arrays;

/**
 * Implements a simple shift register of fixed size. Integer elements are inserted at the end of the
 * register, shifting current contents leftward. The concept is similar to that of a queue, and is
 * described in [Augsten, Bohlen, Gamper 2005] Implemented in order to support their pq-Grams
 * algorithm.
 * 
 * Integer.MIN_VALUE is used as a marker for 'unset' cells in the register.
 * 
 * @author Aaron Dunlop
 * @since Sep 19, 2008
 * 
 *        $Id$
 */
public final class IntShiftRegister implements ShiftRegister, Comparable<IntShiftRegister>
{
    private final int[] register;

    private final static int NULL_VALUE = Integer.MIN_VALUE;

    private int hashCode;

    /**
     * Construct a shift-register.
     * 
     * @param register Initial register contents
     */
    public IntShiftRegister(final int[] register)
    {
        this.register = register;
    }

    /**
     * Construct a shift-register initialized with all nulls.
     * 
     * @param size register size
     */
    public IntShiftRegister(final int size)
    {
        register = new int[size];
        Arrays.fill(register, NULL_VALUE);
    }

    /**
     * Construct a shift-register initialized with the supplied original register, shifted left, and
     * appending the supplied new character.
     * 
     * @param size register size
     */
    private IntShiftRegister(final int[] original, final int newValue)
    {
        register = new int[original.length];
        System.arraycopy(original, 1, register, 0, register.length - 1);
        register[register.length - 1] = newValue;
    }

    /**
     * Returns a new shift-register, shifted left and appending the new integer supplied
     * 
     * @param newValue The new value to enqueue
     */
    public final IntShiftRegister shift(final int newValue)
    {
        return new IntShiftRegister(register, newValue);
    }

    /**
     * Returns a new shift-register, shifted left and appending a null value
     */
    public final IntShiftRegister shift()
    {
        return shift(NULL_VALUE);
    }

    /**
     * @return current register contents
     */
    public final int[] register()
    {
        return register;
    }

    /**
     * @return size of the register
     */
    public final int size()
    {
        return register.length;
    }

    public final IntShiftRegister concat(IntShiftRegister r)
    {
        IntShiftRegister newRegister = new IntShiftRegister(register.length + r.register.length);
        System.arraycopy(register, 0, newRegister.register, 0, register.length);
        System.arraycopy(r.register, 0, newRegister.register, register.length, r.register.length);
        return newRegister;
    }

    @Override
    public final boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof IntShiftRegister))
        {
            return false;
        }

        return Arrays.equals(register, ((IntShiftRegister) obj).register);
    }

    @Override
    public final int hashCode()
    {
        if (hashCode == 0)
        {
            hashCode = Arrays.hashCode(register);
        }
        return hashCode;
    }

    @Override
    public final String toString()
    {
        StringBuilder sb = new StringBuilder(register.length * 4);
        sb.append('(');
        for (int i = 0; i < register.length; i++)
        {
            int j = register[i];
            if (j >= ' ' && j <= '~')
            {
                sb.append((char) j);
            }
            else
            {
                sb.append(Integer.toString(j));
            }

            if (i < register.length - 1)
            {
                sb.append(',');
            }
        }
        sb.append(')');
        return sb.toString();
    }

    @Override
    public final int compareTo(final IntShiftRegister obj)
    {
        if (this == obj)
        {
            return 0;
        }

        final int length = register.length;
        final int oLength = obj.register.length;

        if (length > oLength)
        {
            return 1;
        }

        if (oLength > length)
        {
            return -1;
        }

        for (int i = 0; i < length; i++)
        {
            if (register[i] > obj.register[i])
            {
                return 1;
            }

            if (register[i] < obj.register[i])
            {
                return -1;
            }
        }

        return 0;
    }
}
