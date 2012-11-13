/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */ 
package edu.ohsu.cslu.counters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;

import junit.framework.TestCase;

import org.cjunit.FilteredRunner;
import org.cjunit.PerformanceTest;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.JUnit;

/**
 * Unit tests for Log Likelihood and Log Odds calculations
 * 
 * @author Aaron Dunlop
 * @since Aug 22, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestCoocurrenceCounters extends TestCase {

    @Test
    public void testBigramCounter() throws Exception {
        final String sampleSentences = "in an oct. 19 review of the misanthrope at chicago 's goodman theatre -lrb-"
                + " revitalized classics take the stage in windy city , leisure & arts -rrb- , the role of celimene"
                + " , played by kim cattrall , was mistakenly attributed to christina haag .\n"
                + "ms. haag plays elianti while ms. cattral plays celimene .\n" + "ms. cattral made her debut in 1996.";

        final CoocurrenceCounter cc = new BigramCounter(new StringReader(sampleSentences));
        assertEquals(3, cc.count("in"));
        assertEquals(2, cc.count("haag"));
        assertEquals(2, cc.count("."));
        assertEquals(4, cc.count(","));
        assertEquals(1, cc.count("elianti"));
        assertEquals(0, cc.count("notfound"));

        assertEquals(1, cc.count("ms.", "haag"));
        assertEquals(1, cc.count("revitalized", "classics"));
        assertEquals(0, cc.count("classics", "revitalized"));
        assertEquals(1, cc.count("in", "an"));
        assertEquals(0, cc.count("haag", "ms."));
        assertEquals(1, cc.count("celimene", "."));

        assertEquals(3.7136f, cc.logLikelihoodRatio("ms.", "haag"), .01);
        assertEquals(3.332f, cc.logOddsRatio("ms.", "haag"), .01);

        assertEquals(2.1254f, cc.logLikelihoodRatio(",", "the"), .01);
        assertEquals(2.1972f, cc.logOddsRatio(",", "the"), .01);

        assertEquals(Float.NaN, cc.logLikelihoodRatio("in", "twin-engine"));
        assertEquals(Float.NaN, cc.logOddsRatio("in", "twin-engine"));
    }

    @Test
    public void testSentenceCounter() throws Exception {
        checkSententialCoocurrenceCounter(sententialCoocurrenceCounter());
    }

    @Test
    public void testSerialize() throws Exception {
        // Serialize a counter
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(sententialCoocurrenceCounter());

        // Read in the counter and verify it.
        final CoocurrenceCounter cc = (CoocurrenceCounter) new ObjectInputStream(new ByteArrayInputStream(
                bos.toByteArray())).readObject();
        checkSententialCoocurrenceCounter(cc);
    }

    private void checkSententialCoocurrenceCounter(final CoocurrenceCounter cc) {
        assertEquals(2, cc.count("in"));
        assertEquals(2, cc.count("haag"));
        assertEquals(2, cc.count("."));
        assertEquals(1, cc.count(","));
        assertEquals(1, cc.count("elianti"));
        assertEquals(0, cc.count("notfound"));

        assertEquals(1, cc.count("an", "in"));
        assertEquals(1, cc.count("in", "role"));
        assertEquals(1, cc.count(",", "haag"));
        assertEquals(2, cc.count(".", "haag"));
        assertEquals(1, cc.count("haag", "ms."));
        assertEquals(0, cc.count("plays", "revitalized"));

        // We want all pairs in alphabetical order...
        assertEquals(0, cc.count("ms.", "haag"));

        assertEquals(4.4156f, cc.logLikelihoodRatio("haag", "ms."), .01);
        assertEquals(3.8712f, cc.logOddsRatio("haag", "ms."), .01);
    }

    private CoocurrenceCounter sententialCoocurrenceCounter() throws IOException {
        final String sampleSentences = "in an oct. 19 review of the misanthrope at chicago 's goodman theatre -lrb-"
                + " revitalized classics take the stage in windy city , leisure & arts -rrb- , the role of celimene"
                + " , played by kim cattrall , was mistakenly attributed to christina haag .\n"
                + "ms. haag plays elianti while ms. cattral plays celimene .\n" + "ms. cattral made her debut in 1996.";

        final CoocurrenceCounter cc = new SententialCoocurrenceCounter(new StringReader(sampleSentences));
        return cc;
    }

    @Test
    @PerformanceTest({ "d820", "4938" })
    public void profileBigramCounter() throws Exception {
        final CoocurrenceCounter cc = new BigramCounter(new InputStreamReader(
                JUnit.unitTestDataAsStream("counters/f2-21.lowercase.txt.gz")));

        assertEquals("Wrong log likelihood for '64', '.'", 0f, cc.logLikelihoodRatio("64", "."), .01);
        assertEquals("Wrong log likelihood for 'industry', 'than'", Float.NaN,
                cc.logLikelihoodRatio("indstry", "than"), .01);
        assertEquals("Wrong log likelihood for 'mr.', 'white'", 0f, cc.logLikelihoodRatio("mr.", "white"), .01);

        assertEquals("Wrong log likelihood for 'helga', 'kern'", 25.0, cc.logLikelihoodRatio("helga", "kern"), .01);

        assertEquals("Wrong log likelihood for 'massive', 'bill'", 6.6408f, cc.logLikelihoodRatio("massive", "bill"),
                .01);
        assertEquals("Wrong log likelihood for 'twin-engine', 'and'", 5.3197f,
                cc.logLikelihoodRatio("twin-engine", "and"), .01);

        assertEquals("Wrong log likelihood for '.', '.'", 3378.9f, cc.logLikelihoodRatio(".", "."), .01);
        assertEquals("Wrong log likelihood for 'in', 'in'", 610.3038f, cc.logLikelihoodRatio("in", "in"), .01);
        assertEquals("Wrong log likelihood for 'a', 'and'", 718.2013f, cc.logLikelihoodRatio("a", "and"), .01);
        assertEquals("Wrong log likelihood for 'dilutive', 'eqivalents'", 94.2626f,
                cc.logLikelihoodRatio("dilutive", "equivalents"), .01);
        assertEquals("Wrong log likelihood for 'k', 'mart'", 532.9244f, cc.logLikelihoodRatio("k", "mart"), .01);
    }

    @Test
    @PerformanceTest({ "d820", "19297" })
    public void profileSententialCoocurrenceCounter() throws Exception {
        final CoocurrenceCounter cc = new SententialCoocurrenceCounter(new InputStreamReader(
                JUnit.unitTestDataAsStream("counters/f2-21.lowercase.txt.gz")));

        // ByteArrayOutputStream bos = new ByteArrayOutputStream();
        // ObjectOutputStream oos = new ObjectOutputStream(bos);
        // oos.writeObject(cc);
        //
        // System.out.println(bos.size());

        assertEquals("Wrong log likelihood for 'repaying', 'said'", 10.024f, cc.logLikelihoodRatio("repaying", "said"),
                .01);
        assertEquals("Wrong log likelihood for 'helga', 'move'", 15.8869f, cc.logLikelihoodRatio("helga", "move"), .01);
        assertEquals("Wrong log likelihood for '.', 'for'", 46478.395f, cc.logLikelihoodRatio(".", "for"), .01);
        assertEquals("Wrong log likelihood for 'of', 'the'", 83680.55f, cc.logLikelihoodRatio("of", "the"), .01);
        assertEquals("Wrong log likelihood for 'and', 'the'", 57286.11f, cc.logLikelihoodRatio("and", "the"), .01);

        assertEquals("Wrong log likelihood for 'million', 'she'", 0.2527f, cc.logLikelihoodRatio("million", "she"), .01);
        assertEquals("Wrong log likelihood for 'composite', 'from'", 0.02775f,
                cc.logLikelihoodRatio("composite", "from"), .01);
        assertEquals("Wrong log likelihood for 'banca', 'nazionale'", 72.22401f,
                cc.logLikelihoodRatio("banca", "nazionale"), .01);
        assertEquals("Wrong log likelihood for 'basir', 'sri'", 119.4472f, cc.logLikelihoodRatio("basir", "sri"), .01);
        assertEquals("Wrong log likelihood for 'burnham', 'lambert'", 951.8335f,
                cc.logLikelihoodRatio("burnham", "lambert"), .01);
    }
}
