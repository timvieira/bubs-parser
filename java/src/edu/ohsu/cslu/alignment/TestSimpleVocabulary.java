package edu.ohsu.cslu.alignment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Basic unit tests for {@link SimpleVocabulary}
 * 
 * @author Aaron Dunlop
 * @since Oct 13, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestSimpleVocabulary {

    private String sampleInput;
    private String stringSampleVocabulary;
    private SimpleVocabulary sampleVocabulary;

    @Before
    public void setUp() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("(DT The) (NNS computers) (MD will) (VB display) (NN stock)");
        sb.append(" (NNS prices) (VBN selected) (IN by) (NNS users) (. .)\n");
        sb
            .append("(IN At) (JJS least) (RB not) (WRB when) (PRP you) (AUX are) (VBG ascending) (. .) (-RRB- -RRB-)");
        sampleVocabulary = SimpleVocabulary.induce(sb.toString(), 1);

        sb = new StringBuilder(256);
        sb.append("(DT The _sib) (NNS computers _head) (MD will _head) (VB display _head) (NN stock _sib)");
        sb.append(" (NNS prices _head) (VBN selected _head) (IN by _sib) (NNS users _head) (. . _head)\n");
        sb.append("(IN At _sib) (JJS least _head) (RB not _sib) (WRB when _sib) (PRP you _head)");
        sb.append(" (AUX are _sib) (VBG ascending _head) (. . _head) (-RRB- -RRB- _sib)");
        sampleInput = sb.toString();

        sb = new StringBuilder(256);
        sb.append("vocabulary size=33\n");
        sb.append("0 : _- : true\n");
        sb.append("1 : -unk- : true\n");
        sb.append("2 : DT : true\n");
        sb.append("3 : The : true\n");
        sb.append("4 : NNS : true\n");
        sb.append("5 : computers : true\n");
        sb.append("6 : MD : true\n");
        sb.append("7 : will : true\n");
        sb.append("8 : VB : true\n");
        sb.append("9 : display : true\n");
        sb.append("10 : NN : true\n");
        sb.append("11 : stock : true\n");
        sb.append("12 : prices : true\n");
        sb.append("13 : VBN : true\n");
        sb.append("14 : selected : true\n");
        sb.append("15 : IN : false\n");
        sb.append("16 : by : false\n");
        sb.append("17 : users : true\n");
        sb.append("18 : . : true\n");
        sb.append("19 : At : true\n");
        sb.append("20 : JJS : true\n");
        sb.append("21 : least : true\n");
        sb.append("22 : RB : true\n");
        sb.append("23 : not : true\n");
        sb.append("24 : WRB : true\n");
        sb.append("25 : when : true\n");
        sb.append("26 : PRP : true\n");
        sb.append("27 : you : true\n");
        sb.append("28 : AUX : true\n");
        sb.append("29 : are : true\n");
        sb.append("30 : VBG : true\n");
        sb.append("31 : ascending : true\n");
        sb.append("32 : -RRB- : -RRB-\n");
        sb.append("\n");
        stringSampleVocabulary = sb.toString();
    }

    @Test
    public void testSampleVocabulary() throws Exception {
        checkSampleVocabulary(sampleVocabulary);
    }

    private void checkSampleVocabulary(SimpleVocabulary vocabulary) {
        assertEquals(33, vocabulary.size());

        assertEquals(2, vocabulary.map("DT"));
        assertEquals("DT", vocabulary.map(2));

        assertEquals(3, vocabulary.map("The"));
        assertEquals("The", vocabulary.map(3));

        assertEquals(29, vocabulary.map("are"));
        assertEquals("are", vocabulary.map(29));

        assertTrue(vocabulary.isRareToken("prices"));
        assertFalse(vocabulary.isRareToken("IN"));
    }

    @Test
    public void testThreeVocabularies() throws Exception {
        SimpleVocabulary[] vocabularies = SimpleVocabulary.induceVocabularies(sampleInput);

        assertEquals(3, vocabularies.length);
        assertEquals(17, vocabularies[0].size());
        assertEquals(20, vocabularies[1].size());
        assertEquals(4, vocabularies[2].size());

        assertEquals(0, vocabularies[0].map("_-"));
        assertEquals(2, vocabularies[0].map("DT"));
        assertEquals("DT", vocabularies[0].map(2));

        assertEquals(6, vocabularies[0].map("NN"));
        assertEquals("NN", vocabularies[0].map(6));

        assertEquals(0, vocabularies[1].map("_-"));
        assertEquals(2, vocabularies[1].map("The"));
        assertEquals("The", vocabularies[1].map(2));

        assertEquals(13, vocabularies[1].map("least"));
        assertEquals("least", vocabularies[1].map(13));

        assertEquals(0, vocabularies[2].map("_-"));
        assertEquals(2, vocabularies[2].map("_sib"));
        assertEquals("_sib", vocabularies[2].map(2));

        assertEquals(3, vocabularies[2].map("_head"));
        assertEquals("_head", vocabularies[2].map(3));
    }

    @Test
    public void testRead() throws Exception {
        checkSampleVocabulary(SimpleVocabulary.read(new StringReader(stringSampleVocabulary)));
    }

    @Test
    public void testWrite() throws Exception {
        StringWriter writer = new StringWriter();
        sampleVocabulary.write(writer);
        checkSampleVocabulary(SimpleVocabulary.read(new StringReader(writer.toString())));
    }
}
