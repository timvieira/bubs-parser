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
package edu.ohsu.cslu.datastructs.vectors;

public abstract class SparseIntVectorTestCase<V extends IntVector> extends IntVectorTestCase<V> {

    @Override
    public void setUp() throws Exception {
        final int[] sampleArray = new int[] { -11, 0, 11, 22, 33, 44, 56, 67, 78, 89, 100 };
        sampleVector = create(sampleArray);

        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=" + serializedName() + " length=11 sparse="
                + ((sampleVector instanceof SparseVector) ? "true\n" : "false\n"));
        sb.append("0 -11 1 0 2 11 3 22 4 33 5 44 6 56 7 67 8 78 9 89 10 100\n");
        stringSampleVector = sb.toString();
    }

}
