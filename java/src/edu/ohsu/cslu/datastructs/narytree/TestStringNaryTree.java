/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.datastructs.narytree;

import static edu.ohsu.cslu.tests.JUnit.assertMapContains;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.cjunit.FilteredRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.GrammarFormatType;

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
        tmp2.addChild(tmp1);
        tmp2.addChild("e");

        sampleTree.addChild(tmp2);
        sampleTree.addChild("g");

        tmp1 = new NaryTree<String>("i");
        tmp1.addChild("h");
        tmp2 = tmp1.addChild("k");
        tmp2.addChild("j");
        sampleTree.addChild(tmp1);

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
        NaryTree<String> tree = new NaryTree<String>("a");
        assertEquals(1, tree.size());
        tree.addChild("b");
        assertEquals(2, tree.size());
        assertNull(tree.child("a"));
        assertNotNull(tree.child("b"));

        tree = new NaryTree<String>("a");
        final NaryTree<String> tmp = new NaryTree<String>("b");
        tmp.addChildren(new String[] { "c", "d" });
        tree.addChild(tmp);
        assertEquals(4, tree.size());
        assertEquals(2, tree.leaves());
        assertNotNull(tree.child("b"));
        assertNotNull(tree.child("b").child("c"));
        assertNotNull(tree.child("b").child("d"));
    }

    @Test
    public void testAddChildren() throws Exception {
        final NaryTree<String> tree = new NaryTree<String>("a");
        assertEquals(1, tree.size());
        tree.addChildren(new String[] { "b", "c" });
        assertEquals(3, tree.size());
        assertNull(tree.child("a"));
        assertNotNull(tree.child("b"));
        assertNotNull(tree.child("c"));
    }

    @Test
    public void testRemoveChild() throws Exception {
        assertEquals(11, sampleTree.size());

        sampleTree.removeChild("g");
        assertEquals(10, sampleTree.size());

        // Removing the "d" node should move its children up
        assertNull(sampleTree.child("b"));
        assertNull(sampleTree.child("e"));

        sampleTree.removeChild("d");
        assertEquals(5, sampleTree.size());
    }

    @Test
    public void testRemoveChildrenByStringArray() throws Exception {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.child("h"));
        assertNull(sampleTree.child("j"));

        // Removing the "i" node should move its children up ("g" has no children)
        sampleTree.removeChildren(new String[] { "g", "i" });
        assertEquals(6, sampleTree.size());
    }

    @Test
    public void testWithoutLabels() throws Exception {
        assertEquals(11, sampleTree.size());

        NaryTree<String> newTree = sampleTree.withoutLabels(Arrays.asList(new String[] { "e", "g" }));
        assertEquals(9, newTree.size());

        newTree = newTree.withoutLabels(new String[] { "i", "c", "." });
        assertEquals(4, newTree.size());
    }

    @Test
    public void testSubtree() throws Exception {
        final NaryTree<String> subtree = sampleTree.child("d");
        assertEquals(5, subtree.size());
        assertNotNull(subtree.child("b"));
        assertNotNull(subtree.child("e"));
        assertEquals(3, subtree.child("b").size());

        assertEquals(4, sampleTree.child("i").size());
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(11, sampleTree.size());
        sampleTree.child("i").removeChild("k");
        assertEquals(9, sampleTree.size());
        sampleTree.child("g").addChild("z");
        assertEquals(10, sampleTree.size());
    }

    @Test
    public void testHeight() throws Exception {
        assertEquals(4, sampleTree.height());
        assertEquals(3, sampleTree.children().getFirst().height());
        assertEquals(3, sampleTree.children().getLast().height());

        sampleTree.child("i").removeChild("k");
        assertEquals(2, sampleTree.children().getLast().height());

        sampleTree.child("i").child("h").addChild("z");
        assertEquals(3, sampleTree.children().getLast().height());
        assertEquals(4, sampleTree.height());
    }

    @Test
    public void testDepthFromRoot() throws Exception {
        assertEquals(0, sampleTree.depthFromRoot());
        assertEquals(1, sampleTree.child("d").depthFromRoot());
        assertEquals(2, sampleTree.child("i").child("k").depthFromRoot());
        assertEquals(3, sampleTree.child("i").child("k").child("j").depthFromRoot());
    }

    @Test
    public void testChildLabels() throws Exception {
        assertArrayEquals(new String[] { "d", "g", "i" }, sampleTree.childLabels().toArray(new String[0]));
    }

    @Test
    public void testLeaves() throws Exception {
        assertEquals(6, sampleTree.leaves());
        assertEquals(3, sampleTree.child("d").leaves());

        sampleTree.child("g").addChild("g2");
        assertEquals(6, sampleTree.leaves());

        sampleTree.child("g").removeChild("g3");
        assertEquals(6, sampleTree.leaves());

        sampleTree.child("i").removeChild("k");
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
        assertFalse(sampleTree.child("d").isLeaf());
        assertTrue(sampleTree.child("g").isLeaf());
        assertTrue(sampleTree.child("i").child("h").isLeaf());
    }

    @Test
    public void testIsLeftmostChild() throws Exception {
        final Iterator<NaryTree<String>> i = sampleTree.inOrderTraversal().iterator();
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
        final Iterator<NaryTree<String>> i = sampleTree.inOrderTraversal().iterator();
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
    public void testInOrderTraversal() throws Exception {
        int i = 0;
        for (final NaryTree<String> tree : sampleTree.inOrderTraversal()) {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i++], tree.label());
        }
    }

    @Test
    public void testInOrderLabelTraversal() throws Exception {
        int i = 0;
        for (final String label : sampleTree.inOrderLabelTraversal()) {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i++], label);
        }
    }

    @Test
    public void testPreOrderTraversal() throws Exception {
        int i = 0;
        for (final NaryTree<String> tree : sampleTree.preOrderTraversal()) {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i++], tree.label());
        }
    }

    @Test
    public void testPreOrderLabelTraversal() throws Exception {
        int i = 0;
        for (final String label : sampleTree.preOrderLabelTraversal()) {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i++], label);
        }
    }

    @Test
    public void testPostOrderTraversal() throws Exception {
        int i = 0;
        for (final NaryTree<String> tree : sampleTree.postOrderTraversal()) {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i++], tree.label());
        }
    }

    @Test
    public void testPostOrderLabelTraversal() throws Exception {
        int i = 0;
        for (final String label : sampleTree.postOrderLabelTraversal()) {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i++], label);
        }
    }

    @Test
    public void testLeafTraversal() throws Exception {
        int i = 0;
        for (final NaryTree<String> leaf : sampleTree.leafTraversal()) {
            assertEquals(SAMPLE_LEAF_ARRAY[i++], leaf.label());
        }
    }

    @Test
    public void testReadFromReader() throws Exception {

        final String stringSimpleTree = "(a (b c) d)";
        final NaryTree<String> simpleTree = NaryTree.read(new StringReader(stringSimpleTree), String.class);
        assertEquals(4, simpleTree.size());
        assertEquals(2, simpleTree.child("b").size());

        assertEquals("a", simpleTree.label());
        assertEquals("b", simpleTree.child("b").label());
        assertEquals("c", simpleTree.child("b").child("c").label());
        assertEquals("d", simpleTree.child("d").label());

        final String stringTestTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final NaryTree<String> testTree = NaryTree.read(new StringReader(stringTestTree), String.class);
        assertEquals(12, testTree.size());
        assertEquals(8, testTree.child("b").child("c").size());

        assertEquals("a", testTree.label());
        assertEquals("b", testTree.child("b").label());
        assertEquals("c", testTree.child("b").child("c").label());
        assertEquals("d", testTree.child("b").child("c").child("d").label());
        assertEquals("e", testTree.child("b").child("c").child("d").child("e").label());
        assertEquals("f", testTree.child("b").child("c").child("d").child("e").child("f").label());
        assertEquals("g", testTree.child("b").child("c").child("d").child("g").label());
        assertEquals("h", testTree.child("b").child("c").child("d").child("g").child("h").label());
        assertEquals("i", testTree.child("b").child("c").child("i").label());
        assertEquals("j", testTree.child("b").child("c").child("i").child("j").label());
        assertEquals("k", testTree.child("b").child("k").label());
        assertEquals("l", testTree.child("b").child("k").child("l").label());

        final NaryTree<String> tree = NaryTree.read(new StringReader(stringSampleTree), String.class);
        assertEquals(sampleTree, tree);

        // Test reading an empty tree
        assertEquals(new NaryTree<String>(""), NaryTree.read("()", String.class));

        // And an empty string
        try {
            NaryTree.read("", String.class);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
        }

        // And a garbage string
        try {
            NaryTree.read(")#", String.class);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
        }
    }

    @Test
    public void testWriteToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        sampleTree.write(writer);
        assertEquals(stringSampleTree, writer.toString());

        final String stringSimpleTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final NaryTree<String> tree = new NaryTree<String>("a");
        tree.addChild("b").addChild("c").addChild("d").addChild("e").addChild("f");
        tree.child("b").child("c").child("d").addChild("g").addChild("h");
        tree.child("b").child("c").addChild("i").addChild("j");
        tree.child("b").addChild("k").addChild("l");

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

        tree2.child("c").addChild("d");
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
    public void testRightBinarize() {
        assertEquals("(S (NP C0))",
                NaryTree.read("(S (NP C0))", String.class).binarize(GrammarFormatType.Berkeley, Binarization.LEFT)
                        .toString());

        assertEquals("(S (NP (NP C0)))",
                NaryTree.read("(S (NP (NP C0)))", String.class).binarize(GrammarFormatType.Berkeley, Binarization.LEFT)
                        .toString());

        assertEquals("(S (NP C0 C1))",
                NaryTree.read("(S (NP C0 C1))", String.class).binarize(GrammarFormatType.Berkeley, Binarization.RIGHT)
                        .toString());

        assertEquals(
                "(S (NP C0 (@NP C1 C2)))",
                NaryTree.read("(S (NP C0 C1 C2))", String.class)
                        .binarize(GrammarFormatType.Berkeley, Binarization.RIGHT).toString());

        assertEquals(
                "(S (NP C0 (@NP C1 (@NP C2 C3))))",
                NaryTree.read("(S (NP C0 C1 C2 C3))", String.class)
                        .binarize(GrammarFormatType.Berkeley, Binarization.RIGHT).toString());
    }

    @Test
    public void testLeftBinarize() {
        assertEquals("(S (NP C0))",
                NaryTree.read("(S (NP C0))", String.class).binarize(GrammarFormatType.Berkeley, Binarization.LEFT)
                        .toString());

        assertEquals("(S (NP (NP C0)))",
                NaryTree.read("(S (NP (NP C0)))", String.class).binarize(GrammarFormatType.Berkeley, Binarization.LEFT)
                        .toString());

        assertEquals("(S (NP C0 C1))",
                NaryTree.read("(S (NP C0 C1))", String.class).binarize(GrammarFormatType.Berkeley, Binarization.LEFT)
                        .toString());

        assertEquals("(S (NP (@NP C0 C1) C2))",
                NaryTree.read("(S (NP C0 C1 C2))", String.class)
                        .binarize(GrammarFormatType.Berkeley, Binarization.LEFT).toString());

        assertEquals(
                BinaryTree.read("(S (NP (@NP (@NP C0 C1) C2) C3))", String.class),
                NaryTree.read("(S (NP C0 C1 C2 C3))", String.class).binarize(GrammarFormatType.Berkeley,
                        Binarization.LEFT));
    }

    @Test
    public void testClone() {
        final NaryTree<String> clone = sampleTree.clone();
        assertFalse(sampleTree == clone);
        assertEquals(sampleTree, clone);
    }

    @Test
    public void testTransform() {
        assertEquals("(F (D (B A C) E) G (I H (K J)))", sampleTree.transform(new Tree.LabelTransformer<String>() {
            @Override
            public String transform(final String s) {
                return s.toUpperCase();
            }
        }).toString());
    }

    @Test
    public void testRemoveAll() {
        assertEquals("(f (d (b a c) e) (i h j))",
                sampleTree.removeAll(new HashSet<String>(Arrays.asList(new String[] { "g", "k" })), 1).toString());
        assertEquals("(f (d (b a c) e) (i h))",
                sampleTree.removeAll(new HashSet<String>(Arrays.asList(new String[] { "g", "k" })), 2).toString());

        // Remove the root node - OK as long as it has only one child
        assertEquals(
                "(b c d)",
                NaryTree.read("(a (b c d))", String.class)
                        .removeAll(new HashSet<String>(Arrays.asList(new String[] { "a" })), 1).toString());

        // Try to remove a root node with more than one child
        try {
            NaryTree.read("(a (b c d) e)", String.class).removeAll(
                    new HashSet<String>(Arrays.asList(new String[] { "a" })), 1);
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
        }
    }

    @Test
    public void testLabeledSpans() {
        Map<String, Integer> spans = sampleTree.labeledSpans(1);
        assertEquals(sampleTree.size, spans.size());
        assertMapContains(spans, "0,1,a", 1);
        assertMapContains(spans, "1,2,c", 1);
        assertMapContains(spans, "0,2,b", 1);
        assertMapContains(spans, "2,3,e", 1);
        assertMapContains(spans, "0,3,d", 1);
        assertMapContains(spans, "3,4,g", 1);
        assertMapContains(spans, "4,5,h", 1);
        assertMapContains(spans, "4,6,i", 1);
        assertMapContains(spans, "5,6,k", 1);
        assertMapContains(spans, "5,6,j", 1);
        assertMapContains(spans, "0,6,f", 1);

        spans = sampleTree.labeledSpans(2);
        assertEquals(5, spans.size());
        assertMapContains(spans, "0,2,b", 1);
        assertMapContains(spans, "0,3,d", 1);
        assertMapContains(spans, "4,6,i", 1);
        assertMapContains(spans, "0,6,f", 1);
        assertMapContains(spans, "5,6,k", 1);

        final Map<String, String> equivalentLabels = new HashMap<String, String>();
        equivalentLabels.put("h", "i");
        equivalentLabels.put("k", "j");
        spans = sampleTree.labeledSpans(1, null, equivalentLabels);
        assertEquals(sampleTree.size() - 1, spans.size());
        assertMapContains(spans, "0,1,a", 1);
        assertMapContains(spans, "1,2,c", 1);
        assertMapContains(spans, "0,2,b", 1);
        assertMapContains(spans, "2,3,e", 1);
        assertMapContains(spans, "0,3,d", 1);
        assertMapContains(spans, "3,4,g", 1);
        assertMapContains(spans, "4,5,i", 1);
        assertMapContains(spans, "4,6,i", 1);
        assertMapContains(spans, "5,6,j", 2);
        assertMapContains(spans, "0,6,f", 1);
    }

    @Test
    public void testLeftmostLeaf() {
        assertEquals("a", sampleTree.leftmostLeaf().label());
        assertEquals("a", sampleTree.child("d").leftmostLeaf().label());
        assertEquals("g", sampleTree.child("g").leftmostLeaf().label());
        assertEquals("h", sampleTree.child("i").leftmostLeaf().label());
    }

    @Test
    public void testRightmostLeaf() {
        assertEquals("j", sampleTree.rightmostLeaf().label());
        assertEquals("e", sampleTree.child("d").rightmostLeaf().label());
        assertEquals("g", sampleTree.child("g").rightmostLeaf().label());
        assertEquals("j", sampleTree.child("i").rightmostLeaf().label());
    }

    @Test
    public void testIsBinaryTree() {
        assertFalse(sampleTree.isBinaryTree());
        assertTrue(sampleTree.child("d").isBinaryTree());
        assertTrue(sampleTree.child("g").isBinaryTree());
        assertTrue(sampleTree.child("i").isBinaryTree());
    }

    @Test
    public void testReplaceLeafLabels() {
        NaryTree<String> t = sampleTree.clone();
        t.replaceLeafLabels(Arrays.asList(new String[] { "u", "v", "w", "x", "y", "z" }));
        assertEquals("(f (d (b u v) w) x (i y (k z)))", t.toString());

        t = sampleTree.clone();
        t.replaceLeafLabels(new String[] { "u", "v", "w", "x", "y", "z" });
        assertEquals("(f (d (b u v) w) x (i y (k z)))", t.toString());

        try {
            sampleTree.replaceLeafLabels(new String[] { "t", "u", "v", "w", "x", "y", "z" });
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
        }
    }

    @Test
    public void testLeafLabels() {
        final String[] leafLabels = sampleTree.leafLabels();
        assertArrayEquals(new String[] { "a", "c", "e", "g", "h", "j" }, leafLabels);
    }

    @Test
    public void testUnaryChainHeight() {
        assertEquals(0, NaryTree.read("(a b)", String.class).unaryChainHeight());
        assertEquals(0, NaryTree.read("(a b c)", String.class).unaryChainHeight());
        assertEquals(1, NaryTree.read("(a (b c))", String.class).unaryChainHeight());
        assertEquals(2, NaryTree.read("(a (b (c d e)))", String.class).unaryChainHeight());
        assertEquals(2, NaryTree.read("(a (b (c d (e f))))", String.class).unaryChainHeight());
    }

    @Test
    public void testUnaryChainDepth() {
        NaryTree<String> tree = NaryTree.read("(a b)", String.class);
        assertEquals(0, tree.unaryChainDepth());
        assertEquals(1, tree.children().getFirst().unaryChainDepth());

        tree = NaryTree.read("(a b c)", String.class);
        assertEquals(0, tree.unaryChainDepth());
        assertEquals(0, tree.children().getFirst().unaryChainDepth());

        tree = NaryTree.read("(a (b c))", String.class);
        assertEquals(0, tree.unaryChainDepth());
        assertEquals(1, tree.children().getFirst().unaryChainDepth());
        assertEquals(2, tree.children().getFirst().children().getFirst().unaryChainDepth());

        tree = NaryTree.read("(a ((b (c d e)))", String.class);
        assertEquals(1, tree.children().getFirst().unaryChainDepth());
        assertEquals(2, tree.children().getFirst().children().getFirst().unaryChainDepth());
        assertEquals(0, tree.children().getFirst().children().getFirst().children().getFirst().unaryChainDepth());
    }
}
