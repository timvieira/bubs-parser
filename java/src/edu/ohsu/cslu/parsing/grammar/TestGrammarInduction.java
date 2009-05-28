package edu.ohsu.cslu.parsing.grammar;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import edu.ohsu.cslu.tests.SharedNlpTests;

public class TestGrammarInduction
{
    private static String FULL_CORPUS = "corpora/wsj/wsj_02-21.mrgEC.gz";

    @Test
    public void fullCorpus() throws Exception
    {
        InducedGrammar grammar = new InducedGrammar("TOP", new InputStreamReader(SharedNlpTests
            .unitTestDataAsStream(FULL_CORPUS)), true);

        // assertEquals(17590, grammar.totalRules());
        // assertEquals(72, grammar.categories().length);
        // assertEquals(8024, grammar.productions().length);

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
