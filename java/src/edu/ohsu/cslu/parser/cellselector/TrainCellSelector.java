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
