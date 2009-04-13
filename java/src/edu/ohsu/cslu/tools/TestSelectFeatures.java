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
        final String oneLineInput = "(<s> <s> before_head) (Ms. NNP before_head) (Haag NNP before_head) (plays VBZ head_verb) (Elianti NNP after_head) (. . after_head) (</s> </s> after_head)";

        // Select only a single feature
        assertEquals("(<s>) (Ms.) (Haag) (plays) (Elianti) (.) (</s>)\n",
            executeTool("-i bracketed -f 1", oneLineInput));
        assertEquals("(<s>) (Ms.) (Haag) (plays) (Elianti) (.) (</s>)\n", executeTool("-i bracketed -f 1 -o bracketed",
            oneLineInput));
        assertEquals("[<s>] [Ms.] [Haag] [plays] [Elianti] [.] [</s>]\n", executeTool("-f 1 -o square-bracketed",
            oneLineInput));
        assertEquals("<s> Ms. Haag plays Elianti . </s>\n", executeTool("-f 1 -o stanford", oneLineInput));

        // Select two features
        assertEquals(
            "(<s> before_head) (Ms. before_head) (Haag before_head) (plays head_verb) (Elianti after_head) (. after_head) (</s> after_head)\n",
            executeTool("-i bracketed -f 1,3", oneLineInput));
        assertEquals(
            "<s>/before_head Ms./before_head Haag/before_head plays/head_verb Elianti/after_head ./after_head </s>/after_head\n",
            executeTool("-i bracketed -f 1,3 -o stanford", oneLineInput));

        // Reverse input order
        assertEquals("(<s> <s>) (NNP Ms.) (NNP Haag) (VBZ plays) (NNP Elianti) (. .) (</s> </s>)\n", executeTool(
            "-i bracketed -f 2,1", oneLineInput));
        // Reverse and output in slash-delimited format
        assertEquals("<s>/<s> NNP/Ms. NNP/Haag VBZ/plays NNP/Elianti ./. </s>/</s>\n", executeTool(
            "-f 2,1 -o stanford", oneLineInput));

        // Now a few tests with two input lines
        final String twoLineInput = "(RB Sometimes before_head) (PRP they before_head) (AUX are head_verb) (JJ constructive after_head)"
            + " (, , after_head) (CC but after_head) (RB often after_head) (RB not after_head) (. . after_head)\n"
            + "(DT The before_head) (NNP Ontario before_head) (NNP Supreme before_head) (NNP Court before_head)"
            + " (VBD overturned head_verb) (NNP Mr. after_head) (NNP Blair after_head) (POS 's after_head) (NN decision after_head)"
            + " (. . after_head)";

        // Select only the word
        assertEquals("(Sometimes) (they) (are) (constructive)" + " (,) (but) (often) (not) (.)\n"
            + "(The) (Ontario) (Supreme) (Court)" + " (overturned) (Mr.) (Blair) ('s) (decision)" + " (.)\n",
            executeTool("-f 2", twoLineInput));

        // Select all features
        assertEquals(
            "(RB Sometimes before_head) (PRP they before_head) (AUX are head_verb) (JJ constructive after_head) (, , after_head)"
                + " (CC but after_head) (RB often after_head) (RB not after_head) (. . after_head)\n"
                + "(DT The before_head) (NNP Ontario before_head) (NNP Supreme before_head) (NNP Court before_head)"
                + " (VBD overturned head_verb) (NNP Mr. after_head) (NNP Blair after_head) (POS 's after_head) (NN decision after_head)"
                + " (. . after_head)\n", executeTool("-f 1,2,3", twoLineInput));
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

        // Test capitalization and head_verb (but not before_head or after_head)
        assertEquals("(Sometimes) (they) (are _head_verb) (constructive)" + " (,) (but) (often) (not) (.)\n",
            executeTool("-i bracketed-tree -w -h -cap", oneLineInput));

        // Test extracting word, pos, head from a bracketed tree
        assertEquals(
            "(sometimes _pos_RB _before_head) (they _pos_PRP _before_head) (are _pos_AUX _head_verb) (constructive _pos_JJ _after_head)"
                + " (, _pos_, _after_head) (but _pos_CC _after_head) (often _pos_RB _after_head) (not _pos_RB _after_head) (. _pos_. _after_head)\n",
            executeTool("-i bracketed-tree -w -p -h -bh -ah", oneLineInput));

        // Test extracting previous 2 words, subsequent 1 word
        assertEquals(
            "(sometimes _word+1_they) (they _word-1_sometimes _word+1_are) (are _word-1_they _word-2_sometimes _word+1_constructive)"
                + " (constructive _word-1_are _word-2_they _word+1_,) (, _word-1_constructive _word-2_are _word+1_but)"
                + " (but _word-1_, _word-2_constructive _word+1_often) (often _word-1_but _word-2_, _word+1_not)"
                + " (not _word-1_often _word-2_but _word+1_.) (. _word-1_not _word-2_often)\n", executeTool(
                "-i bracketed-tree -w -prevword 2 -subword 1", oneLineInput));

        // Test extracting previous word, previous and subsequent POS
        assertEquals(
            "(sometimes _pos+1_PRP) (they _word-1_sometimes _pos-1_RB _pos+1_AUX) (are _word-1_they _pos-1_PRP _pos+1_JJ)"
                + " (constructive _word-1_are _pos-1_AUX _pos+1_,) (, _word-1_constructive _pos-1_JJ _pos+1_CC)"
                + " (but _word-1_, _pos-1_, _pos+1_RB) (often _word-1_but _pos-1_CC _pos+1_RB)"
                + " (not _word-1_often _pos-1_RB _pos+1_.) (. _word-1_not _pos-1_RB)\n", executeTool(
                "-i bracketed-tree -w -prevword 1 -prevpos 1 -subpos 1", oneLineInput));

        // Select only the head features
        assertEquals(
            "(_before_head) (_before_head) (_head_verb) (_after_head) (_after_head) (_after_head) (_after_head) (_after_head) (_after_head)\n",
            executeTool("-i tree -h -bh -ah", oneLineInput));
        assertEquals(
            "_before_head _before_head _head_verb _after_head _after_head _after_head _after_head _after_head _after_head\n",
            executeTool("-i tree -h -bh -ah -o stanford", oneLineInput));

        // And from a square-bracketed tree
        assertEquals(
            "(sometimes _pos_RB _before_head) (they _pos_PRP _before_head) (are _pos_AUX _head_verb) (constructive _pos_JJ _after_head)"
                + " (, _pos_, _after_head) (but _pos_CC _after_head) (often _pos_RB _after_head) (not _pos_RB _after_head) (. _pos_. _after_head)\n",
            executeTool("-i square-bracketed-tree -w -p -h -bh -ah", oneLineInput.replaceAll("\\(", "[").replaceAll(
                "\\)", "]")));

        // Extract only word and head-verb feature from two lines, outputting a square-bracketed
        // format
        assertEquals(
            "[sometimes _before_head] [they _before_head] [are _head_verb] [constructive _after_head] [, _after_head]"
                + " [but _after_head] [often _after_head] [not _after_head] [. _after_head]\n"
                + "[the _before_head] [ontario _before_head] [supreme _before_head] [court _before_head] [overturned _head_verb]"
                + " [mr. _after_head] [blair _after_head] ['s _after_head] [decision _after_head] [. _after_head]\n",
            executeTool("-i tree -w -h -bh -ah -o square-bracketed", twoLineInput));

        // And just the word and plain POS, outputting in Stanford format
        assertEquals("sometimes/RB they/PRP are/AUX constructive/JJ ,/, but/CC often/RB not/RB ./.\n"
            + "the/DT ontario/NNP supreme/NNP court/NNP overturned/VBD mr./NNP blair/NNP 's/POS decision/NN ./.\n",
            executeTool("-i tree -w -ppos -o stanford", twoLineInput));
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
        String output = executeTool("-i tree -w -p -h -bh -ah -xt 2 -cap", input);
        assertEquals(expectedOutput, output);
    }

    @Override
    protected BaseCommandlineTool tool()
    {
        return new SelectFeatures();
    }
}
