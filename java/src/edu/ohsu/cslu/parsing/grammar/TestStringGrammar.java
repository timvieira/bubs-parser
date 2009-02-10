package edu.ohsu.cslu.parsing.grammar;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import edu.ohsu.cslu.tests.SharedNlpTests;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Grammar Tests
 * 
 * $Id$
 */
public class TestStringGrammar
{
    private final static String FULL_CORPUS = "parsing/f2-21.topos.txt.gz";
    private final static String SMALL_CORPUS = "parsing/f2-21.5000.txt.gz";
    private final static String SIMPLE_TEST_SENTENCE = "(TOP (S (NNP Wnnp) (VP Wvp) (. W.)))";
    private final static String TEST_SENTENCE = "(TOP (S (NP (NNP Wnnp) (NNP Wnnp) (NNPS Wnnps) (NNP Wnnp)) (VP (VBD Wvbd) (S (PRP Wprp)"
        + "(VP (VBZ Wvbz) (S (NP (PRP$ Wprp$) (NNP Wnnp) (NNS Wnns))"
        + "(VP (TO Wto) (VP (VB Wvb) (JJ Wjj) (PP (IN Win) (NP (QP (IN Win) (CD Wcd))"
        + "(NNS Wnns))) (PP (IN Win) (CD Wcd)))))))) (. W.)))";

