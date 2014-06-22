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
package edu.ohsu.cslu.parser.cellselector;

import cltool4j.BaseCommandlineTool;

public class TrainCellSelector extends BaseCommandlineTool {

    @Override
    protected void run() throws Exception {
        // Moved to edu.ohsu.cslu.parser.beam.BSCPBeamConfTrain.java
        // In general, the trainers just output features that can then be used to
        // separately train a perceptron/avg perceptron/etc model

    }
    /*
     * // == Parser options == // @Option(name = "-p", aliases = { "--parser" }, metaVar = "parser", usage =
     * "Parser implementation") // public ParserType parserType = ParserType.CKY;
     * 
     * // @Option(name = "-rp", metaVar = "parser", usage = "Research Parser implementation") private ResearchParserType
     * researchParserType = ResearchParserType.ECPCellCrossList;
     * 
     * @Option(name = "-g", required = true, metaVar = "grammar", usage =
     * "Grammar file (text, gzipped text, or binary serialized") private String grammarFile = null;
     * 
     * // @Option(name = "-boundaryFOM", usage = "Train a Boundary Figure of Merit model") // public boolean boundaryFOM
     * = false; // public EdgeSelectorType edgeFOMType = null;
     * 
     * // @Option(name = "-beamConf", usage = "Train Beam Confidence model") // public boolean beamConf = false;
     * 
     * // @Option(name = "-cellConstraints", usage = "Train a Cell Constraints model") // public boolean cellConstraints
     * = false;
     * 
     * public BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out)); public
     * BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in)); private Grammar grammar;
     * 
     * public static void main(final String[] args) { run(args); }
     * 
     * @Override public void run() throws Exception { // To train a BoundaryInOut FOM model we need a grammar and //
     * binarized gold input trees with NTs from same grammar grammar = ParserDriver.readGrammar(grammarFile,
     * researchParserType, null);
     * 
     * // } else if (beamConf == true) { // final ModelTrainer m = new ModelTrainer(); // m.natesTraining(); // // final
     * PerceptronCellSelector perceptronCellSelector = (PerceptronCellSelector) // //
     * CellSelector.create(cellSelectorType, cellModelStream, cslutScoresStream); // // final BSCPPerceptronCellTrainer
     * parser = new BSCPPerceptronCellTrainer(opts, (LeftHashGrammar) // // grammar); // //
     * perceptronCellSelector.train(inputStream, parser); // } else { // System.out.println("ERROR."); // } }
     */

}
