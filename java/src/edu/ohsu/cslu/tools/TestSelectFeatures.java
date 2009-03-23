package edu.ohsu.cslu.tools;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import org.apache.commons.cli.ParseException;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.common.tools.BaseCommandlineTool;
import edu.ohsu.cslu.common.tools.LinewiseCommandlineTool;
import edu.ohsu.cslu.common.tools.ToolTestCase;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Unit tests for {@link SelectFeatures}. Also a bit of a playground to try out new JUnit features.
 * 
 * @author Aaron Dunlop
 * @since Dec 19, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(Theories.class)
public class TestSelectFeatures extends ToolTestCase
{
    @DataPoint
    public final static String BAD_ARGS_1 = "-i bracketed -w";
    @DataPoint
    public final static String BAD_ARGS_2 = "-i bracketed -p";
    @DataPoint
    public final static String BAD_ARGS_3 = "-i bracketed-tree -f 1,2";

    @Theory
    public void testBadArgs(String args) throws Exception
    {
        // @Theory doesn't support (expected=...) so we have to use the old pattern of expecting an
        // Exception
        try
        {
            executeTool(args, "(Bracketed) (input)");
            fail("Expected ParseException");
        }
        catch (ParseException expected)
        {}
    }

    /**
     * Tests selecting features from flat, bracketed input.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testBracketedInput() throws Exception
    {
        final String oneLineInput = "(<s> <s> BEFORE) (Ms. NNP BEFORE) (Haag NNP BEFORE) (plays VBZ HEAD) (Elianti NNP AFTER) (. . AFTER) (</s> </s> AFTER)";

        // Select only a single feature
        assertEquals("(<s>) (Ms.) (Haag) (plays) (Elianti) (.) (</s>)\n",
            executeTool("-i bracketed -f 1", oneLineInput));
        assertEquals("(<s>) (Ms.) (Haag) (plays) (Elianti) (.) (</s>)\n", executeTool("-i bracketed -f 1 -o bracketed",
            oneLineInput));
        assertEquals("[<s>] [Ms.] [Haag] [plays] [Elianti] [.] [</s>]\n", executeTool("-f 1 -o square-bracketed",
            oneLineInput));
        assertEquals("<s> Ms. Haag plays Elianti . </s>\n", executeTool("-f 1 -o stanford", oneLineInput));

        // Select two features
        assertEquals("(<s> BEFORE) (Ms. BEFORE) (Haag BEFORE) (plays HEAD) (Elianti AFTER) (. AFTER) (</s> AFTER)\n",
            executeTool("-i bracketed -f 1,3", oneLineInput));
        assertEquals("<s>/BEFORE Ms./BEFORE Haag/BEFORE plays/HEAD Elianti/AFTER ./AFTER </s>/AFTER\n", executeTool(
            "-i bracketed -f 1,3 -o stanford", oneLineInput));

        // Reverse input order
        assertEquals("(<s> <s>) (NNP Ms.) (NNP Haag) (VBZ plays) (NNP Elianti) (. .) (</s> </s>)\n", executeTool(
            "-i bracketed -f 2,1", oneLineInput));
        // Reverse and output in slash-delimited format
        assertEquals("<s>/<s> NNP/Ms. NNP/Haag VBZ/plays NNP/Elianti ./. </s>/</s>\n", executeTool(
            "-f 2,1 -o stanford", oneLineInput));

        // Now a few tests with two input lines
        final String twoLineInput = "(RB Sometimes BEFORE) (PRP they BEFORE) (AUX are HEAD) (JJ constructive AFTER)"
            + " (, , AFTER) (CC but AFTER) (RB often AFTER) (RB not AFTER) (. . AFTER)\n"
            + "(DT The BEFORE) (NNP Ontario BEFORE) (NNP Supreme BEFORE) (NNP Court BEFORE)"
            + " (VBD overturned HEAD) (NNP Mr. AFTER) (NNP Blair AFTER) (POS 's AFTER) (NN decision AFTER)"
            + " (. . AFTER)";

        // Select only the word
        assertEquals("(Sometimes) (they) (are) (constructive)" + " (,) (but) (often) (not) (.)\n"
            + "(The) (Ontario) (Supreme) (Court)" + " (overturned) (Mr.) (Blair) ('s) (decision)" + " (.)\n",
            executeTool("-f 2", twoLineInput));

        // Select all features
        assertEquals("(RB Sometimes BEFORE) (PRP they BEFORE) (AUX are HEAD) (JJ constructive AFTER) (, , AFTER)"
            + " (CC but AFTER) (RB often AFTER) (RB not AFTER) (. . AFTER)\n"
            + "(DT The BEFORE) (NNP Ontario BEFORE) (NNP Supreme BEFORE) (NNP Court BEFORE)"
            + " (VBD overturned HEAD) (NNP Mr. AFTER) (NNP Blair AFTER) (POS 's AFTER) (NN decision AFTER)"
            + " (. . AFTER)\n", executeTool("-f 1,2,3", twoLineInput));

    }

    /**
     * Tests selecting features from flat, bracketed input.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testTreeInput() throws Exception
    {
        final String oneLineInput = "(TOP (S (S (ADVP (RB Sometimes)) (NP (PRP they)) (VP (AUX are)"
            + " (ADJP (JJ constructive)))) (, ,) (CC but) (FRAG (ADVP (RB often)) (RB not)) (. .)))\n";
        final String twoLineInput = "(TOP (S (S (ADVP (RB Sometimes)) (NP (PRP they)) (VP (AUX are)"
            + " (ADJP (JJ constructive)))) (, ,) (CC but) (FRAG (ADVP (RB often)) (RB not)) (. .)))\n"
            + "(TOP (S (NP (DT The) (NNP Ontario) (NNP Supreme) (NNP Court)) (VP (VBD overturned)"
            + " (NP (NP (NNP Mr.) (NNP Blair) (POS 's)) (NN decision))) (. .)))";

        // Test extracting all features from a bracketed tree
        assertEquals("(Sometimes RB BEFORE) (they PRP BEFORE) (are AUX HEAD) (constructive JJ AFTER)"
            + " (, , AFTER) (but CC AFTER) (often RB AFTER) (not RB AFTER) (. . AFTER)\n", executeTool(
            "-i bracketed-tree -w -p -h", oneLineInput));

        // Select only the head feature
        assertEquals("(BEFORE) (BEFORE) (HEAD) (AFTER) (AFTER) (AFTER) (AFTER) (AFTER) (AFTER)\n", executeTool(
            "-i tree -h", oneLineInput));
        assertEquals("BEFORE BEFORE HEAD AFTER AFTER AFTER AFTER AFTER AFTER\n", executeTool("-i tree -h -o stanford",
            oneLineInput));

        // And from a square-bracketed tree
        assertEquals("(Sometimes RB BEFORE) (they PRP BEFORE) (are AUX HEAD) (constructive JJ AFTER)"
            + " (, , AFTER) (but CC AFTER) (often RB AFTER) (not RB AFTER) (. . AFTER)\n", executeTool(
            "-i square-bracketed-tree -w -p -h", oneLineInput.replaceAll("\\(", "[").replaceAll("\\)", "]")));

        // Extract only word and head-verb feature from two lines, outputting a square-bracketed
        // format
        assertEquals("[Sometimes BEFORE] [they BEFORE] [are HEAD] [constructive AFTER] [, AFTER]"
            + " [but AFTER] [often AFTER] [not AFTER] [. AFTER]\n"
            + "[The BEFORE] [Ontario BEFORE] [Supreme BEFORE] [Court BEFORE] [overturned HEAD]"
            + " [Mr. AFTER] [Blair AFTER] ['s AFTER] [decision AFTER] [. AFTER]\n", executeTool(
            "-i tree -w -h -o square-bracketed", twoLineInput));

        // And just the word and POS, outputting in Stanford format
        assertEquals("Sometimes/RB they/PRP are/AUX constructive/JJ ,/, but/CC often/RB not/RB ./.\n"
            + "The/DT Ontario/NNP Supreme/NNP Court/NNP overturned/VBD Mr./NNP Blair/NNP 's/POS decision/NN ./.\n",
            executeTool("-i tree -w -p -o stanford", twoLineInput));
    }

    /**
     * Tests running the tool with multiple threads. Not a very useful scenario for
     * {@link SelectFeatures}, but a reasonable test for the threading of
     * {@link LinewiseCommandlineTool}.
     * 
     * @throws Exception
     */
    @Test
    public void testMultiThreaded() throws Exception
    {
        String input = new String(SharedNlpTests.readUnitTestData("tools/select-features.input"));
        String expectedOutput = new String(SharedNlpTests.readUnitTestData("tools/select-features.output"));
        String output = executeTool("-i tree -w -p -h -xt 2", input);
        assertEquals(expectedOutput, output);
    }

    @Override
    protected BaseCommandlineTool tool()
    {
        return new SelectFeatures();
    }
}