    @Test
    public void induceSimpleGrammar() throws Exception
    {
        InducedGrammar grammar = new InducedGrammar("TOP", new StringReader("(NNP Wnnp)"), false);
        assertEquals(2, grammar.categories().length);
        assertEquals(1, grammar.totalRules());
        // assertEquals(0, grammar.nonWordRules());
        assertEquals(1, grammar.occurrences("NNP", "Wnnp"));
        assertEquals("NNP", grammar.possibleCategories("Wnnp")[0]);

        grammar = new InducedGrammar("TOP", new StringReader(SIMPLE_TEST_SENTENCE), false);
        assertEquals(5, grammar.categories().length);
        assertEquals(5, grammar.totalRules());
        // assertEquals(2, grammar.nonWordRules());
        assertEquals(1, grammar.occurrences("TOP", "S"));
        assertEquals(1, grammar.occurrences("NNP", "Wnnp"));
        assertEquals(1, grammar.occurrences("VP", "Wvp"));
        assertEquals(1, grammar.occurrences(".", "W."));
        assertEquals("S", grammar.possibleCategories("NNP")[0]);
        assertEquals("NNP", grammar.possibleCategories("Wnnp")[0]);

        assertEquals(null, grammar.binaryProductionCategories("S", "W."));

        assertEquals(1, grammar.secondProductions().length);
        assertEquals("VP .", grammar.secondStringProductions()[0]);

        assertEquals(1, grammar.validTopCategories("NNP", "VP .").length);
        assertEquals("S", grammar.validTopCategories("NNP", "VP .")[0]);

        // Now from the test sentence
        grammar = new InducedGrammar("TOP", new StringReader(TEST_SENTENCE), false);

        // We expect:
        // . -> W.
        // CD -> Wcd (2)
        // IN -> Win (3)
        // JJ -> Wjj
        // NNP -> Wnnp (4)
        // NNPS -> Wnnps
        // NNS -> Wnns (2)
        // NP -> NNP NNP NNPS NNP
        // NP -> PRP$ NNP NNS
        // NP -> QP NNS
        // PP -> IN CD
        // PP -> IN NP
        // PRP -> Wprp
        // PRP$ -> Wprp$
        // QP -> IN CD
        // S -> NP VP
        // S -> NP VP .
        // S -> PRP VP
        // TO -> Wto
        // TOP -> S
        // VB -> Wvb
        // VBD -> Wvbd
        // VBZ -> Wvbz
        // VP -> TO VP
        // VP -> VB JJ PP PP
        // VP -> VBD S
        // VP -> VBZ S
        assertEquals(19, grammar.categories().length);
        assertEquals(27, grammar.totalRules());
        // assertEquals(14, grammar.nonWordRules());
        assertEquals(1, grammar.occurrences(".", "W."));
        assertEquals(2, grammar.occurrences("CD", "Wcd"));
        assertEquals(3, grammar.occurrences("IN", "Win"));
        assertEquals(1, grammar.occurrences("JJ", "Wjj"));
        assertEquals(4, grammar.occurrences("NNP", "Wnnp"));
        assertEquals(1, grammar.occurrences("NNPS", "Wnnps"));
        assertEquals(2, grammar.occurrences("NNS", "Wnns"));
        assertEquals(1, grammar.occurrences("NP", "NNP", "NNP NNPS NNP"));
        assertEquals(1, grammar.occurrences("NP", "QP", "NNS"));
        assertEquals(1, grammar.occurrences("PP", "IN", "CD"));
        assertEquals(1, grammar.occurrences("PP", "IN", "NP"));
        assertEquals(1, grammar.occurrences("PRP", "Wprp"));
        assertEquals(1, grammar.occurrences("PRP$", "Wprp$"));
        assertEquals(1, grammar.occurrences("QP", "IN", "CD"));
        assertEquals(1, grammar.occurrences("S", "NP", "VP ."));
        assertEquals(1, grammar.occurrences("S", "NP", "VP"));
        assertEquals(1, grammar.occurrences("S", "PRP", "VP"));
        assertEquals(1, grammar.occurrences("TO", "Wto"));
        assertEquals(1, grammar.occurrences("TOP", "S"));
        assertEquals(1, grammar.occurrences("VB", "Wvb"));
        assertEquals(1, grammar.occurrences("VBD", "Wvbd"));
        assertEquals(1, grammar.occurrences("VBZ", "Wvbz"));
        assertEquals(1, grammar.occurrences("VP", "TO", "VP"));
        assertEquals(1, grammar.occurrences("VP", "VB", "JJ PP PP"));
        assertEquals(1, grammar.occurrences("VP", "VBD", "S"));
        assertEquals(1, grammar.occurrences("VP", "VBZ", "S"));

        assertEquals(-1.099, grammar.logProbability("NP", "QP", "NNS"), .01);
        assertEquals(Float.NEGATIVE_INFINITY, grammar.logProbability("NP", "NNS", "QP"), .01);

        assertTrue(grammar.validUnaryProduction("W."));
        assertTrue(grammar.validUnaryProduction("Wnnp"));
        assertFalse(grammar.validUnaryProduction("NNP"));

        assertTrue(grammar.validFirstProduction("QP"));
        assertTrue(grammar.validFirstProduction("TO"));
        assertFalse(grammar.validFirstProduction("CD"));

        assertTrue(grammar.validSecondProduction("CD"));
        assertFalse(grammar.validSecondProduction("QP"));

        // Order doesn't actually matter in this case
        assertEquals("CD", grammar.validSecondProductions("IN")[0]);
        assertEquals("NP", grammar.validSecondProductions("IN")[1]);
    }

