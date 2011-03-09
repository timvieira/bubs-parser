package edu.ohsu.cslu.parser.ml;

import java.io.Reader;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.RightShiftFunction;
import edu.ohsu.cslu.tests.PerformanceTest;

public class TestRightChildLoopSpmlParser extends SparseMatrixLoopParserTestCase<RightChildLoopSpmlParser> {

    @Override
    public Grammar createGrammar(final Reader grammarReader) throws Exception {
        return grammarClass().getConstructor(new Class[] { Reader.class, Class.class }).newInstance(
                new Object[] { grammarReader, RightShiftFunction.class });
    }

    @Override
    @Test
    @PerformanceTest({ "mbp", "7714", "d820", "10000" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }
}
