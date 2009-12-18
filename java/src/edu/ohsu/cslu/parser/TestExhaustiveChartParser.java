package edu.ohsu.cslu.parser;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.tests.SharedNlpTests;
import static org.junit.Assert.assertEquals;

public class TestExhaustiveChartParser {

    private final static String PCFG_FILE = SharedNlpTests.UNIT_TEST_DIR + "grammars/f2-21-R2-p1-unk.pcfg";
    private final static String LEX_FILE = SharedNlpTests.UNIT_TEST_DIR + "grammars/f2-21-R2-p1-unk.lex";

    String f24sentence1 = "The economy 's temperature will be taken from several vantage points this week , with readings on trade "
            + ", output , housing and inflation . ";
    String f24sentence2 = "The most troublesome report may be the August merchandise trade deficit due out tomorrow . ";
    String f24sentence3 = "The trade gap is expected to widen to about $ 9 billion from July 's $ 7.6 billion , according to a "
            + "survey by MMS International , a unit of McGraw-Hill Inc. , New York . ";
    String f24sentence4 = "Thursday 's report on the September consumer price index is expected to rise , although not as sharply "
            + "as the 0.9 % gain reported Friday in the producer price index . ";
    String f24sentence5 = "That gain was being cited as a reason the stock market was down early in Friday 's session , before "
            + "it got started on its reckless 190-point plunge . ";
    String f24sentence6 = "Economists are divided as to how much manufacturing strength they expect to see in September reports "
            + "on industrial production and capacity utilization , also due tomorrow . ";
    String f24sentence7 = "Meanwhile , September housing starts , due Wednesday , are thought to have inched upward . ";
    String f24sentence8 = "`` There 's a possibility of a surprise '' in the trade report , said Michael Englund , director of "
            + "research at MMS . ";
    String f24sentence9 = "A widening of the deficit , if it were combined with a stubbornly strong dollar , would exacerbate "
            + "trade problems -- but the dollar weakened Friday as stocks plummeted . ";
    String f24sentence10 = "In any event , Mr. Englund and many others say that the easy gains in narrowing the trade gap have "
            + "already been made . ";

