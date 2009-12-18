package edu.ohsu.cslu.datastructs.narytree;

import java.util.Arrays;

/**
 * Implements a simple shift register of fixed size. Character elements are inserted at the end of the
 * register, shifting current contents leftward. The concept is similar to that of a queue, and is described
 * in [Augsten, Bohlen, Gamper 2005] Implemented in order to support their pq-Grams algorithm.
 * 
 * This implementation is specifically intended for testing; in particular, it should make testing the
 * examples from the Augsten et al paper easier.
 * 
 * '*' is used as a marker for 'unset' cells in the register.
 * 
 * @author Aaron Dunlop
 * @since Sep 19, 2008
 * 
 *        $Id$
 */

public class CharShiftRegister implements ShiftRegister, Comparable<CharShiftRegister> {

    private final char[] register;

    private final static char NULL_VALUE = '*';

    private int hashCode;

    /**
     * Construct a shift-register.
     * 
     * @param register
     *            Initial register contents
     */
    public CharShiftRegister(final char[] register) {
        this.register = register;
    }

    /**
     * Construct a shift-register initialized with all nulls.
     * 
     * @param size
     *            register size
     */
    public CharShiftRegister(final int size) {
        register = new char[size];
        Arrays.fill(register, NULL_VALUE);
    }

    /**
     * Construct a shift-register initialized with the supplied original register, shifted left, and appending
     * the supplied new character.
     * 
     * @param size
     *            register size
     */
    private CharShiftRegister(final char[] original, final char newValue) {
        register = new char[original.length];
        System.arraycopy(original, 1, register, 0, register.length - 1);
        register[register.length - 1] = newValue;
    }

    /**
     * Returns a new shift-register, shifted left and appending the new character supplied
     * 
     * @param newValue
     *            The new value to enqueue
     */
    public CharShiftRegister shift(final char newValue) {
        return new CharShiftRegister(register, newValue);
    }

    /**
     * Returns a new shift-register, shifted left and appending a null value
     */
    public CharShiftRegister shift() {
        return shift(NULL_VALUE);
    }

    /**
     * @return current register contents
     */
    public char[] register() {
        return register;
    }

    /**
     * @return size of the register
     */
    public int size() {
        return register.length;
    }

    public CharShiftRegister concat(CharShiftRegister r) {
        CharShiftRegister newRegister = new CharShiftRegister(register.length + r.register.length);
        System.arraycopy(register, 0, newRegister.register, 0, register.length);
        System.arraycopy(r.register, 0, newRegister.register, register.length, r.register.length);
        return newRegister;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CharShiftRegister)) {
            return false;
        }
        return Arrays.equals(register, ((CharShiftRegister) obj).register);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = Arrays.hashCode(register);
        }
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(register.length * 4);
        sb.append('(');
        for (int i = 0; i < register.length; i++) {
            sb.append(register[i]);
            if (i < register.length - 1) {
                sb.append(',');
            }
        }
        sb.append(')');
        return sb.toString();
    }

    @Override
    public int compareTo(CharShiftRegister o) {
        final int length = register.length;
        final int oLength = o.register.length;

        if (length > oLength) {
            return 1;
        }

        if (oLength > length) {
            return -1;
        }

        for (int i = 0; i < length; i++) {
            if (register[i] > o.register[i]) {
                return 1;
            }

            if (register[i] < o.register[i]) {
                return -1;
            }
        }

        return 0;
    }
}
