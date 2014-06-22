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
package edu.ohsu.cslu.counters;

import java.util.HashMap;

public class SimpleCounterSet<E> {
    public HashMap<E, SimpleCounter<E>> items;

    public SimpleCounterSet() {
        items = new HashMap<E, SimpleCounter<E>>();
    }

    public void increment(final E numerator, final E denominator) {
        increment(numerator, denominator, 1);
    }

    public void increment(final E numerator, final E denominator, final float amount) {
        if (items.containsKey(denominator) == false) {
            items.put(denominator, new SimpleCounter<E>());
        }
        items.get(denominator).increment(numerator, amount);
        // System.out.println("inc: " + numerator + " | " + denominator + "  adding " + amount);
    }

    public float getCount(final E numerator, final E denominator) {
        if (items.containsKey(denominator) == false) {
            return 0; // should we smooth these too?
        }
        return items.get(denominator).getCount(numerator);
    }

    public float getProb(final E numerator, final E denominator) {
        if (items.containsKey(denominator) == false) {
            return 0;
        }
        return items.get(denominator).getProb(numerator);
    }

    public void smoothAddConst(final float constToAdd, final int numTotalElements) {
        for (final SimpleCounter<E> counter : items.values()) {
            counter.smoothAddConst(constToAdd, numTotalElements);
        }
    }

    public void smoothAddConst(final double constToAdd, final int numTotalElements) {
        smoothAddConst((float) constToAdd, numTotalElements);
    }
}