    String f24parse1 = "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (NP^<NP> (NP|<DT-NN>^<NP> (DT The) (NN economy)) (POS 's)) (NN "
            + "temperature)) (VP^<S> (MD will) (VP^<VP> (AUX be) (VP^<VP> (VP|<NP-,>^<VP> (VP|<PP-NP>^<VP> (VP|<VBN-PP>^<VP> "
            + "(VBN taken) (PP^<VP> (IN from) (NP^<PP> (NP|<JJ-NN>^<PP> (JJ several) (NN vantage)) (NNS points)))) "
            + "(NP^<VP> (DT this) (NN week))) (, ,)) (PP^<VP> (IN with) (NP^<PP> (NP^<NP> (NNS readings)) (PP^<NP> "
            + "(IN on) (NP^<PP> (NP|<NN-CC>^<PP> (NP|<,-NN>^<PP> (NP|<NN-,>^<PP> (NP|<,-NN>^<PP> (NP|<NN-,>^<PP> (NN "
            + "trade) (, ,)) (NN output)) (, ,)) (NN housing)) (CC and)) (NN inflation))))))))) (. .)))";
    String f24parse2 = "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (NP|<DT-ADJP>^<S> (DT The) (ADJP^<NP> (RBS most) (JJ troublesome))) "
            + "(NN report)) (VP^<S> (MD may) (VP^<VP> (AUX be) (NP^<VP> (NP^<NP> (NP|<NN-NN>^<NP> (NP|<NNP-NN>^<NP> "
            + "(NP|<DT-NNP>^<NP> (DT the) (NNP August)) (NN merchandise)) (NN trade)) (NN deficit)) (PP^<NP> (PP|<JJ-IN>^<NP> "
            + "(JJ due) (IN out)) (NP^<PP> (NN tomorrow))))))) (. .)))";
    String f24parse3 = "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (NP|<DT-NN>^<S> (DT The) (NN trade)) (NN gap)) (VP^<S> (AUX "
            + "is) (VP^<VP> (VBN expected) (S^<VP> (VP^<S> (TO to) (VP^<VP> (VB widen) (PP^<VP> (TO to) (NP^<PP> (NP^<NP> "
            + "(QP^<NP> (QP|<$-CD>^<NP> (QP|<RB-$>^<NP> (RB about) ($ $)) (CD 9)) (CD billion))) (PP^<NP> (IN from) "
            + "(NP^<PP> (NP|<ADJP-,>^<PP> (NP|<NP-ADJP>^<PP> (NP^<NP> (NNP July) (POS 's)) (ADJP^<NP> (QP^<ADJP> (QP|<$-CD>^<ADJP> "
            + "($ $) (CD 7.6)) (CD billion)))) (, ,)) (VP^<NP> (VBG according) (PP^<VP> (TO to) (NP^<PP> (NP^<NP> "
            + "(DT a) (NN survey)) (PP^<NP> (IN by) (NP^<PP> (NP|<NP-,>^<PP> (NP|<,-NP>^<PP> (NP|<NP-,>^<PP> (NP^<NP> "
            + "(NNP MMS) (NNP International)) (, ,)) (NP^<NP> (NP^<NP> (DT a) (NN unit)) (PP^<NP> (IN of) (NP^<PP> "
            + "(NNP McGraw-Hill) (NNP Inc.))))) (, ,)) (NP^<NP> (NNP New) (NNP York))))))))))))))))) (. .)))";
    String f24parse4 = "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (NP^<NP> (NP^<NP> (NNP Thursday) (POS 's)) (NN report)) (PP^<NP> "
            + "(IN on) (NP^<PP> (NP|<NN-NN>^<PP> (NP|<NNP-NN>^<PP> (NP|<DT-NNP>^<PP> (DT the) (NNP September)) (NN "
            + "consumer)) (NN price)) (NN index)))) (VP^<S> (AUX is) (VP^<VP> (VP|<PP-,>^<VP> (VP|<VBN-PP>^<VP> (VBN "
            + "expected) (PP^<VP> (TO to) (NP^<PP> (NN rise)))) (, ,)) (SBAR^<VP> (IN although) (S^<SBAR> (VP^<S> "
            + "(VP|<ADVP-VBD>^<S> (ADVP^<VP> (ADVP^<ADVP> (ADVP|<RB-RB>^<ADVP> (RB not) (RB as)) (RB sharply)) (PP^<ADVP> "
            + "(IN as) (NP^<PP> (NP|<DT-ADJP>^<PP> (DT the) (ADJP^<NP> (CD 0.9) (NN %))) (NN gain)))) (VBD reported)) "
            + "(NP^<VP> (NP^<NP> (NNP Friday)) (PP^<NP> (IN in) (NP^<PP> (NP|<NN-NN>^<PP> (NP|<DT-NN>^<PP> (DT the) "
            + "(NN producer)) (NN price)) (NN index)))))))))) (. .)))";
    String f24parse5 = "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (DT That) (NN gain)) (VP^<S> (AUX was) (VP^<VP> (AUXG being) "
            + "(VP^<VP> (VP|<VBN-PP>^<VP> (VBN cited) (PP^<VP> (IN as) (NP^<PP> (DT a) (NN reason)))) (SBAR^<VP> (S^<SBAR> "
            + "(NP^<S> (NP|<DT-NN>^<S> (DT the) (NN stock)) (NN market)) (VP^<S> (VP|<ADJP-,>^<S> (VP|<AUX-ADJP>^<S> "
            + "(AUX was) (ADJP^<VP> (ADJP|<RB-JJ>^<VP> (RB down) (JJ early)) (PP^<ADJP> (IN in) (NP^<PP> (NP^<NP> "
            + "(NNP Friday) (POS 's)) (NN session))))) (, ,)) (SBAR^<VP> (IN before) (S^<SBAR> (NP^<S> (PRP it)) (VP^<S> "
            + "(VBD got) (S^<VP> (VP^<S> (VBD started) (PP^<VP> (IN on) (NP^<PP> (NP|<JJ-JJ>^<PP> (NP|<PRP$-JJ>^<PP> "
            + "(PRP$ its) (JJ reckless)) (JJ 190-point)) (NN plunge))))))))))))))) (. .)))";
    String f24parse6 = "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (NNS Economists)) (VP^<S> (AUX are) (VP^<VP> (VBN divided) (PP^<VP> "
            + "(IN as) (PP^<PP> (TO to) (SBAR^<PP> (WHADVP^<SBAR> (WRB how)) (S^<SBAR> (NP^<S> (NP^<NP> (NP|<JJ-NN>^<NP> "
            + "(JJ much) (NN manufacturing)) (NN strength)) (SBAR^<NP> (S^<SBAR> (NP^<S> (PRP they)) (VP^<S> (VBP "
            + "expect) (S^<VP> (VP^<S> (TO to) (VP^<VP> (VB see) (PP^<VP> (IN in) (NP^<PP> (NNP September)))))))))) "
            + "(VP^<S> (VBZ reports) (PP^<VP> (IN on) (NP^<PP> (NP|<RB-JJ>^<PP> (NP|<,-RB>^<PP> (NP|<NP-,>^<PP> (NP^<NP> "
            + "(NP|<CC-NN>^<NP> (NP|<NN-CC>^<NP> (NP|<JJ-NN>^<NP> (JJ industrial) (NN production)) (CC and)) (NN capacity)) "
            + "(NN utilization)) (, ,)) (RB also)) (JJ due)) (NN tomorrow))))))))))) (. .)))";
    String f24parse7 = "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (S|<,-NP>^<TOP> (S|<ADVP-,>^<TOP> (ADVP^<S> (RB Meanwhile)) (, ,)) (NP^<S> "
            + "(NP|<,-NP>^<S> (NP|<NP-,>^<S> (NP^<NP> (NP|<NNP-NN>^<NP> (NNP September) (NN housing)) (NNS starts)) "
            + "(, ,)) (NP^<NP> (JJ due) (NNP Wednesday))) (, ,))) (VP^<S> (AUX are) (VP^<VP> (VBN thought) (S^<VP> "
            + "(VP^<S> (TO to) (VP^<VP> (AUX have) (VP^<VP> (VBD inched) (ADVP^<VP> (RB upward))))))))) (. .)))";
    String f24parse8 = "(TOP (SINV^<TOP> (SINV|<VP-NP>^<TOP> (SINV|<,-VP>^<TOP> (SINV|<S-,>^<TOP> (S^<SINV> (S|<ADVP-NP>^<SINV> "
            + "(ADVP^<S> (RB UNK-LC)) (NP^<S> (EX There))) (VP^<S> (AUX 's) (NP^<VP> (NP^<NP> (DT a) (NN possibility)) "
            + "(PP^<NP> (IN of) (NP^<PP> (NP^<NP> (NP|<DT-NN>^<NP> (DT a) (NN surprise)) (NN UNK-LC)) (PP^<NP> (IN "
            + "in) (NP^<PP> (NP|<DT-NN>^<PP> (DT the) (NN trade)) (NN report)))))))) (, ,)) (VP^<SINV> (VBD said))) "
            + "(NP^<SINV> (NP|<NP-,>^<SINV> (NP^<NP> (NNP Michael) (NNP Englund)) (, ,)) (NP^<NP> (NP^<NP> (NN director)) "
            + "(PP^<NP> (IN of) (NP^<PP> (NP^<NP> (NN research)) (PP^<NP> (IN at) (NP^<PP> (NNP MMS)))))))) (. .)))";
    String f24parse9 = "(TOP (S^<TOP> (S|<CC-S>^<TOP> (S|<:-CC>^<TOP> (S|<S-:>^<TOP> (S^<S> (NP^<S> (NP|<,-SBAR>^<S> (NP|<PP-,>^<S> "
            + "(NP|<NP-PP>^<S> (NP^<NP> (DT A) (NN widening)) (PP^<NP> (IN of) (NP^<PP> (DT the) (NN deficit)))) (, "
            + ",)) (SBAR^<NP> (IN if) (S^<SBAR> (NP^<S> (PRP it)) (VP^<S> (AUX were) (VP^<VP> (VBN combined) (PP^<VP> "
            + "(IN with) (NP^<PP> (NP|<DT-ADJP>^<PP> (DT a) (ADJP^<NP> (RB stubbornly) (JJ strong))) (NN dollar)))))))) "
            + "(, ,)) (VP^<S> (MD would) (VP^<VP> (VB exacerbate) (NP^<VP> (NN trade) (NNS problems))))) (: --)) (CC "
            + "but)) (S^<S> (NP^<S> (DT the) (NN dollar)) (VP^<S> (VBD weakened) (SBAR^<VP> (S^<SBAR> (NP^<S> (NP^<NP> "
            + "(NNP Friday)) (PP^<NP> (IN as) (NP^<PP> (NNS stocks)))) (VP^<S> (VBD plummeted))))))) (. .)))";
    String f24parse10 = "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (S|<,-NP>^<TOP> (S|<PP-,>^<TOP> (PP^<S> (IN In) (NP^<PP> (DT any) (NN "
            + "event))) (, ,)) (NP^<S> (NP|<NP-CC>^<S> (NP^<NP> (NNP Mr.) (NNP Englund)) (CC and)) (NP^<NP> (JJ many) "
            + "(NNS others)))) (VP^<S> (VBP say) (SBAR^<VP> (IN that) (S^<SBAR> (NP^<S> (NP^<NP> (NP|<DT-JJ>^<NP> "
            + "(DT the) (JJ easy)) (NNS gains)) (PP^<NP> (IN in) (S^<PP> (VP^<S> (VBG narrowing) (NP^<VP> (NP|<DT-NN>^<VP> "
            + "(DT the) (NN trade)) (NN gap)))))) (VP^<S> (VP|<AUX-RB>^<S> (AUX have) (RB already)) (VP^<VP> (AUX "
            + "been) (VP^<VP> (VBN made)))))))) (. .)))";
    private static Grammar grammar;
    private static Parser parser;

