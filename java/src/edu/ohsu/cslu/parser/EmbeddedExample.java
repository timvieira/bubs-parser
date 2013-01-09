package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.cellselector.PerceptronBeamWidthModel;
import edu.ohsu.cslu.parser.fom.BoundaryInOut;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FOMType;
import edu.ohsu.cslu.parser.spmv.CscSpmvParser;

/**
 * Demonstrates a very simple method of embedding BUBS functionality into user code, including reading in a grammar and
 * pruning model and parsing simple example sentences.
 * 
 * Usage: EmbeddedExample <grammar file> <edge selector model> <cell selector model>
 * 
 * e.g. EmbeddedExample models/eng.sm6.gr.gz models/eng.sm6.fom.gz models/eng.sm6.bcm.gz
 */
public class EmbeddedExample {

    public static void main(final String[] args) throws FileNotFoundException, IOException {

        // The 'ParserDriver' class also serves as the container for parser options
        final ParserDriver opts = new ParserDriver();

        // Instantiate a Grammar class and load in the grammar from disk
        final LeftCscSparseMatrixGrammar grammar = new LeftCscSparseMatrixGrammar(uncompressFile(args[0]));
        opts.setGrammar(grammar);

        // Configure the beam model before we load it from disk
        GlobalConfigProperties.singleton().setProperty("beamModelBias", "200,200,200,200");

        // Create FOMModel and CellSelectorModel instances and load models from disk
        opts.fomModel = new BoundaryInOut(FOMType.BoundaryPOS, grammar, uncompressFile(args[1]));
        opts.cellSelectorModel = new PerceptronBeamWidthModel(uncompressFile(args[2]));

        // Create a Parser instance
        final CscSpmvParser parser = new CscSpmvParser(opts, grammar);

        // Parse example sentences and write output to STDOUT
        ParseTask result = parser
                .parseSentence("The economy 's temperature will be taken from several vantage points this week , with readings on trade , output , housing and inflation .");
        System.out.println(result.parseBracketString(false));

        result = parser
                .parseSentence("The most troublesome report may be the August merchandise trade deficit due out tomorrow .");
        System.out.println(result.parseBracketString(false));
    }

    // Open and uncompress a gzipped file, returning a BufferedReader
    private static BufferedReader uncompressFile(final String filename) throws FileNotFoundException, IOException {
        return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));
    }
}
