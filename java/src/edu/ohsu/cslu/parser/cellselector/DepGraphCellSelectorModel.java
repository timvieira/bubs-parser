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

package edu.ohsu.cslu.parser.cellselector;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.dep.DependencyGraph;
import edu.ohsu.cslu.dep.DependencyNode;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

/**
 * @author Aaron Dunlop
 * @since Jun 6, 2012
 */
public class DepGraphCellSelectorModel implements CellSelectorModel {

    /**
     * Configuration property key for the default confidence score assigned to each arc when reading a dependency input
     * file. Used to perform testing over a wide range of confidence values and simulate real-world confidence variance.
     */
    public final static String OPT_DEFAULT_ARC_CONFIDENCE = "depArcConfidence";

    /**
     * Fraction of dependency arcs to sample randomly
     */
    public final static String OPT_ARC_FRACTION = "depArcFraction";

    private HashMap<String, DependencyGraph> map = new HashMap<String, DependencyGraph>();

    /**
     * The minimum score at which a dependency subtree will be used to prune the constituent chart. Note: the score of a
     * subtree is generally a function of the scores of its children. If a subtree rooted at a particular node does not
     * meet this threshold, children (or grandchildren) of that node may.
     */
    private float subtreeScoreThreshold;

    /**
     * The fraction of arcs to randomly skip
     */
    private float arcSkipSampleFraction;

    private DepGraphCellSelectorModel() {
        subtreeScoreThreshold = (float) Math.log(GlobalConfigProperties.singleton().getFloatProperty("depTreeScore",
                0.9f));
        arcSkipSampleFraction = 1f - GlobalConfigProperties.singleton().getFloatProperty(OPT_ARC_FRACTION, 1f);
    }

    public DepGraphCellSelectorModel(final List<DependencyGraph> goldGraphs) {
        this();

        for (final DependencyGraph g : goldGraphs) {
            map.put(g.tokenizedSentence(), g);
        }
    }

    public DepGraphCellSelectorModel(final Reader goldGraphs) throws IOException {
        this();

        final float confidence = GlobalConfigProperties.singleton().getFloatProperty(OPT_DEFAULT_ARC_CONFIDENCE, 0.95f);
        final BufferedReader br = new BufferedReader(goldGraphs);
        for (DependencyGraph g = DependencyGraph.readConll(br); g != null; g = DependencyGraph.readConll(br)) {
            g.setConfidenceScores(confidence);

            // If we're randomly skipping some arcs, set their scores to 0
            if (arcSkipSampleFraction > 0) {
                final int skipCount = Math.round(g.size() * arcSkipSampleFraction);
                final IntSet arcsToSkip = new IntOpenHashSet();
                final Random r = new Random();
                while (arcsToSkip.size() < skipCount) {
                    arcsToSkip.add(r.nextInt(g.size()));
                }
                for (final int i : arcsToSkip) {
                    g.setConfidenceScore(i, 0);
                }
            }

            map.put(g.tokenizedSentence(), g);
        }
    }

    public CellSelector createCellSelector() {
        return new DepGraphCellSelector();
    }

    public short[] openCells(final String sentence) {

        final DependencyGraph graph = map.get(sentence);
        final int sentenceLength = graph.size() - 1;
        final int currentOpenCells = (sentenceLength) * (sentenceLength + 1) / 2;
        final boolean[] openCellFlags = new boolean[currentOpenCells];
        Arrays.fill(openCellFlags, true);

        final NaryTree<DependencyNode> tree = graph.tree();
        for (final NaryTree<DependencyNode> subtree : tree.preOrderTraversal()) {

            if (subtree.label().span() > 1 && subtree.label().subtreeScore() > subtreeScoreThreshold) {

                // Mark cells which would cross brackets with this cell as closed
                final short subtreeSpan = subtree.label().span();

                // Traverse rightward up the closed diagonals
                final int sStart = subtree.label().start() + 1;
                final int eStart = sStart + subtreeSpan;

                for (int s = sStart; s < sStart + subtreeSpan - 1; s++) {
                    for (int e = eStart; e <= sentenceLength; e++) {
                        openCellFlags[ParallelArrayChart.cellIndex(s, e, sentenceLength)] = false;
                    }
                }

                // Traverse rightward up the closed diagonals
                final int subtreeEnd = subtree.label().start() + subtreeSpan;

                final int eEnd = subtreeEnd - 1;
                final int sEnd = eEnd - subtreeSpan;

                for (int e = eEnd; e > subtreeEnd - subtreeSpan; e--) {
                    for (int s = sEnd; s >= 0; s--) {
                        openCellFlags[ParallelArrayChart.cellIndex(s, e, sentenceLength)] = false;
                    }
                }

                // TODO Mark cells underneath this cell with a maximum span. Later cells must consider this cell as
                // a potential child, but need not consider its children.
            }
        }
        int openCells = 0;
        for (int i = 0; i < openCellFlags.length; i++) {
            if (openCellFlags[i]) {
                openCells++;
            }
        }
        final short[] openCellIndices = new short[openCells * 2];
        for (int span = 1, i = 0; span <= sentenceLength; span++) {
            for (short start = 0; start < sentenceLength - span + 1; start++) {
                if (openCellFlags[PackedArrayChart.cellIndex(start, start + span, sentenceLength)]) {
                    openCellIndices[i++] = start;
                    openCellIndices[i++] = (short) (start + span);
                }
            }
        }
        return openCellIndices;
    }

    public class DepGraphCellSelector extends CellConstraints {

        private boolean[] openCellsArray;
        private int sentenceLength;

        @Override
        public void initSentence(final ChartParser<?, ?> p) {
            super.initSentence(p);
            cellIndices = openCells(p.chart.parseTask.sentence);
            openCells = cellIndices.length / 2;
            sentenceLength = p.chart.size();
        }

        @Override
        public boolean isCellOpen(final short start, final short end) {
            return openCellsArray[ParallelArrayChart.cellIndex(start, end, sentenceLength)];
        }

        @Override
        public boolean isCellOnlyFactored(final short start, final short end) {
            // Dependency graph constraints are strictly binary, and do not allow incomplete constituents in otherwise
            // closed cells
            return false;
        }

        @Override
        public boolean isUnaryOpen(final short start, final short end) {
            return true;
        }

        @Override
        protected boolean isGrammarLeftFactored() {
            return false;
        }
    }
}
