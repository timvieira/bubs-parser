package edu.ohsu.cslu.datastructs.narytree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.tests.FilteredRunner;

import static junit.framework.Assert.*;

/**
 * Unit tests for {@link ParseTree}
 * 
 * TODO: Update when InducedGrammar can handle real words instead of Wpos
 * 
 * @author Aaron Dunlop
 * @since Sep 24, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestParseTree {

    private static Vocabulary vocabulary;

    private ParseTree sampleTree;
    private String stringSampleTree;

    private final static String[] SAMPLE_IN_ORDER_ARRAY = new String[] { "Wdt", "DT", "NP", "Wnnp", "NNP", "Wnn", "NN", "S", "Wvbd", "VBD", "W.", ".", "TOP" };
    private final static String[] SAMPLE_PRE_ORDER_ARRAY = new String[] { "TOP", "S", "NP", "DT", "Wdt", "NNP", "Wnnp", "NN", "Wnn", "VBD", "Wvbd", ".", "W." };
    private final static String[] SAMPLE_POST_ORDER_ARRAY = new String[] { "Wdt", "DT", "Wnnp", "NNP", "Wnn", "NN", "NP", "Wvbd", "VBD", "W.", ".", "S", "TOP" };

    @BeforeClass
    public static void suiteSetUp() {
        if (vocabulary == null) {
            vocabulary = SimpleVocabulary.induce("NP S TOP VP DT Wdt NNP Wnnp NN Wnn VBD Wvbd . W.");
        }
    }

    @Before
    public void setUp() {
        sampleTree = new ParseTree("TOP", vocabulary);

        final ParseTree s = sampleTree.addChild("S");
        final ParseTree np = s.addChild("NP");
        np.addChild("DT").addChild("Wdt");
        np.addChild("NNP").addChild("Wnnp");
        np.addChild("NN").addChild("Wnn");

        s.addChild("VBD").addChild("Wvbd");
        s.addChild(".").addChild("W.");

        stringSampleTree = "(TOP (S (NP (DT Wdt) (NNP Wnnp) (NN Wnn)) (VBD Wvbd) (. W.)))";
    }

    @Test
    public void testAddChild() throws Exception {
        final ParseTree tree = new ParseTree("TOP", vocabulary);
        assertEquals(1, tree.size());
        tree.addChild("S");
        assertEquals(2, tree.size());
        assertNull(tree.subtree("NP"));
        assertNotNull(tree.subtree("S"));
    }

    @Test
    public void testAddChildren() throws Exception {
        final ParseTree tree = new ParseTree("TOP", vocabulary);
        assertEquals(1, tree.size());
        tree.addChildren(new String[] { "S", "." });
        assertEquals(3, tree.size());
        assertNull(tree.subtree("TOP"));
        assertNotNull(tree.subtree("S"));
        assertNotNull(tree.subtree("."));
    }

    @Test
    public void testAddSubtree() throws Exception {
        final ParseTree tree = new ParseTree("TOP", vocabulary);
        final ParseTree s = new ParseTree("S", vocabulary);
        s.addChildren(new String[] { "NP", "VBD" });
        tree.addSubtree(s);
        assertEquals(4, tree.size());
        assertNotNull(tree.subtree("S"));
        assertNotNull(tree.subtree("S").subtree("NP"));
        assertNotNull(tree.subtree("S").subtree("VBD"));
    }

    @Test
    public void testRemoveChild() throws Exception {
        assertEquals(13, sampleTree.size());
        final ParseTree s = sampleTree.subtree("S");

        s.removeChild("VBD");
        assertEquals(12, sampleTree.size());

        // Removing the 'NP' node should move its children up
        assertNull(s.subtree("DT"));
        assertNull(s.subtree("NNP"));
        assertNull(s.subtree("NN"));

        s.removeChild("NP");
        assertEquals(11, sampleTree.size());
        assertNotNull(s.subtree("DT"));
        assertNotNull(s.subtree("NNP"));
        assertNotNull(s.subtree("NN"));

        // TODO: Validate that the children were inserted at the proper place with iteration order
    }

    @Test
    public void testRemoveChildrenByStringArray() throws Exception {
        final ParseTree s = sampleTree.subtree("S");
        assertEquals(12, s.size());

        assertNull(s.subtree("DT"));
        assertNull(s.subtree("NNP"));

        // Removing the "NP" node should move its children up
        s.removeChildren(new String[] { "NP", "VBD" });
        assertEquals(10, s.size());

        assertNotNull(s.subtree("DT"));
        assertNotNull(s.subtree("NNP"));
    }

    @Test
    public void testRemoveSubtree() throws Exception {
        final ParseTree s = sampleTree.subtree("S");
        assertEquals(12, s.size());

        s.removeSubtree(".");
        assertEquals(10, s.size());

        // Removing the "NP" node should remove all its children as well
        s.removeSubtree("NP");
        assertEquals(3, s.size());
        assertNull(s.subtree("NP"));
        assertNull(s.subtree("DT"));
    }

    @Test
    public void testSubtree() throws Exception {
        final BaseNaryTree<String> s = sampleTree.subtree("S");
        assertEquals(12, s.size());
        assertNotNull(s.subtree("NP"));
        assertNotNull(s.subtree("VBD"));
        assertEquals(7, s.subtree("NP").size());

        assertEquals(2, s.subtree("VBD").size());
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(13, sampleTree.size());
        sampleTree.subtree("S").removeSubtree("VBD");
        assertEquals(11, sampleTree.size());
        sampleTree.subtree("S").addChild("PRP");
        assertEquals(12, sampleTree.size());
    }

    @Test
    public void testDepthFromRoot() throws Exception {
        assertEquals(0, sampleTree.depthFromRoot());
        assertEquals(1, sampleTree.subtree("S").depthFromRoot());
        assertEquals(2, sampleTree.subtree("S").subtree("NP").depthFromRoot());
        assertEquals(3, sampleTree.subtree("S").subtree("NP").subtree("NNP").depthFromRoot());
    }

    @Test
    public void testLeaves() throws Exception {
        assertEquals(5, sampleTree.leaves());
        assertEquals(5, sampleTree.subtree("S").leaves());
        assertEquals(3, sampleTree.subtree("S").subtree("NP").leaves());

        sampleTree.subtree("S").addChild(";");
        assertEquals(6, sampleTree.leaves());

        sampleTree.subtree("S").removeChild(";");
        assertEquals(5, sampleTree.leaves());

        sampleTree.subtree("S").subtree("VBD").removeSubtree("Wvbd");
        assertEquals(5, sampleTree.leaves());

        sampleTree.subtree("S").removeSubtree("NP");
        assertEquals(2, sampleTree.leaves());
    }

    @Test
    public void testInOrderIterator() throws Exception {
        final Iterator<NaryTree<String>> iter = sampleTree.inOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            final BaseNaryTree<String> tree = (BaseNaryTree<String>) iter.next();
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i], tree.stringLabel());
        }
    }

    @Test
    public void testInOrderLabelIterator() throws Exception {
        final Iterator<String> iter = sampleTree.inOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i], iter.next());
        }
    }

    @Test
    public void testPreOrderIterator() throws Exception {
        final Iterator<NaryTree<String>> iter = sampleTree.preOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            final BaseNaryTree<String> tree = (BaseNaryTree<String>) iter.next();
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i], tree.stringLabel());
        }
    }

    @Test
    public void testPreOrderLabelIterator() throws Exception {
        final Iterator<String> iter = sampleTree.preOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i], iter.next());
        }
    }

    @Test
    public void testPostOrderIterator() throws Exception {
        final Iterator<NaryTree<String>> iter = sampleTree.postOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            final BaseNaryTree<String> tree = (BaseNaryTree<String>) iter.next();
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i], tree.stringLabel());
        }
    }

    @Test
    public void testPostOrderLabelIterator() throws Exception {
        final Iterator<String> iter = sampleTree.postOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i], iter.next());
        }
    }

    @Test
    public void testReadFromReader() throws Exception {

        final String stringSimpleTree = "(TOP (NP VP) .)";
        final ParseTree simpleTree = ParseTree.read(new StringReader(stringSimpleTree), vocabulary);
        assertEquals(4, simpleTree.size());
        assertEquals(2, simpleTree.subtree("NP").size());

        assertEquals("TOP", simpleTree.label());
        assertEquals("NP", simpleTree.subtree("NP").label());
        assertEquals("VP", simpleTree.subtree("NP").subtree("VP").label());
        assertEquals(".", simpleTree.subtree(".").label());

        final String stringTestTree = "(TOP (S (NP (DT Wdt) (NNP Wnnp) (NN Wnn)) (VBD Wvbd) (. W.)))";
        final ParseTree testTree = ParseTree.read(new StringReader(stringTestTree), vocabulary);
        assertEquals(13, testTree.size());
        assertEquals(7, testTree.subtree("S").subtree("NP").size());

        assertEquals("TOP", testTree.label());
        assertEquals("S", testTree.subtree("S").label());
        assertEquals("NP", testTree.subtree("S").subtree("NP").label());
        assertEquals("DT", testTree.subtree("S").subtree("NP").subtree("DT").label());
        assertEquals("Wdt", testTree.subtree("S").subtree("NP").subtree("DT").subtree("Wdt").label());
        assertEquals("NNP", testTree.subtree("S").subtree("NP").subtree("NNP").label());
        assertEquals("Wnnp", testTree.subtree("S").subtree("NP").subtree("NNP").subtree("Wnnp").label());
        assertEquals("NN", testTree.subtree("S").subtree("NP").subtree("NN").label());
        assertEquals("Wnn", testTree.subtree("S").subtree("NP").subtree("NN").subtree("Wnn").label());
        assertEquals("Wdt", testTree.subtree("S").subtree("NP").subtree("DT").subtree("Wdt").label());
        assertEquals("VBD", testTree.subtree("S").subtree("VBD").label());
        assertEquals("Wvbd", testTree.subtree("S").subtree("VBD").subtree("Wvbd").label());
        assertEquals(".", testTree.subtree("S").subtree(".").label());
        assertEquals("W.", testTree.subtree("S").subtree(".").subtree("W.").label());

        final ParseTree tree = ParseTree.read(new StringReader(stringSampleTree), vocabulary);
        assertEquals(sampleTree, tree);
    }

    @Test
    public void testWriteToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        sampleTree.write(writer);
        assertEquals(stringSampleTree, writer.toString());

        final String stringSimpleTree = "(TOP (NP (NP (NP (NN Wnn) (NN Wnn)) (NN Wnn)) (NN Wnn)))";
        final ParseTree tree = new ParseTree("TOP", vocabulary);
        tree.addChild("NP").addChild("NP").addChild("NP").addChild("NN").addChild("Wnn");
        tree.subtree("NP").subtree("NP").subtree("NP").addChild("NN").addChild("Wnn");
        tree.subtree("NP").subtree("NP").addChild("NN").addChild("Wnn");
        tree.subtree("NP").addChild("NN").addChild("Wnn");

        writer = new StringWriter();
        tree.write(writer);
        assertEquals(stringSimpleTree, writer.toString());
    }

    @Test
    public void testIsLeaf() throws Exception {
        assertFalse(sampleTree.isLeaf());
        assertFalse(sampleTree.subtree("S").isLeaf());
        assertTrue(sampleTree.subtree("S").subtree("VBD").subtree("Wvbd").isLeaf());
        assertTrue(sampleTree.subtree("S").subtree(".").subtree("W.").isLeaf());
    }

    @Test
    public void testEquals() throws Exception {
        final BaseNaryTree<String> tree1 = new ParseTree("TOP", vocabulary);
        tree1.addChildren(new String[] { "S", "." });

        final BaseNaryTree<String> tree2 = new ParseTree("TOP", vocabulary);
        tree2.addChildren(new String[] { "S", "." });

        final BaseNaryTree<String> tree3 = new ParseTree("TOP", vocabulary);
        tree3.addChildren(new String[] { "VBD", "." });

        assertTrue(tree1.equals(tree2));
        assertFalse(tree1.equals(tree3));

        tree2.subtree("S").addChild("VBD");
        assertFalse(tree1.equals(tree2));
    }

    /**
     * Tests Java serialization and deserialization of trees
     * 
     * @throws Exception if something bad happens
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSerialize() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(sampleTree);

        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        final BaseNaryTree<Character> t = (BaseNaryTree<Character>) ois.readObject();
        assertTrue(sampleTree.equals(t));
    }

    /**
     * Tests the pq-gram tree-edit-distance approximation between two trees
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testPqGramSimilarity() throws Exception {
        // Example taken from Augsten, Bohlen, Gamper, 2005, page 304
        ParseTree t1 = new ParseTree("TOP", vocabulary);
        ParseTree tmp = t1.addChild("TOP");
        tmp.addChild("NP");
        tmp.addChild("S");
        t1.addChild("S");
        t1.addChild("VP");

        ParseTree t2 = new ParseTree("TOP", vocabulary);
        tmp = t2.addChild("TOP");
        tmp.addChild("NP");
        tmp.addChild("S");
        t2.addChild("S");
        t2.addChild("NNP");

        assertEquals(.31, t1.pqgramDistance(t2, 2, 3), .01f);

        t1 = ParseTree.read("(TOP (S (NP (DT Wdt) (NNP Wnnp) (NN Wnn)) (VBD Wvbd) (. W.)))", vocabulary);
        t2 = ParseTree.read("(TOP (S (NP (DT Wdt) (NNP Wnnp)) (VBD  (NN Wnn) (VBD Wvbd)) (. W.)))", vocabulary);
        final ParseTree t3 = ParseTree.read("(TOP (S (DT Wdt) (NNP Wnnp) (NN Wnn) (VBD Wvbd) (. W.)))", vocabulary);

        assertEquals(0f, t1.pqgramDistance(t1, 3, 3), .01f);
        assertEquals(.36f, t1.pqgramDistance(t2, 3, 3), .01f);
        assertEquals(.58f, t1.pqgramDistance(t3, 3, 3), .01f);
        assertEquals(.73f, t2.pqgramDistance(t3, 3, 3), .01f);
    }

    /**
     * Tests Charniak / Magerman style head-percolation (headDescendant(), headLevel(), and isHeadOfTreeRoot() methods)
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testHeadPercolation() throws Exception {
        final HeadPercolationRuleset charniakRuleset = new CharniakHeadPercolationRuleset();
        final HeadPercolationRuleset msaRuleset = new MsaHeadPercolationRuleset();

        final String sentence1 = "(TOP (S (NP (DT The) (JJ industrial) (NN average)) (VP (VBD closed)"
                + " (ADVP (RB down) (NP (CD 18.65))) (, ,) (PP (TO to) (NP (CD 2638.73)))) (. .)))";
        final String sentence2 = "(TOP (S (NP (NN nobody)) (VP (VBD moved) (CC or) (VBD gestured))) (. .))";
        final String sentence3 = "(TOP (S (NP (NP (QP (IN About) ($ $) (CD 518) (CD million)))" + " (PP (IN of) (NP (NN debt)))) (VP (AUX is) (VP (VBN affected))) (. .)))";
        final String sentence4 = "(TOP (S (NP (NNP SHORT) (NNP SKIRTS)) (ADJP (RB not) (JJ welcome)) (PP (IN in) (NP (NNP Texas) (NN court))) (: :)))";
        final String sentence5 = "(TOP (S (NP (NP (PRP$ Their) (NN ridicule)) (PP (IN of) (TO to) (NP (PRP him))))"
                + " (VP (AUX is) (NP (NP (DT no) (NN substitute)) (PP (IN for) (NP (NN argument))))) (. .)))";

        final SimpleVocabulary v = SimpleVocabulary.induce(sentence1 + '\n' + sentence2 + '\n' + sentence3 + '\n' + sentence4 + '\n' + sentence5);
        final ParseTree parseTree1 = ParseTree.read(sentence1, v);

        assertEquals("closed", parseTree1.headDescendant(charniakRuleset).label());
        final ParseTree s = parseTree1.subtree("S");
        assertEquals("closed", s.headDescendant(charniakRuleset).label());
        assertEquals("average", s.subtree("NP").headDescendant(charniakRuleset).label());
        assertEquals("industrial", s.subtree("NP").subtree("JJ").headDescendant(charniakRuleset).label());
        assertEquals("industrial", s.subtree("NP").subtree("JJ").subtree("industrial").headDescendant(charniakRuleset).label());
        assertEquals("closed", s.subtree("VP").headDescendant(charniakRuleset).label());
        assertEquals("closed", s.subtree("VP").subtree("VBD").headDescendant(charniakRuleset).label());
        assertEquals("down", s.subtree("VP").subtree("ADVP").headDescendant(charniakRuleset).label());
        assertEquals("18.65", s.subtree("VP").subtree("ADVP").subtree("NP").headDescendant(charniakRuleset).label());
        assertEquals("to", s.subtree("VP").subtree("PP").headDescendant(charniakRuleset).label());
        assertEquals("2638.73", s.subtree("VP").subtree("PP").subtree("NP").headDescendant(charniakRuleset).label());

        assertEquals(-1, s.subtree("VP").headLevel(charniakRuleset));
        assertEquals(0, s.subtree("VP").subtree("VBD").subtree("closed").headLevel(charniakRuleset));
        assertTrue(s.subtree("VP").subtree("VBD").subtree("closed").isHeadOfTreeRoot(charniakRuleset));
        assertEquals(2, s.subtree("NP").subtree("NN").subtree("average").headLevel(charniakRuleset));
        assertFalse(s.subtree("NP").subtree("NN").subtree("average").isHeadOfTreeRoot(charniakRuleset));
        assertEquals(3, s.subtree("VP").subtree("ADVP").subtree("RB").subtree("down").headLevel(charniakRuleset));
        assertFalse(s.subtree("NP").subtree("NN").subtree("average").isHeadOfTreeRoot(charniakRuleset));

        // The VP has two 'VBD' children - we want to be sure the rightmost one is selected
        final ParseTree parseTree2 = ParseTree.read(sentence2, v);
        assertEquals("gestured", parseTree2.subtree("S").subtree("VP").headDescendant(charniakRuleset).headDescendant(charniakRuleset).label());

        // Test secondary-preference behavior - a QP that doesn't contain another QP, but does
        // contain a '$'
        final ParseTree parseTree3 = ParseTree.read(sentence3, v);
        assertEquals("$", parseTree3.subtree("S").subtree("NP").subtree("NP").subtree("QP").headDescendant(charniakRuleset).label());
        // Test alternate head-percolation rules, preferring embedded VP to AUX as head of a VP
        assertEquals("is", parseTree3.headDescendant(charniakRuleset).label());
        assertEquals("affected", parseTree3.headDescendant(msaRuleset).label());

        // Test 'default' behavior - an S that doesn't contain any of its preferred children (no
        // verb phrase)
        final ParseTree parseTree4 = ParseTree.read(sentence4, v);
        assertEquals("welcome", parseTree4.subtree("S").headDescendant(charniakRuleset).label());

        // Test selection of _leftmost_ preferred child of a PP. This sentence is arbitrarily
        // constructed (ungrammatically) such that the PP has both IN and TO children.
        // (A quick glance didn't turn up any real sentences in the WSJ corpus that exercised this
        // rule properly)
        final ParseTree parseTree5 = ParseTree.read(sentence5, v);
        assertEquals("of", parseTree5.subtree("S").subtree("NP").subtree("PP").headDescendant(charniakRuleset).label());
    }
}