    @Test
    public void leftFactor() throws Exception
    {
        InducedGrammar grammar = new InducedGrammar("TOP", new StringReader("(NP (NNP Wnnp) (NNP Wnnp) (NNPS Wnnps))"),
            false);
        InducedGrammar leftFactoredGrammar = grammar.leftFactor();
        // We expect:
        // NP -> NNP NP-NNP
        // NP-NNP -> NNP NNPS
        // NNP -> Wnnp
        // NNPS -> Wnnps
        assertEquals(5, leftFactoredGrammar.categories().length);
        assertEquals(4, leftFactoredGrammar.totalRules());
        // assertEquals(2, leftFactoredGrammar.nonWordRules());
        assertEquals(1, leftFactoredGrammar.occurrences("NP", "NNP", "NP-NNP"));
        assertEquals(1, leftFactoredGrammar.occurrences("NP-NNP", "NNP", "NNPS"));

        grammar = new InducedGrammar("TOP", new StringReader("(TOP (S (NP Wnp) (NNP Wnnp) (VP Wvp) (. W.)))"), false);
        leftFactoredGrammar = grammar.leftFactor();
        // We expect:
        // S -> NP S-NP
        // S-NP -> NNP S-NP-NNP
        // S-NP-NNP -> VP .
        // TOP -> S
        // . -> W.
        // NP -> Wnp
        // NNP -> Wnnp
        // VP -> Wvp
        assertEquals(8, leftFactoredGrammar.categories().length);
        assertEquals(8, leftFactoredGrammar.totalRules());
        assertEquals(1, leftFactoredGrammar.occurrences("S", "NP", "S-NP"));
        assertEquals(1, leftFactoredGrammar.occurrences("S-NP", "NNP", "S-NP-NNP"));
        assertEquals(1, leftFactoredGrammar.occurrences("S-NP-NNP", "VP", "."));
        assertEquals(1, leftFactoredGrammar.occurrences("TOP", "S"));
        assertEquals(1, leftFactoredGrammar.occurrences("NNP", "Wnnp"));
        assertEquals(1, leftFactoredGrammar.occurrences("VP", "Wvp"));
        assertEquals(1, leftFactoredGrammar.occurrences(".", "W."));
        assertEquals("S", leftFactoredGrammar.validTopCategories("NP", "S-NP")[0]);

        // Now from the test sentence
        grammar = new InducedGrammar("TOP", new StringReader(TEST_SENTENCE), false);
        leftFactoredGrammar = grammar.leftFactor();
        // We expect (left-factored rules only)
        // NP -> NNP NP-NNP
        // NP-NNP -> NNP NP-NNP-NNP
        // NP-NNP-NNP -> NNPS NNP
        // NP -> PRP$ NP-PRP$
        // NP-PRP$ -> NNP NNS
        // VP -> VB VP-VB
        // VP-VB -> JJ VP-VB-JJ
        // VP-VB-JJ -> PP PP
        assertEquals(25, leftFactoredGrammar.categories().length);
        assertEquals(33, leftFactoredGrammar.totalRules());
        // assertEquals(20, leftFactoredGrammar.nonWordRules());
        assertEquals(1, leftFactoredGrammar.occurrences("NP", "NNP", "NP-NNP"));
        assertEquals(1, leftFactoredGrammar.occurrences("NP-NNP", "NNP", "NP-NNP-NNP"));
        assertEquals(1, leftFactoredGrammar.occurrences("NP-NNP-NNP", "NNPS", "NNP"));
        assertEquals(1, leftFactoredGrammar.occurrences("NP", "PRP$", "NP-PRP$"));
        assertEquals(1, leftFactoredGrammar.occurrences("NP-PRP$", "NNP", "NNS"));
        assertEquals(1, leftFactoredGrammar.occurrences("VP", "VB", "VP-VB"));
        assertEquals(1, leftFactoredGrammar.occurrences("VP-VB", "JJ", "VP-VB-JJ"));
        assertEquals(1, leftFactoredGrammar.occurrences("VP-VB-JJ", "PP", "PP"));

        assertEquals(-1.099f, leftFactoredGrammar.logProbability("NP", "NNP", "NP-NNP"), .01);
        assertEquals(-1.099f, leftFactoredGrammar.logProbability("NP", "PRP$", "NP-PRP$"), .01);
        assertEquals(Float.NEGATIVE_INFINITY, leftFactoredGrammar.logProbability("NP", "PRP", "NP-PRP$"), .01);
        assertEquals(0, leftFactoredGrammar.logProbability("NNS", "Wnns"), .01);
    }

