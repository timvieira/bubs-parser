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

import org.cjunit.FilteredRunner;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link DenseFloatVector}.
 * 
 * @author Aaron Dunlop
 * @since Dec 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestDenseFloatVector extends FloatVectorTestCase<DenseFloatVector> {

    @Override
    public void setUp() throws Exception {
        final float[] sampleArray = new float[] { -11, 0, 11, 22, 33, 44, 56, 67, 78, 89, 100 };
        sampleVector = create(sampleArray);

        final StringBuilder sb = new StringBuilder();
        sb.append("vector type=" + serializedName() + " length=11 sparse="
                + ((sampleVector instanceof SparseVector) ? "true\n" : "false\n"));
        sb.append("-11.000000 0.000000 11.000000 22.000000 33.000000 44.000000 56.000000 67.000000 78.000000 89.000000 100.000000\n");
        stringSampleVector = sb.toString();
    }

    @Override
    protected String serializedName() {
        return "float";
    }
}
