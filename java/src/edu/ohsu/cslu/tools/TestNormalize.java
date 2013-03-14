package edu.ohsu.cslu.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cltool4j.ToolTestCase;

public class TestNormalize extends ToolTestCase {

    @Test
    public void testNNP() throws Exception {

        final StringBuilder input = new StringBuilder();
        input.append("(ROOT (S (NP (NNP AAPL)) (VP (VBD rose) (PP (IN on) (DT the) (NN news))) (. .)))\n");
        input.append("(ROOT (NP (: --) (NNP Rollin) (NNP S.) (NNP Trexler) (. .)))\n");

        final StringBuilder expectedOutput = new StringBuilder();
        expectedOutput.append("(ROOT (S (NP (NNP UNK-CAPS)) (VP (VBD rose) (PP (IN on) (DT the) (NN news))) (. .)))\n");
        expectedOutput.append("(ROOT (NP (: --) (NNP UNK-CAPS) (NNP UNK-CAPS) (NNP UNK-CAPS-er) (. .)))\n");

        final String output = executeTool(new Normalize(), "-t NNP -th 1", input.toString());
        assertEquals(expectedOutput.toString(), output);
    }
}
