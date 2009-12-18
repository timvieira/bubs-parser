package edu.ohsu.cslu.alignment.tools;

import org.junit.Test;
import org.junit.runner.RunWith;

import cltool.ToolTestCase;
import edu.ohsu.cslu.alignment.TestSimpleVocabulary;
import edu.ohsu.cslu.tests.FilteredRunner;

import static junit.framework.Assert.assertEquals;

/**
 * Simple unit tests for {@link InduceMappedVocabularies}. The core functionality is tested in
 * {@link TestSimpleVocabulary}, so these tests are only cursory verification that the command-line tool
 * structure works properly.
 * 
 * @author Aaron Dunlop
 * @since Mar 25, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestInduceMappedVocabularies extends ToolTestCase {

    @Test
    public void testInduceVocabularies() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("vocabulary size=8\n");
        sb.append("0 : _- : false\n");
        sb.append("1 : -unk- : false\n");
        sb.append("2 : DT : false\n");
        sb.append("3 : NNS : false\n");
        sb.append("4 : MD : false\n");
        sb.append("5 : VB : false\n");
        sb.append("6 : NN : false\n");
        sb.append("7 : . : false\n");
        sb.append("\n");
        sb.append("vocabulary size=8\n");
        sb.append("0 : _- : false\n");
        sb.append("1 : -unk- : false\n");
        sb.append("2 : The : false\n");
        sb.append("3 : computers : false\n");
        sb.append("4 : will : false\n");
        sb.append("5 : display : false\n");
        sb.append("6 : stock : false\n");
        sb.append("7 : . : false\n");
        sb.append("\n");
        sb.append("vocabulary size=4\n");
        sb.append("0 : _- : false\n");
        sb.append("1 : -unk- : false\n");
        sb.append("2 : _sib : false\n");
        sb.append("3 : _head : false\n");
        sb.append("\n");

        String output = executeTool(new InduceMappedVocabularies(), "",
            "(DT The _sib) (NNS computers _head) (MD will _head) (VB display _head) (NN stock _sib) (. . _head)");
        assertEquals(sb.toString(), output);
    }

    @Test
    public void testInduceLogLinearVocabulary() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("vocabulary size=15 categoryboundaries=14\n");
        sb.append("0 : _- : true\n");
        sb.append("1 : -unk- : true\n");
        sb.append("2 : . : false\n");
        sb.append("3 : DT : false\n");
        sb.append("4 : MD : false\n");
        sb.append("5 : NN : false\n");
        sb.append("6 : NNS : false\n");
        sb.append("7 : The : false\n");
        sb.append("8 : VB : false\n");
        sb.append("9 : computers : false\n");
        sb.append("10 : display : false\n");
        sb.append("11 : stock : false\n");
        sb.append("12 : will : false\n");
        sb.append("13 : _head : false\n");
        sb.append("14 : _sib : false\n");
        sb.append("\n");

        String output = executeTool(new InduceMappedVocabularies(), "-l",
            "(DT The _sib) (NNS computers _head) (MD will _head) (VB display _head) (NN stock _sib) (. . _head)");
        assertEquals(sb.toString(), output);
    }
}
