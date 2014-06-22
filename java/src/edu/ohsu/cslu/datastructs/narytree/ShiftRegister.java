/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.datastructs.narytree;

import java.util.Arrays;

/**
 * TODO Pull up shift, compareTo, etc. methods from implementing classes
 * 
 * @author Aaron Dunlop
 * @since Jun 27, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class ShiftRegister<E> implements Comparable<ShiftRegister<E>> {
    private final E[] register;

    private int hashCode;

    /**
     * Construct a shift-register.
     * 
     * @param register Initial register contents
     */
    public ShiftRegister(final E[] register) {
        this.register = register;
    }

    /**
     * Construct a shift-register initialized with all nulls.
     * 
     * @param size register size
     */
    @SuppressWarnings("unchecked")
    public ShiftRegister(final int size) {
        register = (E[]) new Object[size];
        Arrays.fill(register, null);
    }

    /**
     * Construct a shift-register initialized with the supplied original register, shifted left, and appending the
     * supplied new character.
     * 
     * @param size register size
     */
    @SuppressWarnings("unchecked")
    private ShiftRegister(final E[] original, final E newValue) {
        register = (E[]) new Object[original.length];
        System.arraycopy(original, 1, register, 0, register.length - 1);
        register[register.length - 1] = newValue;
    }

    /**
     * Returns a new shift-register, shifted left and appending the new value supplied
     * 
     * @param newValue The new value to enqueue
     */
    public final ShiftRegister<E> shift(final E newValue) {
        return new ShiftRegister<E>(register, newValue);
    }

    /**
     * Returns a new shift-register, shifted left and appending a null value
     */
    public final ShiftRegister<E> shift() {
        return shift(null);
    }

    /**
     * @return current register contents
     */
    public final E[] register() {
        return register;
    }

    /**
     * @return size of the register
     */
    public final int size() {
        return register.length;
    }

    public final ShiftRegister<E> concat(final ShiftRegister<E> r) {
        final ShiftRegister<E> newRegister = new ShiftRegister<E>(register.length + r.register.length);
        System.arraycopy(register, 0, newRegister.register, 0, register.length);
        System.arraycopy(r.register, 0, newRegister.register, register.length, r.register.length);
        return newRegister;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof ShiftRegister<?>)) {
            return false;
        }

        return Arrays.equals(register, ((ShiftRegister<?>) obj).register);
    }

    @Override
    public final int hashCode() {
        if (hashCode == 0) {
            hashCode = Arrays.hashCode(register);
        }
        return hashCode;
    }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder(register.length * 4);
        sb.append('(');
        for (int i = 0; i < register.length; i++) {
            sb.append(register[i].toString());

            if (i < register.length - 1) {
                sb.append(',');
            }
        }
        sb.append(')');
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public final int compareTo(final ShiftRegister<E> obj) {
        if (this == obj) {
            return 0;
        }

        final int length = register.length;
        final int oLength = obj.register.length;

        if (length > oLength) {
            return 1;
        }

        if (oLength > length) {
            return -1;
        }

        for (int i = 0; i < length; i++) {
            final int c = ((Comparable<E>) register[i]).compareTo(obj.register[i]);

            if (c > 0) {
                return 1;
            }

            if (c < 0) {
                return -1;
            }
        }

        return 0;
    }

}
