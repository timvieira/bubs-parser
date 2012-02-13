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

package edu.ohsu.cslu.parser.ecp;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;

import org.junit.AfterClass;
import org.junit.Before;

import cltool4j.ConfigProperties;
import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarTestCase;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.cellselector.CellSelectorModel;
import edu.ohsu.cslu.parser.cellselector.LeftRightBottomTopTraversal;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.parser.fom.InsideProb;
import edu.ohsu.cslu.tests.JUnit;

/**
 * @author Aaron Dunlop
 * @since Feb 9, 2012
 */
public abstract class ChartParserTestCase<P extends ChartParser<? extends Grammar, ? extends Chart>> {
    // Grammar file paths, relative to unit test data root directory
    protected final static String PCFG_FILE = "grammars/eng.R2.gr.gz";

    /** Very simple grammar for parsing 'systems analyst arbitration chef' */
    protected static Grammar simpleGrammar1;

    /** Slightly larger grammar for parsing 'The fish market stands last' */
    protected static Grammar simpleGrammar2;

    /** Grammar induced from WSJ sections 2-21 */
    protected static Grammar f2_21_grammar;

    /** WSJ section 24 sentences 1-20 */
    protected static ArrayList<String[]> sentences = new ArrayList<String[]>();

    /** The parser under test */
    protected ChartParser<?, ?> parser;

    /**
     * Constructs the grammar (if necessary) and a new parser instance. Run prior to each test method.
     * 
     * @throws Exception if unable to construct grammar or parser.
     */
    @Before
    public void setUp() throws Exception {
        if (f2_21_grammar == null || f2_21_grammar.getClass() != grammarClass()) {
            f2_21_grammar = createGrammar(JUnit.unitTestDataAsReader(PCFG_FILE));
        }

        if (simpleGrammar1 == null || simpleGrammar1.getClass() != grammarClass()) {
            simpleGrammar1 = createGrammar(GrammarTestCase.simpleGrammar());
        }

        if (simpleGrammar2 == null || simpleGrammar2.getClass() != grammarClass()) {
            simpleGrammar2 = createGrammar(simpleGrammar2());
        }

        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAX_BEAM_WIDTH, "0");
        parser = createParser(f2_21_grammar, LeftRightBottomTopTraversal.MODEL, parserOptions(), configProperties());
    }

    public static Reader simpleGrammar2() throws Exception {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("format=CSLU start=ROOT\n");
        sb.append("S => NP VP 0\n");
        sb.append("ROOT => S 0\n");
        sb.append("NP => DT NP -1.386294361\n");
        sb.append("NP => DT NN -1.386294361\n");
        sb.append("NP => NN NN -1.791759469\n");
        sb.append("NP => NN NP|NN -1.791759469\n");
        sb.append("NP => NN RB -1.791759469\n");
        sb.append("NP|NN => NN NN 0\n");
        sb.append("VP => VB RB -0.693147181\n");
        sb.append("VP => VB VP|VB -1.386294361\n");
        sb.append("VP => VB -1.386294361\n");
        sb.append("VP|VB => NP 0\n");

        sb.append(Grammar.LEXICON_DELIMITER);
        sb.append('\n');

        sb.append("DT => The 0\n");
        sb.append("NN => fish 0\n");
        sb.append("NN => market -0.405465108\n");
        sb.append("VB => market -1.098612289\n");
        sb.append("NN => stands -0.693147181\n");
        sb.append("VB => stands -0.693147181\n");
        sb.append("RB => last -0.405465108\n");
        sb.append("VB => last -1.098612289\n");
        sb.append("NN => UNK 0\n");

        return new StringReader(sb.toString());
    }

    /**
     * Creates the appropriate parser options for each test class.
     * 
     * @return options
     * @throws Exception if something breaks while constructing the options instance (e.g. failing to find a model
     *             file).
     */
    protected ParserDriver parserOptions() throws Exception {
        final ParserDriver options = new ParserDriver();
        options.binaryTreeOutput = true;
        options.fomModel = new InsideProb();
        return options;
    }

    @AfterClass
    public static void suiteTearDown() throws Exception {
        GlobalConfigProperties.singleton().clear();
    }

    /**
     * Returns parser configuration options.
     * 
     * @return options
     * @throws Exception if something breaks while constructing the options instance (e.g. failing to find a model
     *             file).
     */
    protected ConfigProperties configProperties() throws Exception {
        return new ConfigProperties();
    }

    /**
     * Creates the appropriate parser for each test class. Ugly reflection code, but at least it's all localized here.
     * 
     * @param grammar The grammar to use when parsing
     * @param cellSelectorModel Model to produce {@link CellSelector} controlling chart traversal
     * @return Parser instance
     */
    @SuppressWarnings("unchecked")
    protected final P createParser(final Grammar grammar, final CellSelectorModel cellSelectorModel,
            final ParserDriver options, final ConfigProperties configProperties) {
        if (options != null) {
            options.cellSelectorModel = cellSelectorModel;
        }
        try {
            final Class<P> parserClass = ((Class<P>) ((ParameterizedType) getClass().getGenericSuperclass())
                    .getActualTypeArguments()[0]);
            try {
                // First, try for a constructor that takes both ParserDriver (options) and ConfigProperties
                return parserClass.getConstructor(
                        new Class[] { ParserDriver.class, ConfigProperties.class, grammarClass() }).newInstance(
                        new Object[] { options, configProperties, grammar });

            } catch (final NoSuchMethodException e) {
                // If not found, use a constructor that takes only a ParserDriver instance.
                return parserClass.getConstructor(new Class[] { ParserDriver.class, grammarClass() }).newInstance(
                        new Object[] { options, grammar });
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find the appropriate grammar class for the parser under test. Even more ugly reflection code. Dragons be here...
     * 
     * @return the grammar class appropriate for the parser under test
     */
    @SuppressWarnings("unchecked")
    protected final Class<? extends Grammar> grammarClass() {
        Class<P> parserClass = ((Class<P>) ((ParameterizedType) getClass().getGenericSuperclass())
                .getActualTypeArguments()[0]);
        try {
            final Class<? extends Grammar> grammarClass = ((Class<? extends Grammar>) ((ParameterizedType) parserClass
                    .getGenericSuperclass()).getActualTypeArguments()[0]);

            // If the grammar class is not annotated on this parser class, look up one level
            if (!Grammar.class.isAssignableFrom(grammarClass)) {
                throw new ClassCastException();
            }

            return grammarClass;

        } catch (final ClassCastException e) {

            // Look up one level in the parser hierarchy
            parserClass = (Class<P>) parserClass.getSuperclass();
            final Class<? extends Grammar> grammarClass = ((Class<? extends Grammar>) ((ParameterizedType) parserClass
                    .getGenericSuperclass()).getActualTypeArguments()[0]);
            return grammarClass;
        }
    }

    /**
     * @return the parser class under test
     */
    @SuppressWarnings("unchecked")
    protected final Class<P> parserClass() {
        return ((Class<P>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    public Grammar createGrammar(final Reader grammarReader) throws Exception {
        return grammarClass().getConstructor(new Class[] { Reader.class }).newInstance(new Object[] { grammarReader });
    }

}
