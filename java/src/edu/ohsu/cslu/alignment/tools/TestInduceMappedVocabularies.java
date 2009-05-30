package edu.ohsu.cslu.alignment.tools;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.TestSimpleVocabulary;
import edu.ohsu.cslu.common.tools.BaseCommandlineTool;
import edu.ohsu.cslu.common.tools.ToolTestCase;
import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Simple unit tests for {@link InduceMappedVocabularies}. The core functionality is tested in
 * {@link TestSimpleVocabulary}, so these tests are only cursory verification that the command-line
 * tool structure works properly.
 * 
 * @author Aaron Dunlop
 * @since Mar 25, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestInduceMappedVocabularies extends ToolTestCase
{

    @Test
    public void testInduceVocabularies() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("vocabulary size=7\n");
        sb.append("0 : _-\n");
        sb.append("1 : DT\n");
        sb.append("2 : NNS\n");
        sb.append("3 : MD\n");
        sb.append("4 : VB\n");
        sb.append("5 : NN\n");
        sb.append("6 : .\n");
        sb.append("\n");
        sb.append("vocabulary size=7\n");
        sb.append("0 : _-\n");
        sb.append("1 : The\n");
        sb.append("2 : computers\n");
        sb.append("3 : will\n");
        sb.append("4 : display\n");
        sb.append("5 : stock\n");
        sb.append("6 : .\n");
        sb.append("\n");
        sb.append("vocabulary size=3\n");
        sb.append("0 : _-\n");
        sb.append("1 : _sib\n");
        sb.append("2 : _head\n");
        sb.append("\n");

        String output = executeTool("",
            "(DT The _sib) (NNS computers _head) (MD will _head) (VB display _head) (NN stock _sib) (. . _head)");
        assertEquals(sb.toString(), output);
    }

    @Test
    public void testInduceLogLinearVocabulary() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        sb.append("vocabulary size=15 categoryboundaries=13\n");
        sb.append("0 : _-\n");
        sb.append("1 : -unk-\n");
        sb.append("2 : .\n");
        sb.append("3 : DT\n");
        sb.append("4 : MD\n");
        sb.append("5 : NN\n");
        sb.append("6 : NNS\n");
        sb.append("7 : The\n");
        sb.append("8 : VB\n");
        sb.append("9 : computers\n");
        sb.append("10 : display\n");
        sb.append("11 : stock\n");
        sb.append("12 : will\n");
        sb.append("13 : _head\n");
        sb.append("14 : _sib\n");
        sb.append("\n");

        String output = executeTool("-l",
            "(DT The _sib) (NNS computers _head) (MD will _head) (VB display _head) (NN stock _sib) (. . _head)");
        assertEquals(sb.toString(), output);
    }

    @Override
    protected BaseCommandlineTool tool()
    {
        return new InduceMappedVocabularies();
    }

}
