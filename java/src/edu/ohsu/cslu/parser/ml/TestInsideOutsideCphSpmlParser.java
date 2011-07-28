package edu.ohsu.cslu.parser.ml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.InsideOutsideCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.SparseMatrixParser;
import edu.ohsu.cslu.parser.chart.InsideOutsideChart;
import edu.ohsu.cslu.tests.JUnit;

public class TestInsideOutsideCphSpmlParser {

    private SparseMatrixParser<InsideOutsideCscSparseMatrixGrammar, InsideOutsideChart> parser;

    @Before
    public void setUp() throws IOException {
        final InsideOutsideCscSparseMatrixGrammar grammar = new InsideOutsideCscSparseMatrixGrammar(
                JUnit.unitTestDataAsReader("grammars/eng.R2.gr.gz"), PerfectIntPairHashPackingFunction.class);
        parser = new InsideOutsideCphSpmlParser(new ParserDriver(), grammar);
    }

    @After
    public void tearDown() {
        if (parser != null) {
            parser.shutdown();
        }
    }

    @AfterClass
    public static void suiteTearDown() {
        GlobalConfigProperties.singleton().clear();
    }

    @Test
    public void testAll() throws IOException {

        final BufferedReader tokenizedReader = new BufferedReader(new InputStreamReader(
                JUnit.unitTestDataAsStream("parsing/wsj.24.tokens.1-20")));

        final BufferedReader parsedReader = new BufferedReader(new InputStreamReader(
                JUnit.unitTestDataAsStream("parsing/wsj.24.parsed.R2.beam.fom.1-20")));

        int i = 1;
        for (String sentence = tokenizedReader.readLine(); sentence != null; sentence = tokenizedReader.readLine()) {
            final String parsedSentence = parsedReader.readLine();
            System.out.println(parser.parseSentence(sentence).binaryParse.toString());
            i++;
        }
    }
}
