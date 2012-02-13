/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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

package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.lela.ConstrainingChart;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.PackedArrayChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.TemporaryChartCell;

/**
 * Produces a parse tree constrained by the gold input tree. The resulting parse will be identical to the gold input
 * tree, but split categories will be populated. E.g., NP_12 might be populated in place of NP.
 * 
 * Implementation notes: This implementation is quite simple and does not optimize efficiency. Binary rules are
 * processed normally (populating non-terminals other than those observed in the gold tree). Mismatched non-terminals
 * are removed after unary processing (see {@link #unaryAndPruning(PackedArrayChartCell, short, short)}.
 * 
 * TODO Switch to using constrained cell selector
 * 
 * @author Aaron Dunlop
 */
public class ConstrainedCphSpmlParser extends CartesianProductHashSpmlParser {

    private ConstrainingChart constrainingChart;

    final LeftCscSparseMatrixGrammar baseGrammar;

    /**
     * @param opts
     * @param grammar
     */
    public ConstrainedCphSpmlParser(final ParserDriver opts, final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
        this.beamWidth = lexicalRowBeamWidth = grammar.numNonTerms();
        baseGrammar = (LeftCscSparseMatrixGrammar) grammar.toUnsplitGrammar();
    }

    @Override
    protected void initSentence(final ParseTask parseTask) {
        super.initSentence(parseTask);
        constrainingChart = new ConstrainingChart(parseTask.inputTree.binarize(grammar.grammarFormat,
                Binarization.valueOf(grammar.binarization)), baseGrammar);
        System.out.println(constrainingChart.toString());
        constrainingChart.extractBestParse(13);
    }

    /**
     * Processes unary rules if the gold tree contains one or more unaries in the current cell. Removes any
     * non-terminals which do not match the unsplit categories from the gold tree.
     */
    @Override
    protected void unaryAndPruning(final PackedArrayChartCell spvChartCell, final short start, final short end) {

        // Perform normal unary processing
        final TemporaryChartCell tmpCell = spvChartCell.tmpCell;
        unaryAndPruning(tmpCell, beamWidth, start, end);

        // Remove any non-terminals populated in tmpCell that do not match the constraining cell
        final PackedArrayChartCell constrainingCell = constrainingChart.getCell(start, end);

        // TODO This check could be considerably more efficient
        for (short nt = 0; nt < tmpCell.insideProbabilities.length; nt++) {
            final String sNt = grammar.nonTermSet.getSymbol(nt);
            final short baseNt = grammar.nonTermSet.getBaseIndex(nt);
            final String sBaseNt = grammar.nonTermSet.baseVocabulary().getSymbol(baseNt);

            if (constrainingCell.getInside(baseNt) == Float.NEGATIVE_INFINITY
                    || constrainingCell.getMidpoint(baseNt) != tmpCell.midpoints[nt]) {
                tmpCell.insideProbabilities[nt] = Float.NEGATIVE_INFINITY;
            }
        }

        spvChartCell.finalizeCell();
    }

    @Override
    protected boolean implicitPruning() {
        return true;
    }
}
