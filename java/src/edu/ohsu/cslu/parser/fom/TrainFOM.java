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
package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.Parser.ResearchParserType;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.fom.FigureOfMerit.FOMType;
import edu.ohsu.cslu.parser.ml.InsideOutsideCphSpmlParser;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductFunctionType;

public class TrainFOM extends BaseCommandlineTool {

    @Option(name = "-fom", required = true, usage = "FOM to train.  Supports BoundaryInOut,Discriminative")
    private FOMType fomType = null;

    @Option(name = "-g", required = true, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String grammarFile = null;

    @Option(name = "-counts", usage = "Write model counts instead of log probabilities (only BoundaryInOut)")
    public boolean writeCounts = false;

    @Option(name = "-smooth", metaVar = "N", usage = "Apply add-N smoothing to model (only BoundaryInOut)")
    public float smoothingCount = (float) 0.5;

    public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));
    public BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    public void run() throws Exception {
        if (fomType == FOMType.BoundaryInOut) {
            // To train a BoundaryInOut FOM model we need a grammar and
            // binarized gold input trees with NTs from same grammar
            final Grammar grammar = ParserDriver.readGrammar(grammarFile, ResearchParserType.ECPCellCrossList, null);
            final BoundaryInOut fomModel = new BoundaryInOut(FOMType.BoundaryInOut, grammar, null);
            fomModel.train(inputStream, outputStream, smoothingCount, writeCounts);
        } else if (fomType == FOMType.Discriminative) {
            final Grammar grammar = ParserDriver.readGrammar(grammarFile,
                    ResearchParserType.InsideOutsideCartesianProductHash, CartesianProductFunctionType.PerfectHash);
            final ParserDriver opts = new ParserDriver();
            final InsideOutsideCphSpmlParser parser = new InsideOutsideCphSpmlParser(opts,
                    (InsideOutsideCscSparseMatrixGrammar) grammar);

        } else {
            throw new IllegalArgumentException("FOM type '" + fomType + "' not supported.");
        }

        // } else if (beamConf == true) {
        // final ModelTrainer m = new ModelTrainer();
        // m.natesTraining();
        // // final PerceptronCellSelector perceptronCellSelector = (PerceptronCellSelector)
        // // CellSelector.create(cellSelectorType, cellModelStream, cslutScoresStream);
        // // final BSCPPerceptronCellTrainer parser = new BSCPPerceptronCellTrainer(opts, (LeftHashGrammar)
        // // grammar);
        // // perceptronCellSelector.train(inputStream, parser);
        // } else {
        // System.out.println("ERROR.");
        // }
    }

}
