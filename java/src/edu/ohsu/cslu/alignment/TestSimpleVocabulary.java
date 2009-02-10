package edu.ohsu.cslu.alignment;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

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
    private SimpleVocabulary sample;

    @Before
    public void setUp()
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append("(DT The) (NNS computers) (MD will) (VB display) (NN stock)");
        sb.append(" (NNS prices) (VBN selected) (IN by) (NNS users) (. .)\n");
        sb.append("(IN At) (JJS least) (RB not) (WRB when) (PRP you) (AUX are) (VBG ascending) (. .) (-RRB- -RRB-)");

        sample = SimpleVocabulary.induce(sb.toString());
    }

    @Test
    public void testSimpleVocabulary() throws Exception
    {
        checkSample(sample);
    }

    private void checkSample(SimpleVocabulary vocabulary)
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
    public void testInduceVocabularies() throws Exception
    {
        StringBuilder sb = new StringBuilder(256);
        sb.append("(DT The SIB) (NNS computers HEAD) (MD will HEAD) (VB display HEAD) (NN stock SIB)");
        sb.append(" (NNS prices HEAD) (VBN selected HEAD) (IN by SIB) (NNS users HEAD) (. . HEAD)\n");
        sb.append("(IN At SIB) (JJS least HEAD) (RB not SIB) (WRB when SIB) (PRP you HEAD)");
        sb.append(" (AUX are SIB) (VBG ascending HEAD) (. . HEAD) (-RRB- -RRB- SIB)");

        SimpleVocabulary[] vocabularies = SimpleVocabulary.induceVocabularies(sb.toString());

        assertEquals(3, vocabularies.length);
        assertEquals(16, vocabularies[0].size());
        assertEquals(19, vocabularies[1].size());
        assertEquals(3, vocabularies[2].size());

        assertEquals(0, vocabularies[0].map("-"));
        assertEquals(1, vocabularies[0].map("DT"));
        assertEquals("DT", vocabularies[0].map(1));

        assertEquals(5, vocabularies[0].map("NN"));
        assertEquals("NN", vocabularies[0].map(5));

        assertEquals(0, vocabularies[1].map("-"));
        assertEquals(1, vocabularies[1].map("The"));
        assertEquals("The", vocabularies[1].map(1));

        assertEquals(12, vocabularies[1].map("least"));
        assertEquals("least", vocabularies[1].map(12));

        assertEquals(0, vocabularies[2].map("-"));
        assertEquals(1, vocabularies[2].map("SIB"));
        assertEquals("SIB", vocabularies[2].map(1));

        assertEquals(2, vocabularies[2].map("HEAD"));
        assertEquals("HEAD", vocabularies[2].map(2));
    }

    @Test
    public void testWrite() throws Exception
    {
        StringWriter writer = new StringWriter();
        sample.write(writer);
        checkSample(SimpleVocabulary.read(new StringReader(writer.toString())));
    }
}
