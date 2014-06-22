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
package edu.ohsu.cslu.hash;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;

import cltool4j.BaseCommandlineTool;

public class CreatePerfectIntPair2IntHash extends BaseCommandlineTool {

    @Override
    protected void run() throws Exception {
        final HashSet<String> lines = new HashSet<String>();
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            lines.add(line);
        }

        final int[][] keyPairs = new int[2][lines.size()];
        int i = 0;
        for (final String line : lines) {
            final String[] split = line.split(",");
            keyPairs[0][i] = Integer.parseInt(split[0]);
            keyPairs[1][i++] = Integer.parseInt(split[1]);
        }

        final ImmutableIntPair2IntHash hash = new SegmentedPerfectIntPair2IntHash(keyPairs);

        System.out.println(hash.toString());
    }

    public static void main(final String[] args) {
        run(args);
    }

}
