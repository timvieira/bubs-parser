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

import static org.junit.Assert.assertEquals;

import java.io.Reader;

import org.cjunit.FilteredRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.LeftShiftFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.grammar.TokenClassifier;
import edu.ohsu.cslu.lela.ConstrainedCellSelector;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.Parser.InputFormat;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.ecp.ChartParserTestCase;
import edu.ohsu.cslu.parser.fom.InsideProb;

/**
 * @author Aaron Dunlop
 * @since Feb 9, 2012
 */
@RunWith(FilteredRunner.class)
public class TestConstrainedCphSpmlParser extends ChartParserTestCase<ConstrainedCphSpmlParser> {

    @Override
    @Before
    public void setUp() throws Exception {
        GlobalConfigProperties.singleton().setProperty("maxLocalDelta", "1000000");
        super.setUp();
    }

    @Override
    protected ParserDriver parserOptions() throws Exception {
        final ParserDriver options = new ParserDriver();
        options.binaryTreeOutput = true;
        options.fomModel = new InsideProb();
        options.cellSelectorModel = ConstrainedCellSelector.MODEL;
        return options;
    }

    @Test
    public void testSimpleGrammar2() throws Exception {
        parser = createParser(simpleGrammar2, parserOptions(), configProperties());

        final String constrainingTree = "(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))";
        final ParseTask task = new ParseTask(constrainingTree, InputFormat.Tree, simpleGrammar2,
                DecodeMethod.ViterbiMax);
        assertEquals(constrainingTree, parser.findBestParse(task).toString());
    }

    /**
     * Tests a constrained parse in which the constraining tree contains productions not present in the grammar. This
     * requires 'hallucinating' additional grammar productions during inference. This test includes one binary
     * production and one unary production which are not in the grammar.
     * 
     * @throws Exception
     */
    @Test
    public void testSimpleGrammar2WithImpossibleProds() throws Exception {
        parser = createParser(simpleGrammar2, parserOptions(), configProperties());

        final String constrainingTree = "(ROOT (S (NP (DT The) (NP (NN fish) (NP (NN market)))) (VP (VB stands) (RB last))))";
        final ParseTask task = new ParseTask(constrainingTree, InputFormat.Tree, simpleGrammar2,
                DecodeMethod.ViterbiMax);
        assertEquals(constrainingTree, parser.findBestParse(task).toString());
        // assertEquals("(ROOT (S (NP (DT The) (NP (NN fish) (NN market))) (VP (VB stands) (RB last))))", parser
        // .findBestParse(task).toString());
    }

    @Test
    public void testSentence1() throws Exception {
        final String constrainingTree = "(ROOT (S (S (NP (ADJP (NP (DT The) (NN economy) (POS 's))) (NN temperature)) (VP (MD will) (VP (VB be) (VP (VP (VP (VBN taken) (PP (IN from) (NP (NP (JJ several) (NN vantage)) (NNS points)))) (NP (DT this) (NN week))) (, ,) (PP (IN with) (NP (NP (NNS readings)) (PP (IN on) (NP (NN trade) (, ,) (NN output) (, ,) (NN housing) (, ,) (CC and) (NN inflation))))))))) (. .)))";

        final ParseTask task = new ParseTask(constrainingTree, InputFormat.Tree, f2_21_grammar, DecodeMethod.ViterbiMax);
        assertEquals(constrainingTree, parser.findBestParse(task).unfactor(GrammarFormatType.CSLU).toString());
    }

    @Test
    public void testWithImpossibleLexicalRule() throws Exception {
        final String constrainingTree = "(ROOT (S (NP (NN PaineWebber)) (ADVP (RB also)) (VP (VBD was) (ADJP (JJ able) (S (VP (TO to) (VP (VB gear) (PRT (RP up)) (ADVP (RB quickly)))))) (NP (NP (NNS thanks)) (PP (TO to) (NP (DT the) (CD 1987) (NN crash))))) (. .)))";
        final ParseTask task = new ParseTask(constrainingTree, InputFormat.Tree, f2_21_grammar, DecodeMethod.ViterbiMax);
        assertEquals(constrainingTree, parser.findBestParse(task).unfactor(GrammarFormatType.CSLU).toString());
    }

    /**
     * Tests VP -> S -> VP -> VBZ. Our chart structure can't handle unary self-chains, so we miss those cases. Note that
     * the 'expected' parse here does not contain the long unary chain in the constraining parse.
     * 
     * @throws Exception
     */
    @Test
    public void testWithUnarySelfChain() throws Exception {
        final String constrainingTree = "(ROOT (S (CC But) (PRN (SBAR (IN as) (S (NP (NNP Drexel) (NN analyst) (NNP Linda) (NNP Dunn)) (VP (S (VP (VBZ notes))))))) (, ,) (NP (PRP$ its) (NNS properties)) (VP (MD will) (VP (VB be) (VP (VBN developed) (PP (IN over) (NP (QP (CD 15) (TO to) (CD 20)) (NNS years)))))) (. .)))";
        final ParseTask task = new ParseTask(constrainingTree, InputFormat.Tree, f2_21_grammar, DecodeMethod.ViterbiMax);
        assertEquals(
                "(ROOT (S (CC But) (PRN (SBAR (IN as) (S (NP (NNP Drexel) (NN analyst) (NNP Linda) (NNP Dunn)) (VP (VBZ notes))))) (, ,) (NP (PRP$ its) (NNS properties)) (VP (MD will) (VP (VB be) (VP (VBN developed) (PP (IN over) (NP (QP (CD 15) (TO to) (CD 20)) (NNS years)))))) (. .)))",
                parser.findBestParse(task).unfactor(GrammarFormatType.CSLU).toString());
    }

    /**
     * For constrained parsing, we have to use {@link LeftShiftFunction} to pack children instead of
     * {@link PerfectIntPairHashPackingFunction} because we occasionally need to represent productions that don't occur
     * in the grammar.
     */
    @Override
    public Grammar createGrammar(final Reader grammarReader) throws Exception {
        return grammarClass()
                .getConstructor(new Class[] { Reader.class, TokenClassifier.class, Class.class })
                .newInstance(new Object[] { grammarReader, new DecisionTreeTokenClassifier(), LeftShiftFunction.class });
    }

}