    @BeforeClass
    public static void suiteSetUp() throws Exception {
        grammar = new Grammar(PCFG_FILE, LEX_FILE);
        parser = new ECPGramLoopBerkFilter(grammar);
    }

    @Test
    public void testSentence1() throws Exception {
        final ParseTree bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(f24sentence1);
        assertEquals(f24parse1, bestParseTree.toString());
    }

    @Test
    public void testSentence2() throws Exception {
        final ParseTree bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(f24sentence2);
        assertEquals(f24parse2, bestParseTree.toString());
    }

    @Test
    public void testSentence3() throws Exception {
        final ParseTree bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(f24sentence3);
        assertEquals(f24parse3, bestParseTree.toString());
    }

    @Test
    public void testSentence4() throws Exception {
        final ParseTree bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(f24sentence4);
        assertEquals(f24parse4, bestParseTree.toString());
    }

    @Test
    public void testSentence5() throws Exception {
        final ParseTree bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(f24sentence5);
        assertEquals(f24parse5, bestParseTree.toString());
    }

    @Test
    public void testSentence6() throws Exception {
        final ParseTree bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(f24sentence6);
        assertEquals(f24parse6, bestParseTree.toString());
    }

    @Test
    public void testSentence7() throws Exception {
        final ParseTree bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(f24sentence7);
        assertEquals(f24parse7, bestParseTree.toString());
    }

    @Test
    public void testSentence8() throws Exception {
        final ParseTree bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(f24sentence8);
        assertEquals(f24parse8, bestParseTree.toString());
    }

    @Test
    public void testSentence9() throws Exception {
        final ParseTree bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(f24sentence9);
        assertEquals(f24parse9, bestParseTree.toString());
    }

    @Test
    public void testSentence10() throws Exception {
        final ParseTree bestParseTree = ((MaximumLikelihoodParser) parser).findMLParse(f24sentence10);
        assertEquals(f24parse10, bestParseTree.toString());
    }
}
