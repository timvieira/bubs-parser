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

package edu.ohsu.cslu.tools;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;

/**
 * @author Aaron Dunlop
 * @since Sep 27, 2012
 */
public class CountNodes extends BaseCommandlineTool {

    @Option(name = "-l", usage = "Exclude leaves")
    public boolean excludeLeaves;

    @Override
    protected void run() throws Exception {
        int nodes = 0;
        for (final String line : inputLines()) {
            final NaryTree<String> tree = NaryTree.read(line, String.class);
            if (!excludeLeaves) {
                nodes += tree.size();
            } else {
                for (final NaryTree<String> node : tree.inOrderTraversal()) {
                    if (!node.isLeaf()) {
                        nodes++;
                    }
                }
            }
        }

        System.out.println(nodes);
    }

    public static void main(final String[] args) {
        run(args);
    }

}
