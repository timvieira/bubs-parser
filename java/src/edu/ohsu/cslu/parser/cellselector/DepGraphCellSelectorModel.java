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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.vectors.DenseIntVector;
import edu.ohsu.cslu.datastructs.vectors.PackedBitVector;
import edu.ohsu.cslu.dep.DependencyGraph;
import edu.ohsu.cslu.dep.DependencyNode;
import edu.ohsu.cslu.dep.TransitionDepParser;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

/**
 * @author Aaron Dunlop
 * @since Jun 6, 2012
 */
public class DepGraphCellSelectorModel implements CellSelectorModel {

    private static final long serialVersionUID = 1L;

    /**
     * Minimum (simulated) probability at which a subtree will be included in chart pruning
     */
    public final static String OPT_SUBTREE_PROBABILITY = "depSubtreeProb";

    private final TransitionDepParser depParser;

    public DepGraphCellSelectorModel(final TransitionDepParser depParser) {
        this.depParser = depParser;
    }

    public DepGraphCellSelectorModel(final InputStream modelInputStream) throws IOException, ClassNotFoundException {
        final ObjectInputStream ois = new ObjectInputStream(modelInputStream);
        this.depParser = (TransitionDepParser) ois.readObject();
        ois.close();
    }

    public CellSelector createCellSelector() {
        return new DepGraphCellSelector();
    }

    public class DepGraphCellSelector extends CellSelector {

        private PackedBitVector openCellsVector;
        private int sentenceLength;

        /**
         * The minimum score at which a dependency subtree will be used to prune the constituent chart. Note: the score
         * of a subtree is generally a function of the scores of its children. If a subtree rooted at a particular node
         * does not meet this threshold, children (or grandchildren) of that node may. Stored in the
         * {@link CellSelector} instance instead of the model so that it can be changed at runtime even if the model was
         * serialized.
         */
        private final float subtreeScoreThreshold;

        public DepGraphCellSelector() {
            this.subtreeScoreThreshold = (float) Math.log(GlobalConfigProperties.singleton().getFloatProperty(
                    OPT_SUBTREE_PROBABILITY, 0.9f));
        }

        @Override
        public void initSentence(final ChartParser<?, ?> p) {
            super.initSentence(p);
            sentenceLength = p.chart.size();
            cellIndices = openCells(p.chart.parseTask);
            openCells = cellIndices.length / 2;
        }

        public short[] openCells(final ParseTask task) {

            final DependencyGraph graph = new DependencyGraph(task.sentence.split("\\s+"), task.stringInputTags);
            depParser.parse(graph);
            sentenceLength = graph.size() - 1;

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
                    final int subtreeStart = subtree.label().start();
                    final int subtreeEnd = subtreeStart + subtreeSpan;

                    final int eEnd = subtreeEnd - 1;
                    final int sEnd = eEnd - subtreeSpan;

                    for (int e = eEnd; e > subtreeEnd - subtreeSpan; e--) {
                        for (int s = sEnd; s >= 0; s--) {
                            openCellFlags[ParallelArrayChart.cellIndex(s, e, sentenceLength)] = false;
                        }
                    }

                    // Mark cells underneath this cell with a maximum span. Later cells must consider this cell as
                    // a potential child, but need not consider its children.
                    maxSpan = new DenseIntVector((sentenceLength + 1) * sentenceLength / 2, Short.MAX_VALUE);
                    for (int start = subtreeStart; start < subtreeEnd; start++) {
                        for (int end = start + 1; end < subtreeEnd; end++) {
                            maxSpan.set(Chart.cellIndex(start, end, sentenceLength), subtreeSpan);
                        }
                    }
                }
            }

            int openCellCount = 0;
            for (int i = 0; i < openCellFlags.length; i++) {
                if (openCellFlags[i]) {
                    openCellCount++;
                }
            }
            final short[] openCellIndices = new short[openCellCount * 2];
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

        @Override
        public boolean isCellOpen(final short start, final short end) {
            return openCellsVector.getBoolean(ParallelArrayChart.cellIndex(start, end, sentenceLength));
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
    }
}
