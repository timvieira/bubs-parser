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

import org.cjunit.FilteredRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import cltool4j.ToolTestCase;

/**
 * Tests {@link ParserDriver} itself, particularly options which cannot be tested outside of the driver class.
 * 
 * @author Aaron Dunlop
 */
@RunWith(FilteredRunner.class)
public class TestParserDriver extends ToolTestCase {

    private final static String SIMPLE_GRAMMAR_2 = "unit-test-data/grammars/simple2.txt";
    private final static String M0_GRAMMAR = "unit-test-data/grammars/eng.R0.gr.gz";
    private final static String M2_GRAMMAR = "unit-test-data/grammars/eng.R2.gr.gz";

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
        String output = executeTool(new ParserDriver(), "-g " + M0_GRAMMAR + " -head-rules charniak", input.toString());
        assertEquals(expectedOutput.toString(), treeOutput(output));

        // Using a rule file
        output = executeTool(new ParserDriver(), "-g " + M0_GRAMMAR
                + " -head-rules unit-test-data/parsing/charniak.head.rules", input.toString());
        assertEquals(expectedOutput.toString(), treeOutput(output));
    }

    @Test
    public void testRecovery() throws Exception {
        final String input = "(ROOT (SQ (VBZ Is) (NP (NNP Nikon) (NNP low) (NNP end)) (VP (VBG moving) (PP (IN toward) (NP (NP (DT the) (JJ hellish) (NNS ergonomics)) (PP (IN of) (NP (DT the) (NNP Canon) (NNP Rebel)))))) (. ?)))";
        final String output = executeTool(new ParserDriver(), "-g " + M0_GRAMMAR
                + " -rp const -O maxBeamWidth=2 -recovery rb", input.toString());
        assertEquals(
                "(ROOT (SQ (VBZ Is) (NP (NNP Nikon) (NNP low) (NNP end)) (VP (VBG moving) (PP (IN toward) (NP (NP (DT the) (JJ hellish) (NNS ergonomics)) (PP (IN of) (NP (DT the) (NNP Canon) (NNP Rebel)))))) (. ?)))\n",
                treeOutput(output));
    }

    @Test
    public void testReparsing() throws Exception {
        final String input = "Bavaria crew replaced the mooring rope and attaches bristles on bow and stern.";
        final String output = executeTool(new ParserDriver(), "-reparse escalate -g " + M0_GRAMMAR
                + " -O maxBeamWidth=1 -O lexicalRowBeamWidth=1 -O lexicalRowUnaries=0 -if text -v 2", input.toString());
        assertEquals(
                "(ROOT (S (NP (NNP Bavaria)) (NP (NN crew)) (VP (VP (VBD replaced) (NP (DT the) (VBG mooring) (NN rope))) (CC and) (VP (VBZ attaches) (VP (VBZ bristles) (PP (IN on) (UCP (NN bow) (CC and) (JJ stern)))))) (. .)))\n",
                treeOutput(output));
    }

    @Test
    public void testListParser() throws Exception {
        final StringBuilder input = new StringBuilder(256);
        input.append("The economy 's temperature will be taken from several vantage points this week , with readings on trade , output , housing and inflation .\n");
        input.append("The most troublesome report may be the August merchandise trade deficit due out tomorrow .\n");

        final StringBuilder expectedOutput = new StringBuilder(1024);
        expectedOutput
                .append("(ROOT (S (NP (NP (DT The) (NN economy) (POS 's)) (NN temperature)) (VP (MD will) (VP (VB be) (VBN taken) (PP (IN from) (NP (JJ several) (NN vantage) (NNS points))) (VP (NP (DT this) (NN week)) (, ,) (PP (IN with) (NP (NP (NNS readings)) (PP (IN on) (NP (NP (NP (NP (NN trade)) (, ,) (NN output)) (, ,) (NN housing)) (CC and) (NN inflation)))))))) (. .)))\n");
        expectedOutput
                .append("(ROOT (S (NP (DT The) (ADJP (RBS most) (JJ troublesome)) (NN report)) (VP (MD may) (VP (VB be) (NP (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (ADJP (JJ due) (PP (IN out) (NP (NN tomorrow)))))) (. .)))\n");

        final String output = executeTool(new ParserDriver(), "-rp ecpccl -g " + M0_GRAMMAR + " -if token -v 2",
                input.toString());
        assertEquals(expectedOutput.toString(), treeOutput(output));
    }

    @Test
    public void testAgendaParser() throws Exception {
        final StringBuilder input = new StringBuilder(256);
        input.append("The economy 's temperature will be taken from several vantage points this week , with readings on trade , output , housing and inflation .\n");
        input.append("The most troublesome report may be the August merchandise trade deficit due out tomorrow .\n");

        final StringBuilder expectedOutput = new StringBuilder(1024);
        expectedOutput
                .append("(ROOT (S (NP (NP (DT The) (NN economy) (POS 's)) (NN temperature)) (VP (MD will) (VP (VB be) (VBN taken) (PP (IN from) (NP (JJ several) (NN vantage) (NNS points))) (VP (NP (DT this) (NN week)) (, ,) (PP (IN with) (NP (NP (NP (NP (NP (NNS readings)) (PP (IN on) (NP (NN trade)))) (, ,) (NN output)) (, ,) (NN housing)) (CC and) (NN inflation)))))) (. .)))\n");
        expectedOutput
                .append("(ROOT (S (NP (DT The) (ADJP (RBS most) (JJ troublesome)) (NN report)) (VP (MD may) (VP (VB be) (NP (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (ADJP (JJ due) (PP (IN out) (NP (NN tomorrow)))))) (. .)))\n");

        final String output = executeTool(new ParserDriver(), "-rp apall -g " + M0_GRAMMAR
                + " -if token -v 2 -O overParseTune=2", input.toString());
        assertEquals(expectedOutput.toString(), treeOutput(output));
    }

    @Test
    public void testConstrainedParser() throws Exception {
        final StringBuilder input = new StringBuilder(1024);
        input.append("(ROOT (S (NP (NP (JJ Influential) (NNS members)) (PP (IN of) (NP (DT the) (NNP House) (NNP Ways) (CC and) (NNP Means) (NNP Committee)))) (VP (VBD introduced) (NP (NP (NN legislation)) (SBAR (WHNP (WDT that)) (S (VP (MD would) (VP (VB restrict) (SBAR (WHADVP (WRB how)) (S (NP (DT the) (JJ new) (NN savings-and-loan) (NN bailout) (NN agency)) (VP (MD can) (VP (VB raise) (NP (NN capital)))))) (, ,) (S (VP (VBG creating) (NP (NP (DT another) (JJ potential) (NN obstacle)) (PP (TO to) (NP (NP (NP (DT the) (NN government) (POS 's)) (NN sale)) (PP (IN of) (NP (JJ sick) (NNS thrifts)))))))))))))) (. .)))\n");
        input.append("(ROOT (S (NP (NP (DT The) (NN bill)) (, ,) (SBAR (WHNP (WP$ whose) (NNS backers)) (S (VP (VBP include) (NP (NP (NNP Chairman) (NNP Dan) (NNP Rostenkowski)) (PRN (-LRB- -LRB-) (NP (NNP D.)) (, ,) (NP (NNP Ill.)) (-RRB- -RRB-)))))) (, ,)) (VP (MD would) (VP (VB prevent) (NP (DT the) (NNP Resolution) (NNP Trust) (NNP Corp.)) (PP (IN from) (S (VP (VBG raising) (NP (JJ temporary) (VBG working) (NN capital))))) (PP (IN by) (S (VP (VBG having) (NP (NP (DT an) (JJ RTC-owned) (NN bank) (CC or) (NN thrift) (NN issue) (NN debt)) (SBAR (WHNP (WDT that)) (S (VP (MD would) (RB n't) (VP (VB be) (VP (VBN counted) (PP (IN on) (NP (DT the) (JJ federal) (NN budget)))))))))))))) (. .)))\n");

        final StringBuilder expectedOutput = new StringBuilder(1024);
        expectedOutput
                .append("(ROOT (S (NP (NP (JJ Influential) (NNS members)) (PP (IN of) (NP (DT the) (NNP House) (NNP Ways) (CC and) (NNP Means) (NNP Committee)))) (VP (VBD introduced) (NP (NP (NN legislation)) (SBAR (WHNP (WDT that)) (S (VP (MD would) (VP (VB restrict) (SBAR (WHADVP (WRB how)) (S (NP (DT the) (JJ new) (NN savings-and-loan) (NN bailout) (NN agency)) (VP (MD can) (VP (VB raise) (NP (NN capital)))))) (, ,) (S (VP (VBG creating) (NP (NP (DT another) (JJ potential) (NN obstacle)) (PP (TO to) (NP (NP (NP (DT the) (NN government) (POS 's)) (NN sale)) (PP (IN of) (NP (JJ sick) (NNS thrifts)))))))))))))) (. .)))\n");
        expectedOutput
                .append("(ROOT (S (NP (NP (DT The) (NN bill)) (, ,) (SBAR (WHNP (WP$ whose) (NNS backers)) (S (VP (VBP include) (NP (NP (NNP Chairman) (NNP Dan) (NNP Rostenkowski)) (PRN (-LRB- -LRB-) (NP (NNP D.)) (, ,) (NP (NNP Ill.)) (-RRB- -RRB-)))))) (, ,)) (VP (MD would) (VP (VB prevent) (NP (DT the) (NNP Resolution) (NNP Trust) (NNP Corp.)) (PP (IN from) (S (VP (VBG raising) (NP (JJ temporary) (VBG working) (NN capital))))) (PP (IN by) (S (VP (VBG having) (NP (NP (DT an) (JJ RTC-owned) (NN bank) (CC or) (NN thrift) (NN issue) (NN debt)) (SBAR (WHNP (WDT that)) (S (VP (MD would) (RB n't) (VP (VB be) (VP (VBN counted) (PP (IN on) (NP (DT the) (JJ federal) (NN budget)))))))))))))) (. .)))\n");

        final String output = executeTool(new ParserDriver(), "-rp const -g " + M2_GRAMMAR + " -v 2 -O maxBeamWidth=0",
                input.toString());
        assertEquals(expectedOutput.toString(), treeOutput(output));
    }

    // @Test
    public void testLimitedSpanParser() throws Exception {

        String output = executeTool(new ParserDriver(), "-p matrix -maxSubtreeSpan 2 -g " + SIMPLE_GRAMMAR_2
                + " -if token -v 2", "The fish market stands last");
        assertEquals("(ROOT (S (NP (DT The) (NN fish)) (NP (NN market) (NN stands)) (VP (VB last))))\n",
                treeOutput(output));

        final StringBuilder input = new StringBuilder(256);
        input.append("The economy 's temperature will be taken from several vantage points this week , with readings on trade , output , housing and inflation .\n");
        input.append("The most troublesome report may be the August merchandise trade deficit due out tomorrow .\n");

        final StringBuilder expectedOutput = new StringBuilder(1024);
        expectedOutput
                .append("(ROOT (S (NP (NP (DT The) (NN economy) (POS 's)) (NN temperature)) (VP (MD will) (VP (VB be) (VBN taken) (PP (IN from) (NP (JJ several) (NN vantage) (NNS points))) (VP (NP (DT this) (NN week)) (, ,) (PP (IN with) (NP (NP (NNS readings)) (PP (IN on) (NP (NP (NP (NP (NN trade)) (, ,) (NN output)) (, ,) (NN housing)) (CC and) (NN inflation)))))))) (. .)))\n");
        expectedOutput
                .append("(ROOT (S (NP (DT The) (ADJP (RBS most) (JJ troublesome)) (NN report)) (VP (MD may) (VP (VB be) (NP (DT the) (NNP August) (NN merchandise) (NN trade) (NN deficit)) (ADJP (JJ due) (PP (IN out) (NP (NN tomorrow)))))) (. .)))\n");

        output = executeTool(new ParserDriver(), "-p matrix -maxSubtreeSpan 3 -g " + M0_GRAMMAR + " -if token -v 2",
                input.toString());
        assertEquals(expectedOutput.toString(), treeOutput(output));
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
