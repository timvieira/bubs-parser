package edu.ohsu.cslu.datastructs.narytree;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

import edu.ohsu.cslu.datastructs.narytree.BinaryTree.Factorization;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Unit tests for {@link NaryTree} using Strings
 * 
 * @author Aaron Dunlop
 * @since Sep 29, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestStringNaryTree {

    private NaryTree<String> sampleTree;
    private String stringSampleTree;

    private final static String[] SAMPLE_IN_ORDER_ARRAY = new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i",
            "j", "k" };
    private final static String[] SAMPLE_PRE_ORDER_ARRAY = new String[] { "f", "d", "b", "a", "c", "e", "g", "i", "h",
            "k", "j" };
    private final static String[] SAMPLE_POST_ORDER_ARRAY = new String[] { "a", "c", "b", "e", "d", "g", "h", "j", "k",
            "i", "f" };
    private final static String[] SAMPLE_LEAF_ARRAY = new String[] { "a", "c", "e", "g", "h", "j" };

    @Before
    public void setUp() {
        sampleTree = new NaryTree<String>("f");

        NaryTree<String> tmp1 = new NaryTree<String>("b");
        tmp1.addChild("a");
        tmp1.addChild("c");

        NaryTree<String> tmp2 = new NaryTree<String>("d");
        tmp2.addSubtree(tmp1);
        tmp2.addChild("e");

        sampleTree.addSubtree(tmp2);
        sampleTree.addChild("g");

        tmp1 = new NaryTree<String>("i");
        tmp1.addChild("h");
        tmp2 = tmp1.addChild("k");
        tmp2.addChild("j");
        sampleTree.addSubtree(tmp1);

        /**
         * <pre>
         *             f 
         *             |
         *       --------------
         *       |     |     |
         *       d     g     i
         *       |           |
         *    -------     --------
         *    |     |     |      |
         *    b     e     h      k
         *    |                  |
         *  -----                j
         *  |   |
         *  a   c
         * </pre>
         */
        stringSampleTree = "(f (d (b a c) e) g (i h (k j)))";
    }

    @Test
    public void testAddChild() throws Exception {
        final NaryTree<String> tree = new NaryTree<String>("a");
        assertEquals(1, tree.size());
        tree.addChild("b");
        assertEquals(2, tree.size());
        assertNull(tree.subtree("a"));
        assertNotNull(tree.subtree("b"));
    }

    @Test
    public void testAddChildren() throws Exception {
        final NaryTree<String> tree = new NaryTree<String>("a");
        assertEquals(1, tree.size());
        tree.addChildren(new String[] { "b", "c" });
        assertEquals(3, tree.size());
        assertNull(tree.subtree("a"));
        assertNotNull(tree.subtree("b"));
        assertNotNull(tree.subtree("c"));
    }

    @Test
    public void testAddSubtree() throws Exception {
        final NaryTree<String> tree = new NaryTree<String>("a");
        final NaryTree<String> tmp = new NaryTree<String>("b");
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

        sampleTree.removeChild("g");
        assertEquals(10, sampleTree.size());

        // Removing the "d" node should move its children up
        assertNull(sampleTree.subtree("b"));
        assertNull(sampleTree.subtree("e"));

        sampleTree.removeChild("d");
        assertEquals(5, sampleTree.size());
    }

    @Test
    public void testRemoveChildrenByStringArray() throws Exception {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.subtree("h"));
        assertNull(sampleTree.subtree("j"));

        // Removing the "i" node should move its children up ("g" has no children)
        sampleTree.removeChildren(new String[] { "g", "i" });
        assertEquals(6, sampleTree.size());
    }

    @Test
    public void testSubtree() throws Exception {
        final NaryTree<String> subtree = sampleTree.subtree("d");
        assertEquals(5, subtree.size());
        assertNotNull(subtree.subtree("b"));
        assertNotNull(subtree.subtree("e"));
        assertEquals(3, subtree.subtree("b").size());

        assertEquals(4, sampleTree.subtree("i").size());
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(11, sampleTree.size());
        sampleTree.subtree("i").removeChild("k");
        assertEquals(9, sampleTree.size());
        sampleTree.subtree("g").addChild("z");
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
    public void testChildLabels() throws Exception {
        assertArrayEquals(new String[] { "d", "g", "i" }, sampleTree.childLabels().toArray(new String[0]));
    }

    @Test
    public void testLeaves() throws Exception {
        assertEquals(6, sampleTree.leaves());
        assertEquals(3, sampleTree.subtree("d").leaves());

        sampleTree.subtree("g").addChild("g2");
        assertEquals(6, sampleTree.leaves());

        sampleTree.subtree("g").removeChild("g3");
        assertEquals(6, sampleTree.leaves());

        sampleTree.subtree("i").removeChild("k");
        assertEquals(5, sampleTree.leaves());

        sampleTree.removeChild("d");
        assertEquals(2, sampleTree.leaves());

        sampleTree.removeChild("g");
        assertEquals(1, sampleTree.leaves());

        sampleTree.removeChild("i");
        assertEquals(1, sampleTree.leaves());
    }

    @Test
    public void testIsLeaf() throws Exception {
        assertFalse(sampleTree.isLeaf());
        assertFalse(sampleTree.subtree("d").isLeaf());
        assertTrue(sampleTree.subtree("g").isLeaf());
        assertTrue(sampleTree.subtree("i").subtree("h").isLeaf());
    }

    @Test
    public void testIsLeftmostChild() throws Exception {
        final Iterator<NaryTree<String>> i = sampleTree.inOrderIterator();
        assertTrue(i.next().isLeftmostChild()); // a
        assertTrue(i.next().isLeftmostChild()); // b
        assertFalse(i.next().isLeftmostChild()); // c
        assertTrue(i.next().isLeftmostChild()); // d
        assertFalse(i.next().isLeftmostChild()); // e
        assertFalse(i.next().isLeftmostChild()); // f
        assertFalse(i.next().isLeftmostChild()); // g
        assertTrue(i.next().isLeftmostChild()); // h
        assertFalse(i.next().isLeftmostChild()); // i
        assertFalse(i.next().isLeftmostChild()); // j
        assertFalse(i.next().isLeftmostChild()); // k
    }

    @Test
    public void testIsRightmostChild() throws Exception {
        final Iterator<NaryTree<String>> i = sampleTree.inOrderIterator();
        assertFalse(i.next().isRightmostChild()); // a
        assertFalse(i.next().isRightmostChild()); // b
        assertTrue(i.next().isRightmostChild()); // c
        assertFalse(i.next().isRightmostChild()); // d
        assertTrue(i.next().isRightmostChild()); // e
        assertFalse(i.next().isRightmostChild()); // f
        assertFalse(i.next().isRightmostChild()); // g
        assertFalse(i.next().isRightmostChild()); // h
        assertTrue(i.next().isRightmostChild()); // i
        assertTrue(i.next().isRightmostChild()); // j
        assertTrue(i.next().isRightmostChild()); // k
    }

    @Test
    public void testInOrderIterator() throws Exception {
        final Iterator<NaryTree<String>> iter = sampleTree.inOrderIterator();
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
        final Iterator<NaryTree<String>> iter = sampleTree.preOrderIterator();
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
        final Iterator<NaryTree<String>> iter = sampleTree.postOrderIterator();
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
    public void testLeafIterator() throws Exception {
        final Iterator<NaryTree<String>> iter = sampleTree.leafIterator();
        for (int i = 0; i < SAMPLE_LEAF_ARRAY.length; i++) {
            assertEquals(SAMPLE_LEAF_ARRAY[i], iter.next().label());
        }
    }

    @Test
    public void testReadFromReader() throws Exception {

        final String stringSimpleTree = "(a (b c) d)";
        final NaryTree<String> simpleTree = NaryTree.read(new StringReader(stringSimpleTree), String.class);
        assertEquals(4, simpleTree.size());
        assertEquals(2, simpleTree.subtree("b").size());

        assertEquals("a", simpleTree.label());
        assertEquals("b", simpleTree.subtree("b").label());
        assertEquals("c", simpleTree.subtree("b").subtree("c").label());
        assertEquals("d", simpleTree.subtree("d").label());

        final String stringTestTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final NaryTree<String> testTree = NaryTree.read(new StringReader(stringTestTree), String.class);
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

        final NaryTree<String> tree = NaryTree.read(new StringReader(stringSampleTree), String.class);
        assertEquals(sampleTree, tree);
    }

    @Test
    public void testWriteToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        sampleTree.write(writer);
        assertEquals(stringSampleTree, writer.toString());

        final String stringSimpleTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final NaryTree<String> tree = new NaryTree<String>("a");
        tree.addChild("b").addChild("c").addChild("d").addChild("e").addChild("f");
        tree.subtree("b").subtree("c").subtree("d").addChild("g").addChild("h");
        tree.subtree("b").subtree("c").addChild("i").addChild("j");
        tree.subtree("b").addChild("k").addChild("l");

        writer = new StringWriter();
        tree.write(writer);
        assertEquals(stringSimpleTree, writer.toString());
    }

    @Test
    public void testEquals() throws Exception {
        final NaryTree<String> tree1 = new NaryTree<String>("a");
        tree1.addChildren(new String[] { "b", "c" });

        final NaryTree<String> tree2 = new NaryTree<String>("a");
        tree2.addChildren(new String[] { "b", "c" });

        final NaryTree<String> tree3 = new NaryTree<String>("a");
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
        final NaryTree<String> t = (NaryTree<String>) ois.readObject();
        assertTrue(sampleTree.equals(t));
    }

    @Test
    public void testLeftFactor() {
        assertEquals(BinaryTree.read("(S (NP C0 C1))", String.class), NaryTree.read("(S (NP C0 C1))", String.class)
                .factor(GrammarFormatType.Berkeley, Factorization.LEFT));

        assertEquals(BinaryTree.read("(S (NP C0 (@NP C1 C2)))", String.class),
                NaryTree.read("(S (NP C0 C1 C2))", String.class).factor(GrammarFormatType.Berkeley, Factorization.LEFT));
    }

    @Test
    public void testRightFactor() {
        assertEquals(BinaryTree.read("(S (NP C0 C1))", String.class), NaryTree.read("(S (NP C0 C1))", String.class)
                .factor(GrammarFormatType.Berkeley, Factorization.RIGHT));

        assertEquals(BinaryTree.read("(S (NP (@NP C0 C1) C2))", String.class),
                NaryTree.read("(S (NP C0 C1 C2))", String.class)
                        .factor(GrammarFormatType.Berkeley, Factorization.RIGHT));
    }

    @Test
    public void testClone() {
        assertEquals(stringSampleTree, sampleTree.clone().toString());
    }
}
