/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */

package edu.ohsu.cslu.tools;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;

/**
 * Matches and prints input trees matching specific selected tree features.
 * 
 * @author Aaron Dunlop
 * @since Oct 16, 2012
 */
public class TreeGrep extends BaseCommandlineTool {

    // TODO We could include these options, but regular grep works fine
    // @Option(name="-p", metaVar="pos", usage="Non-terminal(s) occurring as preterminals (parts of speech)")
    // private String[] pos;
    //
    // @Option(name="-l", metaVar="token", usage="Word(s) occurring as leaves")
    // private String[] leafWords;

    @Option(name = "-ucl", metaVar = "length", usage = "Unary chain of length >= n")
    private int unaryChainLength;

    @Option(name = "-mcr", usage = "Match trees containing a root node with multiple children")
    private boolean multiChildRoot;

    @Option(name = "-ml", aliases={"--min-length"}, metaVar = "words", usage = "Minimum sentence length")
    private int minLength = 0;

    @Option(name = "-xl", aliases={"--max-length"}, metaVar = "words", usage = "Maximum sentence length")
    private int maxLength = 500;

    @Option(name = "-pd", aliases={"--parent-degree"}, metaVar = "children", usage = "Match trees containing at least one parent with >= n children")
    private int parentDegree = 0;

    @Override
    protected void run() throws Exception {
        line: for (final String line : inputLines()) {
            final NaryTree<String> tree = NaryTree.read(line, String.class);

            if (tree.leaves() > maxLength) {
                continue line;
            }

            if (tree.leaves() < minLength) {
                continue line;
            }

            if (multiChildRoot) {
                // Check for multi-child root
                if (tree.children().size() < 2) {
                    continue line;
                }
            }

            if (parentDegree > 0) {
                int maxChildren = 0;
                for (final NaryTree<String> node : tree.inOrderTraversal()) {
                    if (node.children().size() > maxChildren) {
                        maxChildren = node.children().size();
                    }
                }
                if (maxChildren < parentDegree) {
                    continue line;
                }
            }

            ucl: if (unaryChainLength > 0) {
                // Iterate through the tree looking for a unary chain of length n
                for (final NaryTree<String> node : tree.inOrderTraversal()) {
                    // unaryChainHeight() does not count the current node, so compare to unaryChainLength - 1
                    if (node.unaryChainHeight() >= unaryChainLength - 1) {
                        // We found a matching chain - break out of this loop
                        break ucl;
                    }
                }
                continue line;
            }

            // All conditions matched - print the tree
            System.out.println(line);
        }
    }

    public static void main(final String[] args) {
        run(args);
    }

}
