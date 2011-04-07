package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.tools.TreeTools;

/**
 * Unit tests for {@link Parser} class.
 * 
 * @author Aaron Dunlop
 * @since May 20, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestParser {

    @Test
    public void testUnfactor() throws Exception {
        assertEquals("(ROOT (NP (: --) (NNP C.E.) (NNP Friedman) (. .)))", TreeTools.unfactor(
                "(ROOT_0 (NP_31 (@NP_29 (@NP_40 (:_3 --) (NNP_0 C.E.)) (NNP_9 Friedman)) (._3 .)))",
                GrammarFormatType.Berkeley));

        assertEquals(
                "(ROOT (S (NP (NN Trouble)) (VP (VBZ is) (, ,) (SBAR (S (NP (PRP she)) (VP (VBZ has) (VP (VBN lost) ("
                        + "NP (PRP it)) (ADVP (RB just) (RB as) (RB quickly))))))) (. .)))", TreeTools.unfactor(
                        "(ROOT_0 (S_0 (@S_24 (NP_23 (NN_26 Trouble)) (VP_32 (@VP_10 (VBZ_17 is) (,_0 ,))"
                                + " (SBAR_1 (S_5 (NP_36 (PRP_2 she)) (VP_34 (VBZ_16 has) (VP_11 (@VP_28"
                                + " (VBN_23 lost) (NP_37 (PRP_1 it))) (ADVP_1 (@ADVP_0 (RB_31 just)"
                                + " (RB_32 as)) (RB_2 quickly)))))))) (._3 .)))", GrammarFormatType.Berkeley));

        assertEquals("(TOP (S (NP (NP (JJ Little) (NN chance)) (PP (IN that) (NP (NNP Shane)"
                + " (NNP Longman)))) (VP (AUX is) (VP (VBG going) (S (VP (TO to) (VP (VB recoup) "
                + "(NP (NN today))))))) (. .)))", TreeTools.unfactor(
                "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (NP^<NP> (JJ Little) (NN chance))"
                        + " (PP^<NP> (IN that) (NP^<PP> (NNP Shane) (NNP Longman))))"
                        + " (VP^<S> (AUX is) (VP^<VP> (VBG going) (S^<VP> (VP^<S> (TO to)"
                        + " (VP^<VP> (VB recoup) (NP^<VP> (NN today)))))))) (. .)))", GrammarFormatType.CSLU));
    }
}
