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
import edu.ohsu.cslu.datastructs.narytree.CharniakHeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;

/**
 * @author Aaron Dunlop
 * @since Mar 2, 2015
 */
public class FindHeads extends BaseCommandlineTool {

    /**
     * Specifies a ruleset and performs head-finding (thus labeling dependency structure as well as constituency). This
     * head-finding approach is fairly simplistic, but much faster than the more accurate Stanford parser approach.
     */
    @Option(name = "-head-rules", hidden = true, optionalChoiceGroup = "binary", metaVar = "ruleset", usage = "Enables head-finding using a Charniak-style head-finding ruleset. Specify ruleset as 'charniak' or a rule file.")
    private String headRules = "charniak";

    @Override
    protected void run() throws Exception {
        final HeadPercolationRuleset headPercolationRuleset;
        if (headRules.equalsIgnoreCase("charniak")) {
            headPercolationRuleset = new CharniakHeadPercolationRuleset();
        } else {
            headPercolationRuleset = new HeadPercolationRuleset(fileAsBufferedReader(headRules));
        }

        for (final String line : inputLines()) {
            final NaryTree<String> tree = NaryTree.read(line, String.class);

            final String[] leafLabels = tree.leafLabels();
            // Transform each label, including the head to
            for (final NaryTree<String> node : tree.preOrderTraversal()) {
                // Skip leaf and preterminal nodes (for which head-finding doesn't make sense anyway)
                if (node.height() > 2) {
                    node.setLabel(node.label() + "~~~"
                            + leafLabels[headPercolationRuleset.headChild(node.label(), node.childLabels())]);
                }
            }
            System.out.println(tree.toString());
        }
    }

    public static void main(final String[] args) {
        run(args);
    }

}