    @Test
    public void unfactor() throws Exception
    {
        String example = "(NP (NNP Wnnp) (NP-NNP (NNP Wnnp) (NNPS Wnnps)))";
        String exampleUnfactored = "(NP (NNP Wnnp) (NNP Wnnp) (NNPS Wnnps))";

        String f24Sentence1 = "(TOP (S (NP (NP (DT Wdt) (NP-DT (NN Wnn) (POS Wpos))) (NN Wnn))"
            + " (S-NP (VP (MD Wmd) (VP (AUX Waux) (VP (VBN Wvbn) (VP-VBN (PP (IN Win) (NP (JJ Wjj)"
            + " (NP-JJ (NN Wnn) (NNS Wnns)))) (VP-VBN-PP (NP (DT Wdt) (NN Wnn)) (VP-VBN-PP-NP (, W,)"
            + " (PP (IN Win) (NP (NNS Wnns) (PP (IN Win) (NP (NN Wnn) (NP-NN (, W,) (NP-NN-, (NN Wnn)"
            + " (NP-NN-,-NN (, W,) (NP-NN-,-NN-, (NN Wnn) (NP-NN-,-NN-,-NN (CC Wcc) (NN Wnn)))))))))))))))) (. W.))))";
        String f24Sentence1Unfactored = "(TOP (S (NP (NP (DT Wdt) (NN Wnn) (POS Wpos)) (NN Wnn))"
            + " (VP (MD Wmd) (VP (AUX Waux) (VP (VBN Wvbn) (PP (IN Win) (NP (JJ Wjj) (NN Wnn)"
            + " (NNS Wnns))) (NP (DT Wdt) (NN Wnn)) (, W,) (PP (IN Win) (NP (NNS Wnns) (PP (IN Win)"
            + " (NP (NN Wnn) (, W,) (NN Wnn) (, W,) (NN Wnn) (CC Wcc) (NN Wnn)))))))) (. W.)))";

        assertEquals(exampleUnfactored, BaseStringGrammar.unfactor(example));
        assertEquals(f24Sentence1Unfactored, BaseStringGrammar.unfactor(f24Sentence1));
    }

    @Test
    public void smallCorpus() throws Exception
    {
        InducedGrammar grammar = new InducedGrammar("TOP", new InputStreamReader(SharedNlpTests
            .unitTestDataAsStream(SMALL_CORPUS)), true);

        assertEquals(5155, grammar.totalRules());
        // assertEquals(5116, grammar.nonWordRules());
        assertEquals(606, grammar.occurrences("ADVP"));

        InducedGrammar leftFactoredGrammar = grammar.leftFactor();
        assertEquals(7632, leftFactoredGrammar.totalRules());
        // assertEquals(7593, leftFactoredGrammar.nonWordRules());
        assertEquals(606, leftFactoredGrammar.occurrences("ADVP"));
    }

    @Test
    public void fullCorpus() throws Exception
    {
        InducedGrammar grammar = new InducedGrammar("TOP", new InputStreamReader(SharedNlpTests
            .unitTestDataAsStream(FULL_CORPUS)), true);

        assertEquals(17590, grammar.totalRules());
        assertEquals(72, grammar.categories().length);
        assertEquals(8024, grammar.productions().length);

        InducedGrammar leftFactoredGrammar = grammar.leftFactor();
        assertEquals(25961, leftFactoredGrammar.totalRules());
        assertEquals(8443, leftFactoredGrammar.categories().length);
        assertEquals(8488, leftFactoredGrammar.productions().length);

        assertEquals(5, leftFactoredGrammar.binaryProductionCategories("CD", ".").length);

        assertEquals(1, leftFactoredGrammar.binaryProductionCategories("VB", "VP-RB-VB").length);

        Set<String> s = new HashSet<String>(Arrays.asList(leftFactoredGrammar.validSecondProductions("VB")));
        assertTrue(s.contains("VP-RB-VB"));

        s = new HashSet<String>(Arrays.asList(leftFactoredGrammar.validSecondProductions("NP")));
        assertTrue(s.contains("S-S-:-,-NP"));
        assertEquals(0, leftFactoredGrammar.logProbability("S-S-:-,", "NP", "S-S-:-,-NP"), 0.01f);
    }
}
