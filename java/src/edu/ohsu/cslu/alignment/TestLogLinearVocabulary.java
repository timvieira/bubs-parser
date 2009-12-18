package edu.ohsu.cslu.alignment;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Unit tests for {@link LogLinearVocabulary}
 * 
 * @author Aaron Dunlop
 * @since Apr 1, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestLogLinearVocabulary {

    private String sampleInput;
    private String stringSampleVocabulary;
    private LogLinearVocabulary sampleVocabulary;

    @Before
    public void setUp() throws IOException {
        StringBuilder sb = new StringBuilder(256);
        sb
            .append("(_pos_DT The _sib) (_pos_NNS computers _head) (_pos_MD will _head) (_pos_VB display _head) ");
        // Note missing space at end of line
        sb.append("(_pos_NN stock _sib) (_pos_NNS prices _head) (_pos_VBN selected _head) (_pos_IN by _sib)");
        sb.append("(_pos_NNS users _head) (_pos_. . _head) (_pos_IN At _sib) (_pos_JJS least _head) ");
        sb.append("(_pos_RB not _sib) (_pos_WRB when _sib) (_pos_PRP you _head) (_pos_AUX are _sib) ");
        sb.append("(_pos_VBG ascending _head) (_pos_. . _head) (_pos_-RRB- -RRB- _sib)");
        sampleInput = sb.toString();

        sb = new StringBuilder(512);
        sb.append("vocabulary size=36 categoryboundaries=19,34\n");
        sb.append("0 : _- : false\n");
        sb.append("1 : -RRB- : false\n");
        sb.append("2 : . : false\n");
        sb.append("3 : At : false\n");
        sb.append("4 : The : false\n");
        sb.append("5 : are : false\n");
        sb.append("6 : ascending : false\n");
        sb.append("7 : by : false\n");
        sb.append("8 : computers : false\n");
        sb.append("9 : display : false\n");
        sb.append("10 : least : false\n");
        sb.append("11 : not : false\n");
        sb.append("12 : prices : false\n");
        sb.append("13 : selected : false\n");
        sb.append("14 : stock : false\n");
        sb.append("15 : users : false\n");
        sb.append("16 : when : false\n");
        sb.append("17 : will : false\n");
        sb.append("18 : you : false\n");
        sb.append("19 : _pos_-RRB- : false\n");
        sb.append("20 : _pos_. : false\n");
        sb.append("21 : _pos_AUX : false\n");
        sb.append("22 : _pos_DT : false\n");
        sb.append("23 : _pos_IN : false\n");
        sb.append("24 : _pos_JJS : false\n");
        sb.append("25 : _pos_MD : false\n");
        sb.append("26 : _pos_NN : false\n");
        sb.append("27 : _pos_NNS : false\n");
        sb.append("28 : _pos_PRP : false\n");
        sb.append("29 : _pos_RB : false\n");
        sb.append("30 : _pos_VB : false\n");
        sb.append("31 : _pos_VBG : false\n");
        sb.append("32 : _pos_VBN : false\n");
        sb.append("33 : _pos_WRB : false\n");
        sb.append("34 : _head : false\n");
        sb.append("35 : _sib : false\n");
        stringSampleVocabulary = sb.toString();

        sampleVocabulary = LogLinearVocabulary.read(new StringReader(stringSampleVocabulary));
    }

    private void checkSampleVocabulary(final LogLinearVocabulary vocabulary) {
        assertEquals(36, vocabulary.size());

        final int[] categoryBoundaries = vocabulary.categoryBoundaries();
        assertArrayEquals("Wrong category boundary", new int[] { 19, 34 }, categoryBoundaries);

        assertEquals(1, vocabulary.map("-RRB-"));
        assertEquals("-RRB-", vocabulary.map(1));

        assertEquals(4, vocabulary.map("The"));
        assertEquals("The", vocabulary.map(4));

        assertEquals(28, vocabulary.map("_pos_PRP"));
        assertEquals("_pos_PRP", vocabulary.map(28));

        assertEquals(34, vocabulary.map("_head"));
        assertEquals("_head", vocabulary.map(34));
    }

    @Test
    public void testInduce() throws Exception {
        final LogLinearVocabulary vocabulary = LogLinearVocabulary.induce(sampleInput, 1);

        assertEquals(37, vocabulary.size());

        assertEquals(0, vocabulary.map("_-"));

        assertEquals(12, vocabulary.map("not"));
        assertEquals("not", vocabulary.map(12));

        assertEquals(5, vocabulary.map("The"));
        assertEquals("The", vocabulary.map(5));

        assertEquals(11, vocabulary.map("least"));
        assertEquals("least", vocabulary.map(11));

        assertEquals(36, vocabulary.map("_sib"));
        assertEquals("_sib", vocabulary.map(36));

        assertEquals(35, vocabulary.map("_head"));
        assertEquals("_head", vocabulary.map(35));

        assertTrue(vocabulary.isRareToken("ascending"));
        assertFalse(vocabulary.isRareToken("_pos_IN"));

        // This token should not be mapped, even though we skipped the space between two parentheses
        assertEquals(Integer.MIN_VALUE, vocabulary.map("_sib_pos_NNS"));

        assertArrayEquals("Wrong category boundary", new int[] { 21, 36 }, vocabulary.categoryBoundaries());
    }

    @Test
    public void testRead() throws Exception {
        checkSampleVocabulary(LogLinearVocabulary.read(new StringReader(stringSampleVocabulary)));
    }

    @Test
    public void testWrite() throws Exception {
        final StringWriter writer = new StringWriter();
        sampleVocabulary.write(writer);
        assertEquals(stringSampleVocabulary, writer.toString());
    }

}
