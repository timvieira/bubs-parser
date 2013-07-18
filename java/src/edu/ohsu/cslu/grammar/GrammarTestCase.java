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
package edu.ohsu.cslu.grammar;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;

public abstract class GrammarTestCase {

    /**
     * @return the grammar class under test
     */
    protected abstract Class<? extends Grammar> grammarClass();

    public static <C extends Grammar> C createGrammar(final Class<C> grammarClass, final Reader grammarReader)
            throws Exception {
        return grammarClass.getConstructor(new Class[] { Reader.class, TokenClassifier.class }).newInstance(
                new Object[] { grammarReader, new DecisionTreeTokenClassifier() });
    }

    public static <C extends Grammar> C createGrammar(final Class<C> grammarClass, final Reader grammarReader,
            final Class<? extends SparseMatrixGrammar.PackingFunction> cartesianProductFunctionClass) throws Exception {

        try {
            return grammarClass.getConstructor(new Class[] { Reader.class, Class.class }).newInstance(
                    new Object[] { grammarReader, cartesianProductFunctionClass });
        } catch (final NoSuchMethodException e) {
            return grammarClass.getConstructor(new Class[] { Reader.class })
                    .newInstance(new Object[] { grammarReader });
        }
    }

    public static Reader simpleGrammar() throws Exception {
        final StringBuilder sb = new StringBuilder(256);

        sb.append("format=Berkeley start=ROOT\n");
        sb.append("ROOT => NP 0\n");
        sb.append("NP => NN NN -0.693147\n");
        sb.append("NP => NP NN -1.203972\n");
        sb.append("NP => NN NP -2.302585\n");
        sb.append("NP => NP NP -2.302585\n");

        sb.append(Grammar.LEXICON_DELIMITER);
        sb.append('\n');

        sb.append("NN => systems 0\n");
        sb.append("NN => analyst 0\n");
        sb.append("NN => arbitration 0\n");
        sb.append("NN => chef 0\n");
        sb.append("NN => UNK 0\n");

        return new StringReader(sb.toString());
    }

    /**
     * Tests a _very_ simple grammar.
     * 
     * TODO Share grammar creation with GrammarTestCase
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testSimpleGrammar() throws Exception {

        final Grammar simpleGrammar = createGrammar(grammarClass(), simpleGrammar());

        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "systems"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "analyst"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "arbitration"), .01f);
        assertEquals(0f, simpleGrammar.lexicalLogProbability("NN", "chef"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.lexicalLogProbability("NP", "foo"), .01f);

        assertEquals(-0.693147f, simpleGrammar.binaryLogProbability("NP", "NN", "NN"), .01f);
        assertEquals(-1.203972f, simpleGrammar.binaryLogProbability("NP", "NP", "NN"), .01f);
        assertEquals(-2.302585f, simpleGrammar.binaryLogProbability("NP", "NN", "NP"), .01f);
        assertEquals(-2.302585f, simpleGrammar.binaryLogProbability("NP", "NP", "NP"), .01f);
        assertEquals(Float.NEGATIVE_INFINITY, simpleGrammar.binaryLogProbability("TOP", "NP", "NP"), .01f);
        assertEquals(0f, simpleGrammar.unaryLogProbability("TOP", "NP"), .01f);
    }
}
