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

package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import cltool4j.ToolTestCase;

/**
 * Tests {@link ParserDriver} itself, particularly options which cannot be tested outside of the driver class.
 * 
 * @author Aaron Dunlop
 */
public class TestParserDriver extends ToolTestCase {

    private final static String M0_GRAMMAR = "unit-test-data/grammars/eng.R0.gr.gz";

    /**
     * Tests finding and labeling heads using Charniak's ruleset. Parses with a Markov-0 grammar and outputs head rules
     * in the format requested by Ginger Software, Feb 2012.
     * 
     * @throws Exception
     */
    @Test
    public void testHeadRules() throws Exception {
        final StringBuilder input = new StringBuilder(256);
        input.append("The economy 's temperature will be taken from several vantage points this week , with readings on trade , output , housing and inflation .\n");
        input.append("The most troublesome report may be the August merchandise trade deficit due out tomorrow .\n");

        final StringBuilder expectedOutput = new StringBuilder(1024);
        expectedOutput
                .append("(ROOT 0 (S 1 (NP 1 (NP 2 (DT The) (NN economy) (POS 's)) (NN temperature)) (VP 0 (MD will) (VP 1 (VB be) (VBN taken) (PP 0 (IN from) (NP 2 (JJ several) (NN vantage) (NNS points))) (VP 0 (NP 1 (DT this) (NN week)) (, ,) (PP 0 (IN with) (NP 0 (NP 0 (NNS readings)) (PP 0 (IN on) (NP 2 (NP 2 (NP 2 (NP 0 (NN trade)) (, ,) (NN output)) (, ,) (NN housing)) (CC and) (NN inflation)))))))) (. .)))\n");
        expectedOutput
                .append("(ROOT 0 (S 1 (NP 2 (DT The) (ADJP 1 (RBS most) (JJ troublesome)) (NN report)) (VP 0 (MD may) (VP 0 (VB be) (NP 4 (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (ADJP 0 (JJ due) (PP 0 (IN out) (NP 0 (NN tomorrow)))))) (. .)))\n");

        // Using the built-in Charniak rules
        String output = executeTool(new ParserDriver(), "-g " + M0_GRAMMAR
                + " -O normInsideTune=0 -head-rules charniak", input.toString());
        assertEquals(expectedOutput.toString(), treeOutput(output));

        // Using a rule file
        output = executeTool(new ParserDriver(), "-g " + M0_GRAMMAR
                + " -O normInsideTune=0 -head-rules unit-test-data/parsing/charniak.head.rules", input.toString());
        assertEquals(expectedOutput.toString(), treeOutput(output));
    }

    @Test
    public void testRecovery() throws Exception {
        final String input = "(ROOT (SQ (VBZ Is) (NP (NNP Nikon) (NNP low) (NNP end)) (VP (VBG moving) (PP (IN toward) (NP (NP (DT the) (JJ hellish) (NNS ergonomics)) (PP (IN of) (NP (DT the) (NNP Canon) (NNP Rebel)))))) (. ?)))";
        final String output = executeTool(new ParserDriver(), "-g " + M0_GRAMMAR
                + " -rp const -O maxBeamWidth=10 -O normInsideTune=0 -recovery rb", input.toString());
        assertEquals(
                "(ROOT (SQ (VBZ Is) (NP (NNP Nikon) (JJ low) (JJ end)) (VP (VBG moving) (PP (IN toward) (NP (NP (DT the) (JJ hellish) (NNS ergonomics)) (PP (IN of) (NP (DT the) (NNP Canon) (NN Rebel)))))) (. ?)))\n",
                treeOutput(output));
    }

    private String treeOutput(final String output) {
        final StringBuilder treeOutput = new StringBuilder(512);
        for (final String outputLine : output.split("\n")) {
            if (outputLine.startsWith("(")) {
                treeOutput.append(outputLine);
                treeOutput.append('\n');
            }
        }
        return treeOutput.toString();
    }

}
