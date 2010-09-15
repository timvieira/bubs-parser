package edu.ohsu.cslu.parser.chart;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.spmv.CscSpmvParser;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;
import static org.junit.Assert.assertEquals;

@RunWith(FilteredRunner.class)
public class ProfilePackedArrayChart {

    private static LeftCscSparseMatrixGrammar grammar;
    private static CscSpmvParser parser;

    @BeforeClass
    public static void suiteSetUp() throws IOException {
        if (grammar == null) {
            grammar = new LeftCscSparseMatrixGrammar(
                    SharedNlpTests.unitTestDataAsReader("grammars/berkeley.eng_sm6.nb.gz"));
            parser = new CscSpmvParser(new ParserDriver(), grammar);
        }

        parser.parseSentence("Views on manufacturing strength are split between economists who read"
                + " September 's low level of factory job growth as a sign of a slowdown and those"
                + " who use the somewhat more comforting total employment figures in their calculations .");
    }

    @Test
    @PerformanceTest({ "d820", "4000" })
    public void profileExtractBestParse() {
        final String expectedParse = "(ROOT_0 (S_0 (@S_24 (NP_25 (NP_24 (NNS_9 Views))"
                + " (PP_34 (IN_5 on) (NP_51 (VBG_27 manufacturing) (NN_23 strength))))"
                + " (VP_28 (VBP_13 are) (VP_7 (VBN_20 split) (PP_19 (IN_4 between)"
                + " (NP_8 (NP_49 (NNS_14 economists)) (SBAR_16 (WHNP_0 (WP_1 who))"
                + " (S_17 (VP_21 (@VP_13 (VBP_4 read) (NP_9 (NP_44 (@NP_4 (NP_3 (NNP_38 September)"
                + " (POS_1 's)) (JJ_29 low)) (NN_37 level)) (PP_31 (IN_14 of) (NP_50 (@NP_14 (NN_11 factory)"
                + " (NN_11 job)) (NN_22 growth))))) (PP_28 (IN_34 as) (NP_10 (NP_45 (DT_1 a) (NN_32 sign))"
                + " (PP_32 (IN_14 of) (NP_8 (@NP_46 (NP_43 (DT_0 a) (NN_31 slowdown)) (CC_5 and))"
                + " (NP_8 (NP_49 (DT_13 those)) (SBAR_16 (WHNP_0 (WP_1 who)) (S_17 (VP_21 (@VP_13 (VBP_4 use)"
                + " (NP_50 (@NP_14 (@NP_14 (@NP_9 (DT_2 the) (ADJP_22 (@ADJP_3 (RB_9 somewhat) (RBR_1 more))"
                + " (JJ_0 comforting))) (JJ_15 total)) (NN_8 employment)) (NNS_50 figures))) (PP_6 (IN_3 in)"
                + " (NP_51 (PRP$_1 their) (NNS_18 calculations))))))))))))))))))) (._3 .)))";
        for (int i = 0; i < 20; i++) {
            final ParseTree t = parser.chart.extractBestParse(grammar.startSymbol);
            assertEquals(expectedParse, t.toString());
        }
    }
}
