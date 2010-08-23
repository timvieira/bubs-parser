package edu.ohsu.cslu.datastructs.narytree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import edu.ohsu.cslu.tests.FilteredRunner;

import static junit.framework.Assert.*;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link BinaryTree} parameterized with strings.
 * 
 * @author Aaron Dunlop
 * @since Aug 23, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestStringBinaryTree {

    private BinaryTree<String> sampleTree;
    private String stringSampleTree;

    private final static String[] SAMPLE_IN_ORDER_ARRAY = new String[] { "a", "b", "c", "d", "e", "f", "g",
            "h", "i", "j", "k" };
    private final static String[] SAMPLE_PRE_ORDER_ARRAY = new String[] { "f", "d", "b", "a", "c", "e", "i",
            "h", "g", "k", "j" };
    private final static String[] SAMPLE_POST_ORDER_ARRAY = new String[] { "a", "c", "b", "e", "d", "g", "h",
            "j", "k", "i", "f" };

    @Before
    public void setUp() {
        sampleTree = new BinaryTree<String>("f");

        final BinaryTree<String> d = sampleTree.addChild("d");
        final BinaryTree<String> b = d.addChild("b");
        b.addChild("a");
        b.addChild("c");

        d.addChild("e");

        final BinaryTree<String> i = sampleTree.addChild("i");
        final BinaryTree<String> h = i.addChild("h");
        h.addChild("g");

        final BinaryTree<String> k = i.addChild("k");
        k.addChild("j");

        /**
         * <pre>
         *             f 
         *             |
         *       --------------
         *       |           |
         *       d           i
         *       |           |
         *    -------     --------
         *    |     |     |      |
         *    b     e     h      k
         *    |           |      |
         *  -----         g      j
         *  |   |
         *  a   c
         * </pre>
         */
        stringSampleTree = "(f (d (b a c) e) (i (h g) (k j)))";
    }

    @Test
    public void testAddChild() throws Exception {
        final BinaryTree<String> tree = new BinaryTree<String>("a");
        assertEquals(1, tree.size());
        tree.addChild("b");
        assertEquals(2, tree.size());
        assertNull(tree.subtree("a"));
        assertNotNull(tree.subtree("b"));
    }

    @Test
    public void testAddChildren() throws Exception {
        final BinaryTree<String> tree = new BinaryTree<String>("a");
        assertEquals(1, tree.size());
        tree.addChildren(new String[] { "b", "c" });
        assertEquals(3, tree.size());
        assertNull(tree.subtree("a"));
        assertNotNull(tree.subtree("b"));
        assertNotNull(tree.subtree("c"));
    }

    @Test
    public void testAddSubtree() throws Exception {
        final BinaryTree<String> tree = new BinaryTree<String>("a");
        final BinaryTree<String> tmp = new BinaryTree<String>("b");
        tmp.addChildren(new String[] { "c", "d" });
        tree.addSubtree(tmp);
        assertEquals(4, tree.size());
        assertNotNull(tree.subtree("b"));
        assertNotNull(tree.subtree("b").subtree("c"));
        assertNotNull(tree.subtree("b").subtree("d"));
    }

    @Test
    public void testRemoveChild() throws Exception {
        assertEquals(11, sampleTree.size());

        sampleTree.removeChild("i");
        assertEquals(6, sampleTree.size());

        // Removing the "i" node should remove its children as well
        assertNull(sampleTree.subtree("i"));
        assertNull(sampleTree.subtree("h"));

        sampleTree.removeChild("d");
        assertEquals(1, sampleTree.size());
        assertNull(sampleTree.subtree("d"));
    }

    @Test
    public void testRemoveChildrenByStringArray() throws Exception {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.subtree("h"));
        assertNull(sampleTree.subtree("j"));

        // Removing the "d" node should move the right child ("i") to the left child (there is no "g" subtree)
        sampleTree.removeChildren(new String[] { "d", "g" });
        assertEquals(6, sampleTree.size());

        assertNotNull(sampleTree.subtree("i"));
    }

    @Test
    public void testRemoveSubtree() throws Exception {
        assertEquals(11, sampleTree.size());

        sampleTree.removeSubtree("g");
        assertEquals(11, sampleTree.size());

        // Removing the "d" node should remove all its children as well
        sampleTree.removeSubtree("d");
        assertEquals(6, sampleTree.size());
        assertNull(sampleTree.subtree("d"));
        assertNull(sampleTree.subtree("e"));
    }

    @Test
    public void testSubtree() throws Exception {
        final BinaryTree<String> subtree = sampleTree.subtree("d");
        assertEquals(5, subtree.size());
        assertNotNull(subtree.subtree("b"));
        assertNotNull(subtree.subtree("e"));
        assertEquals(3, subtree.subtree("b").size());

        assertEquals(5, sampleTree.subtree("i").size());
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(11, sampleTree.size());
        sampleTree.subtree("i").removeSubtree("k");
        assertEquals(9, sampleTree.size());
        sampleTree.subtree("d").subtree("e").addChild("z");
        assertEquals(10, sampleTree.size());
    }

    @Test
    public void testDepthFromRoot() throws Exception {
        assertEquals(0, sampleTree.depthFromRoot());
        assertEquals(1, sampleTree.subtree("d").depthFromRoot());
        assertEquals(2, sampleTree.subtree("i").subtree("k").depthFromRoot());
        assertEquals(3, sampleTree.subtree("i").subtree("k").subtree("j").depthFromRoot());
    }

    @Test
    public void testLeaves() throws Exception {
        assertEquals(5, sampleTree.leaves());
        assertEquals(3, sampleTree.subtree("d").leaves());

        sampleTree.subtree("i").subtree("h").removeChild("g");
        assertEquals(5, sampleTree.leaves());

        sampleTree.subtree("i").removeSubtree("h");
        assertEquals(4, sampleTree.leaves());

        sampleTree.removeSubtree("d");
        assertEquals(1, sampleTree.leaves());
    }

    @Test
    public void testInOrderIterator() throws Exception {
        final Iterator<BinaryTree<String>> iter = sampleTree.inOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i], iter.next().label().toString());
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
        final Iterator<BinaryTree<String>> iter = sampleTree.preOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i], iter.next().label().toString());
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
        final Iterator<BinaryTree<String>> iter = sampleTree.postOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i], iter.next().label().toString());
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

        final String stringSimpleTree = "(a (b c) d)";
        final BinaryTree<String> simpleTree = BinaryTree.read(new StringReader(stringSimpleTree),
            String.class);
        assertEquals(4, simpleTree.size());
        assertEquals(2, simpleTree.subtree("b").size());

        assertEquals("a", simpleTree.label());
        assertEquals("b", simpleTree.subtree("b").label());
        assertEquals("c", simpleTree.subtree("b").subtree("c").label());
        assertEquals("d", simpleTree.subtree("d").label());

        final String stringTestTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final BinaryTree<String> testTree = BinaryTree.read(new StringReader(stringTestTree), String.class);
        assertEquals(12, testTree.size());
        assertEquals(8, testTree.subtree("b").subtree("c").size());

        assertEquals("a", testTree.label());
        assertEquals("b", testTree.subtree("b").label());
        assertEquals("c", testTree.subtree("b").subtree("c").label());
        assertEquals("d", testTree.subtree("b").subtree("c").subtree("d").label());
        assertEquals("e", testTree.subtree("b").subtree("c").subtree("d").subtree("e").label());
        assertEquals("f", testTree.subtree("b").subtree("c").subtree("d").subtree("e").subtree("f").label());
        assertEquals("g", testTree.subtree("b").subtree("c").subtree("d").subtree("g").label());
        assertEquals("h", testTree.subtree("b").subtree("c").subtree("d").subtree("g").subtree("h").label());
        assertEquals("i", testTree.subtree("b").subtree("c").subtree("i").label());
        assertEquals("j", testTree.subtree("b").subtree("c").subtree("i").subtree("j").label());
        assertEquals("k", testTree.subtree("b").subtree("k").label());
        assertEquals("l", testTree.subtree("b").subtree("k").subtree("l").label());

        final BinaryTree<String> tree = BinaryTree.read(new StringReader(stringSampleTree), String.class);
        assertEquals(sampleTree, tree);
    }

    @Test
    public void testWriteToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        sampleTree.write(writer);
        assertEquals(stringSampleTree, writer.toString());

        final String stringSimpleTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final BinaryTree<String> tree = new BinaryTree<String>("a");
        tree.addChild("b").addChild("c").addChild("d").addChild("e").addChild("f");
        tree.subtree("b").subtree("c").subtree("d").addChild("g").addChild("h");
        tree.subtree("b").subtree("c").addChild("i").addChild("j");
        tree.subtree("b").addChild("k").addChild("l");

        writer = new StringWriter();
        tree.write(writer);
        assertEquals(stringSimpleTree, writer.toString());
    }

    @Test
    public void testIsLeaf() throws Exception {
        assertFalse(sampleTree.isLeaf());
        assertFalse(sampleTree.subtree("d").isLeaf());
        assertTrue(sampleTree.subtree("i").subtree("h").subtree("g").isLeaf());
    }

    @Test
    public void testEquals() throws Exception {
        final BinaryTree<String> tree1 = new BinaryTree<String>("a");
        tree1.addChildren(new String[] { "b", "c" });

        final BinaryTree<String> tree2 = new BinaryTree<String>("a");
        tree2.addChildren(new String[] { "b", "c" });

        final BinaryTree<String> tree3 = new BinaryTree<String>("a");
        tree3.addChildren(new String[] { "b", "d" });

        assertTrue(tree1.equals(tree2));
        assertFalse(tree1.equals(tree3));

        tree2.subtree("c").addChild("d");
        assertFalse(tree1.equals(tree2));
    }

    /**
     * Tests Java serialization and deserialization of trees
     * 
     * @throws Exception if something bad happens
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSerialize() throws Exception {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(sampleTree);

        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        final BinaryTree<String> t = (BinaryTree<String>) ois.readObject();
        assertTrue(sampleTree.equals(t));
    }

    @Test
    public void testUnfactor() throws Exception {
        BinaryTree<String> binaryTree = BinaryTree.read("(A B C)", String.class);
        assertEquals(BaseNaryTree.read("(A B C)", String.class),
            binaryTree.unfactor(GrammarFormatType.Berkeley));

        binaryTree = BinaryTree.read("(A B (@A C D))", String.class);
        assertEquals(BaseNaryTree.read("(A B C D)", String.class),
            binaryTree.unfactor(GrammarFormatType.Berkeley));

        binaryTree = BinaryTree.read("(A (@A B C) D)", String.class);
        assertEquals(BaseNaryTree.read("(A B C D)", String.class),
            binaryTree.unfactor(GrammarFormatType.Berkeley));

        binaryTree = BinaryTree
            .read("(ROOT_0 (NP_31 (@NP_29 (@NP_40 (:_3 --) (NNP_0 C.E.)) (NNP_9 Friedman)) (._3 .)))",
                String.class);
        assertEquals(BaseNaryTree.read("(ROOT (NP (: --) (NNP C.E.) (NNP Friedman) (. .)))", String.class),
            binaryTree.unfactor(GrammarFormatType.Berkeley));

        binaryTree = BinaryTree.read(
            "(ROOT_0 (S_0 (@S_24 (NP_23 (NN_26 Trouble)) (VP_32 (@VP_10 (VBZ_17 is) (,_0 ,))"
                    + " (SBAR_1 (S_5 (NP_36 (PRP_2 she)) (VP_34 (VBZ_16 has) (VP_11 (@VP_28"
                    + " (VBN_23 lost) (NP_37 (PRP_1 it))) (ADVP_1 (@ADVP_0 (RB_31 just)"
                    + " (RB_32 as)) (RB_2 quickly)))))))) (._3 .)))", String.class);
        assertEquals(BaseNaryTree.read(
            "(ROOT (S (NP (NN Trouble)) (VP (VBZ is) (, ,) (SBAR (S (NP (PRP she)) (VP (VBZ has) (VP (VBN lost) ("
                    + "NP (PRP it)) (ADVP (RB just) (RB as) (RB quickly))))))) (. .)))", String.class),
            binaryTree.unfactor(GrammarFormatType.Berkeley));

        binaryTree = BinaryTree.read(
            "(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (NP^<NP> (JJ Little) (NN chance))"
                    + " (PP^<NP> (IN that) (NP^<PP> (NNP Shane) (NNP Longman))))"
                    + " (VP^<S> (AUX is) (VP^<VP> (VBG going) (S^<VP> (VP^<S> (TO to)"
                    + " (VP^<VP> (VB recoup) (NP^<VP> (NN today)))))))) (. .)))", String.class);
        assertEquals(BaseNaryTree.read(
            "(TOP (S (NP (NP (JJ Little) (NN chance)) (PP (IN that) (NP (NNP Shane)"
                    + " (NNP Longman)))) (VP (AUX is) (VP (VBG going) (S (VP (TO to) (VP (VB recoup) "
                    + "(NP (NN today))))))) (. .)))", String.class),
            binaryTree.unfactor(GrammarFormatType.CSLU));
    }
}
