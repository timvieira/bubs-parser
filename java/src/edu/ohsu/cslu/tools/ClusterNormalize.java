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

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;

import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.util.Strings;

/**
 * Transforms a corpus using clusters from a Weka ARFF-format file. Assumes the following fields will be present in the
 * .arff file:
 * 
 * <pre>
 * SentenceIndex - field 2 (1-indexed)
 * WordIndex - field 3
 * OccurrenceCount - field 4
 * Cluster-ID - last field
 * </pre>
 * 
 * Replaces any children of specified tags occurring less than the specified thresholds with &lt;UNK-ClusterID&gt; or
 * &lt;UNK-ClusterID&gt;|&lt;Token&gt; (see {@link BaseTextNormalizationTool#tags} and
 * {@link BaseTextNormalizationTool#thresholds}).
 * 
 * @author Aaron Dunlop
 */
public class ClusterNormalize extends BaseTextNormalizationTool {

    @Option(name = "-rt", usage = "Retain the token (replace with UNK-<ClusterID>|<Token> instead of just UNK-<ClusterID>")
    protected boolean retainToken;

    @Option(name = "-c", required = true, metaVar = "file", usage = "ARFF-format cluster file")
    protected File arffClusterFile;

    @Override
    protected void run() throws Exception {

        // TODO Share some of the copy-and-paste code with DecisionTreeNormalize (if it doesn't evolve too much)
        // Read the entire corpus and count token occurrences
        final BufferedReader br = inputAsBufferedReader();
        // Allow re-reading up to 50 MB
        br.mark(50 * 1024 * 1024);

        final Object2IntOpenHashMap<String> lexicon = countTokenOccurrences(br);

        //
        // Read in the cluster file and record cluster IDs for all clustered words
        //
        boolean foundData = false;
        int attributeIndex = 0;
        final Object2IntOpenHashMap<String> attributeMap = new Object2IntOpenHashMap<String>();
        final ArrayList<ArrayList<String>> clusterIds = new ArrayList<ArrayList<String>>();
        for (final String arffLine : inputLines(fileAsBufferedReader(arffClusterFile))) {
            // Map @attribute lines until we see '@data'; every line after that represents a word in the corpus
            if (!foundData) {
                if (arffLine.equals("@data")) {
                    foundData = true;
                } else if (arffLine.startsWith("@attribute")) {
                    final String[] split = arffLine.split(" +");
                    attributeMap.put(split[1], attributeIndex++);
                }
                continue;
            }

            final String[] arffAttributes = Strings.splitOn(arffLine, ',', '\'');
            final int sentenceIndex = Integer.parseInt(arffAttributes[attributeMap.getInt("SentenceIndex")]);
            final int wordIndex = Integer.parseInt(arffAttributes[attributeMap.getInt("WordIndex")]);
            ensureSize(clusterIds, sentenceIndex + 1);
            if (clusterIds.get(sentenceIndex) == null) {
                clusterIds.add(sentenceIndex, new ArrayList<String>());
            }
            final ArrayList<String> sentenceClusterIds = clusterIds.get(sentenceIndex);
            ensureSize(sentenceClusterIds, wordIndex + 1);
            sentenceClusterIds.add(wordIndex, arffAttributes[arffAttributes.length - 1]);
        }

        // Reset the reader and reread the corpus, this time applying appropriate normalizations and outputting each
        // tree
        br.reset();

        int sentenceIndex = 0, wordIndex = 0;
        for (final String line : inputLines(br)) {
            final NaryTree<String> tree = NaryTree.read(line, String.class);
            sentenceIndex++;

            final ArrayList<String> sentenceClusterIds = clusterIds.get(sentenceIndex);
            if (sentenceClusterIds != null) {
                // Replace tokens with the appropriate cluster IDs
                wordIndex = 0;
                for (final NaryTree<String> node : tree.leafList()) {
                    wordIndex++;
                    if (sentenceClusterIds.size() <= wordIndex) {
                        break;
                    }
                    final String clusterID = sentenceClusterIds.get(wordIndex);
                    if (clusterID != null && thresholdMap.containsKey(node.parentLabel())
                            && lexicon.getInt(key(node)) <= thresholdMap.getInt(node.parentLabel())) {
                        node.setLabel(retainToken ? "UNK-" + clusterID + "|" + node.label() : "UNK-" + clusterID);
                    }
                }
            }

            System.out.println(tree.toString());
        }
    }

    public static void ensureSize(final ArrayList<?> list, final int size) {
        // Prevent excessive copying while we're adding
        list.ensureCapacity(size);
        while (list.size() < size) {
            list.add(null);
        }
    }

    public static void main(final String[] args) {
        run(args);
    }
}
