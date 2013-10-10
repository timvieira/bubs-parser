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
import java.io.IOException;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;

/**
 * Base class for corpus normalization tasks. Counts POS tag/token occurrences of open-class preterminals. Most
 * subclasses will replace rare words with normalized forms, although some (e.g.
 * {@link ExtractWekaWordClusteringFeatures}) will extract other information.
 * 
 * @author Aaron Dunlop
 * @since Mar 21, 2013
 */
public abstract class BaseTextNormalizationTool extends BaseCommandlineTool {

    @Option(name = "-t", required = true, metaVar = "tag", separator = ",", usage = "Tag(s) to normalize (replace with their unknown-word class)")
    protected OpenClassTags[] tags;

    @Option(name = "-th", required = true, metaVar = "count", separator = ",", usage = "Normalization threshold(s) - observation count in the corpus")
    protected int[] thresholds;

    protected Object2IntOpenHashMap<String> thresholdMap = new Object2IntOpenHashMap<String>();

    @Override
    protected void setup() throws Exception {
        if (tags.length != thresholds.length) {
            throw new IllegalArgumentException("The number of specified thresholds (" + thresholds.length
                    + ") does not match the number of specified tags (" + tags.length + ")");
        }
        for (int i = 0; i < tags.length; i++) {
            thresholdMap.put(tags[i].name(), thresholds[i]);
        }
    }

    protected Object2IntOpenHashMap<String> countTokenOccurrences(final BufferedReader br) throws IOException {
        final Object2IntOpenHashMap<String> lexicon = new Object2IntOpenHashMap<String>();
        for (final String line : inputLines(br)) {
            final NaryTree<String> tree = NaryTree.read(line.trim(), String.class);
            for (final NaryTree<String> leafNode : tree.leafTraversal()) {
                lexicon.add(key(leafNode), 1);
            }
        }
        return lexicon;
    }

    protected static String key(final NaryTree<String> leafNode) {
        return leafNode.parentLabel() + "|" + leafNode.label();
    }

    public static enum OpenClassTags {
        CD, JJ, NN, NNP, NNPS, NNS, RB, VB, VBD, VBG, VBN, VBP, VBZ;
    }
}
