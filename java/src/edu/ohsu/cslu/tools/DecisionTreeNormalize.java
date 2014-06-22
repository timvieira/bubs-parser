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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;

/**
 * Applies decision-tree text normalizations to a treebank (using {@link DecisionTreeTokenClassifier}).
 * 
 * @author Aaron Dunlop
 */
public class DecisionTreeNormalize extends BaseTextNormalizationTool {

    @Override
    protected void run() throws Exception {

        // Read the entire corpus and count token occurrences
        final BufferedReader br = inputAsBufferedReader();
        // Allow re-reading up to 50 MB
        br.mark(50 * 1024 * 1024);

        final Object2IntOpenHashMap<String> lexicon = countTokenOccurrences(br);

        // Reset the reader and reread the corpus, this time applying appropriate normalizations and outputting each
        // tree
        br.reset();

        for (final String line : inputLines(br)) {
            final NaryTree<String> tree = NaryTree.read(line.trim(), String.class);

            for (final NaryTree<String> node : tree.inOrderTraversal()) {

                if (node.isLeaf() && thresholdMap.containsKey(node.parentLabel())
                        && lexicon.getInt(key(node)) <= thresholdMap.getInt(node.parentLabel())) {
                    node.setLabel(DecisionTreeTokenClassifier.berkeleyGetSignature(node.label(), false, null));
                }
            }

            System.out.println(tree.toString());
        }
    }

    public static void main(final String[] args) {
        run(args);
    }
}
