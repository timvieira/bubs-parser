package edu.ohsu.cslu.alignment;

import static junit.framework.Assert.assertEquals;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;

/**
 * Basic unit tests for {@link SimpleVocabulary}
 * 
 * @author Aaron Dunlop
 * @since Oct 13, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestSimpleVocabulary
{
    private String sampleInput;
    private String stringSampleVocabulary;
    private SimpleVocabulary sampleVocabulary;

    @Before
    public void setUp()
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append("(DT The) (NNS computers) (MD will) (VB display) (NN stock)");
        sb.append(" (NNS prices) (VBN selected) (IN by) (NNS users) (. .)\n");
        sb.append("(IN At) (JJS least) (RB not) (WRB when) (PRP you) (AUX are) (VBG ascending) (. .) (-RRB- -RRB-)");
        sampleVocabulary = SimpleVocabulary.induce(sb.toString());

        sb = new StringBuilder(256);
        sb.append("(DT The _sib) (NNS computers _head) (MD will _head) (VB display _head) (NN stock _sib)");
        sb.append(" (NNS prices _head) (VBN selected _head) (IN by _sib) (NNS users _head) (. . _head)\n");
        sb.append("(IN At _sib) (JJS least _head) (RB not _sib) (WRB when _sib) (PRP you _head)");
        sb.append(" (AUX are _sib) (VBG ascending _head) (. . _head) (-RRB- -RRB- _sib)");
        sampleInput = sb.toString();

        sb = new StringBuilder(256);
        sb.append("vocabulary size=32\n");
        sb.append("0 : -\n");
        sb.append("1 : DT\n");
        sb.append("2 : The\n");
        sb.append("3 : NNS\n");
        sb.append("4 : computers\n");
        sb.append("5 : MD\n");
        sb.append("6 : will\n");
        sb.append("7 : VB\n");
        sb.append("8 : display\n");
        sb.append("9 : NN\n");
        sb.append("10 : stock\n");
        sb.append("11 : prices\n");
        sb.append("12 : VBN\n");
        sb.append("13 : selected\n");
        sb.append("14 : IN\n");
        sb.append("15 : by\n");
        sb.append("16 : users\n");
        sb.append("17 : .\n");
        sb.append("18 : At\n");
        sb.append("19 : JJS\n");
        sb.append("20 : least\n");
        sb.append("21 : RB\n");
        sb.append("22 : not\n");
        sb.append("23 : WRB\n");
        sb.append("24 : when\n");
        sb.append("25 : PRP\n");
        sb.append("26 : you\n");
        sb.append("27 : AUX\n");
        sb.append("28 : are\n");
        sb.append("29 : VBG\n");
        sb.append("30 : ascending\n");
        sb.append("31 : -RRB-\n");
        sb.append("\n");
        stringSampleVocabulary = sb.toString();
    }

    @Test
    public void testSampleVocabulary() throws Exception
    {
        checkSampleVocabulary(sampleVocabulary);
    }

    private void checkSampleVocabulary(SimpleVocabulary vocabulary)
    {
        assertEquals(32, vocabulary.size());

        assertEquals(1, vocabulary.map("DT"));
        assertEquals("DT", vocabulary.map(1));

        assertEquals(2, vocabulary.map("The"));
        assertEquals("The", vocabulary.map(2));

        assertEquals(28, vocabulary.map("are"));
        assertEquals("are", vocabulary.map(28));
    }

    @Test
    public void testThreeVocabularies() throws Exception
    {
        SimpleVocabulary[] vocabularies = SimpleVocabulary.induceVocabularies(sampleInput);

        assertEquals(3, vocabularies.length);
        assertEquals(16, vocabularies[0].size());
        assertEquals(19, vocabularies[1].size());
        assertEquals(3, vocabularies[2].size());

        assertEquals(0, vocabularies[0].map("_-"));
        assertEquals(1, vocabularies[0].map("DT"));
        assertEquals("DT", vocabularies[0].map(1));

        assertEquals(5, vocabularies[0].map("NN"));
        assertEquals("NN", vocabularies[0].map(5));

        assertEquals(0, vocabularies[1].map("_-"));
        assertEquals(1, vocabularies[1].map("The"));
        assertEquals("The", vocabularies[1].map(1));

        assertEquals(12, vocabularies[1].map("least"));
        assertEquals("least", vocabularies[1].map(12));

        assertEquals(0, vocabularies[2].map("_-"));
        assertEquals(1, vocabularies[2].map("_sib"));
        assertEquals("_sib", vocabularies[2].map(1));

        assertEquals(2, vocabularies[2].map("_head"));
        assertEquals("_head", vocabularies[2].map(2));
    }

    @Test
    public void testRead() throws Exception
    {
        checkSampleVocabulary(SimpleVocabulary.read(new StringReader(stringSampleVocabulary)));
    }

    @Test
    public void testWrite() throws Exception
    {
        StringWriter writer = new StringWriter();
        sampleVocabulary.write(writer);
        checkSampleVocabulary(SimpleVocabulary.read(new StringReader(writer.toString())));
    }
}
