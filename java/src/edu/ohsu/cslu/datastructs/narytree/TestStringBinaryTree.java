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

import edu.ohsu.cslu.grammar.GrammarFormatType;

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

    private final static String[] SAMPLE_IN_ORDER_ARRAY = new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i",
            "j", "k" };
    private final static String[] SAMPLE_PRE_ORDER_ARRAY = new String[] { "f", "d", "b", "a", "c", "e", "i", "h", "g",
            "k", "j" };
    private final static String[] SAMPLE_POST_ORDER_ARRAY = new String[] { "a", "c", "b", "e", "d", "g", "h", "j", "k",
            "i", "f" };
    private final static String[] SAMPLE_LEAF_ARRAY = new String[] { "a", "c", "e", "g", "j" };

    @Before
    public void setUp() {
        sampleTree = new BinaryTree<String>("f", null);

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
        // Test adding by label
        BinaryTree<String> tree = new BinaryTree<String>("a", null);
        assertEquals(1, tree.size());
        tree.addChild("b");
        assertEquals(1, tree.leaves());
        assertEquals(2, tree.size());
        assertNull(tree.child("a"));
        assertNotNull(tree.child("b"));

        // Test adding a subtree
        tree = new BinaryTree<String>("a", null);
        final BinaryTree<String> tmp = new BinaryTree<String>("b", null);
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
        final BinaryTree<String> tree = new BinaryTree<String>("a", null);
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

        sampleTree.removeChild("i");
        assertEquals(6, sampleTree.size());

        // Removing the "i" node should remove its children as well
        assertNull(sampleTree.child("i"));
        assertNull(sampleTree.child("h"));

        sampleTree.removeChild("d");
        assertEquals(1, sampleTree.size());
        assertNull(sampleTree.child("d"));
    }

    @Test
    public void testRemoveChildrenByStringArray() throws Exception {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.child("h"));
        assertNull(sampleTree.child("j"));

        // Removing the "d" node should move the right child ("i") to the left child (there is no "g" subtree)
        sampleTree.removeChildren(new String[] { "d", "g" });
        assertEquals(6, sampleTree.size());

        assertNotNull(sampleTree.child("i"));
    }

    @Test
    public void testWithoutLabels() throws Exception {
        assertEquals(11, sampleTree.size());

        BinaryTree<String> newTree = sampleTree.withoutLabels(Arrays.asList(new String[] { "e", "g" }));
        assertEquals(9, newTree.size());

        newTree = newTree.withoutLabels(new String[] { "i", "c", "." });
        assertEquals(4, newTree.size());
    }

    @Test
    public void testRemoveSubtree() throws Exception {
        assertEquals(11, sampleTree.size());

        sampleTree.removeSubtree("g");
        assertEquals(11, sampleTree.size());

        // Removing the "d" node should remove all its children as well
        sampleTree.removeSubtree("d");
        assertEquals(6, sampleTree.size());
        assertNull(sampleTree.child("d"));
        assertNull(sampleTree.child("e"));
    }

    @Test
    public void testSubtree() throws Exception {
        final BinaryTree<String> subtree = sampleTree.child("d");
        assertEquals(5, subtree.size());
        assertNotNull(subtree.child("b"));
        assertNotNull(subtree.child("e"));
        assertEquals(3, subtree.child("b").size());

        assertEquals(5, sampleTree.child("i").size());
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(11, sampleTree.size());
        sampleTree.child("i").removeSubtree("k");
        assertEquals(9, sampleTree.size());
        sampleTree.child("d").child("e").addChild("z");
        assertEquals(10, sampleTree.size());
    }

    @Test
    public void testDepthFromRoot() throws Exception {
        assertEquals(0, sampleTree.depthFromRoot());
        assertEquals(1, sampleTree.child("d").depthFromRoot());
        assertEquals(2, sampleTree.child("i").child("k").depthFromRoot());
        assertEquals(3, sampleTree.child("i").child("k").child("j").depthFromRoot());
    }

    @Test
    public void testHeight() throws Exception {
        assertEquals(4, sampleTree.height());
        assertEquals(3, sampleTree.leftChild().height());
        assertEquals(3, sampleTree.rightChild().height());

        sampleTree.child("i").removeChild("k");
        sampleTree.child("i").child("h").removeChild("g");
        assertEquals(2, sampleTree.rightChild().height());

        sampleTree.child("i").child("h").addChild("z");
        assertEquals(3, sampleTree.rightChild().height());
        assertEquals(4, sampleTree.height());
    }

    @Test
    public void testLeaves() throws Exception {
        assertEquals(5, sampleTree.leaves());
        assertEquals(3, sampleTree.child("d").leaves());

        sampleTree.child("i").child("h").removeChild("g");
        assertEquals(5, sampleTree.leaves());

        sampleTree.child("i").removeSubtree("h");
        assertEquals(4, sampleTree.leaves());

        sampleTree.removeSubtree("d");
        assertEquals(1, sampleTree.leaves());
    }

    @Test
    public void testIsLeaf() throws Exception {
        assertFalse(sampleTree.isLeaf());
        assertFalse(sampleTree.child("d").isLeaf());
        assertTrue(sampleTree.child("i").child("h").child("g").isLeaf());
    }

    @Test
    public void testIsLeftmostChild() throws Exception {
        final Iterator<BinaryTree<String>> i = sampleTree.inOrderTraversal().iterator();
        assertTrue(i.next().isLeftmostChild()); // a
        assertTrue(i.next().isLeftmostChild()); // b
        assertFalse(i.next().isLeftmostChild()); // c
        assertTrue(i.next().isLeftmostChild()); // d
        assertFalse(i.next().isLeftmostChild()); // e
        assertFalse(i.next().isLeftmostChild()); // f
        assertTrue(i.next().isLeftmostChild()); // g
        assertTrue(i.next().isLeftmostChild()); // h
        assertFalse(i.next().isLeftmostChild()); // i
        assertFalse(i.next().isLeftmostChild()); // j
        assertFalse(i.next().isLeftmostChild()); // k
    }

    @Test
    public void testIsRightmostChild() throws Exception {
        final Iterator<BinaryTree<String>> i = sampleTree.inOrderTraversal().iterator();
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
    public void testInOrderLabelTraversal() throws Exception {
        int i = 0;
        for (final String label : sampleTree.inOrderLabelTraversal()) {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i++], label);
        }
    }

    @Test
    public void testPreOrderTraversal() throws Exception {
        int i = 0;
        for (final BinaryTree<String> tree : sampleTree.preOrderTraversal()) {
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
        for (final BinaryTree<String> tree : sampleTree.postOrderTraversal()) {
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
        for (final BinaryTree<String> leaf : sampleTree.leafTraversal()) {
            assertEquals(SAMPLE_LEAF_ARRAY[i++], leaf.label());
        }
    }

    @Test
    public void testReadFromReader() throws Exception {

        final String stringSimpleTree = "(a (b c) d)";
        final BinaryTree<String> simpleTree = BinaryTree.read(new StringReader(stringSimpleTree), String.class);
        assertEquals(4, simpleTree.size());
        assertEquals(2, simpleTree.child("b").size());

        assertEquals("a", simpleTree.label());
        assertEquals("b", simpleTree.child("b").label());
        assertEquals("c", simpleTree.child("b").child("c").label());
        assertEquals("d", simpleTree.child("d").label());

        final String stringTestTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final BinaryTree<String> testTree = BinaryTree.read(new StringReader(stringTestTree), String.class);
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

        final BinaryTree<String> tree = BinaryTree.read(new StringReader(stringSampleTree), String.class);
        assertEquals(sampleTree, tree);
    }

    @Test
    public void testWriteToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        sampleTree.write(writer);
        assertEquals(stringSampleTree, writer.toString());

        final String stringSimpleTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final BinaryTree<String> tree = new BinaryTree<String>("a", null);
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
        final BinaryTree<String> tree1 = new BinaryTree<String>("a", null);
        tree1.addChildren(new String[] { "b", "c" });

        final BinaryTree<String> tree2 = new BinaryTree<String>("a", null);
        tree2.addChildren(new String[] { "b", "c" });

        final BinaryTree<String> tree3 = new BinaryTree<String>("a", null);
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
        final BinaryTree<String> t = (BinaryTree<String>) ois.readObject();
        assertTrue(sampleTree.equals(t));
    }

    @Test
    public void testUnfactor() throws Exception {
        BinaryTree<String> binaryTree = BinaryTree.read("(A B C)", String.class);
        assertEquals(NaryTree.read("(A B C)", String.class), binaryTree.unfactor(GrammarFormatType.Berkeley));

        binaryTree = BinaryTree.read("(A B (@A C D))", String.class);
        assertEquals(NaryTree.read("(A B C D)", String.class), binaryTree.unfactor(GrammarFormatType.Berkeley));

        binaryTree = BinaryTree.read("(A (@A B C) D)", String.class);
        assertEquals(NaryTree.read("(A B C D)", String.class), binaryTree.unfactor(GrammarFormatType.Berkeley));

        binaryTree = BinaryTree.read(
                "(ROOT_0 (NP_31 (@NP_29 (@NP_40 (:_3 --) (NNP_0 C.E.)) (NNP_9 Friedman)) (._3 .)))", String.class);
        assertEquals(NaryTree.read("(ROOT (NP (: --) (NNP C.E.) (NNP Friedman) (. .)))", String.class),
                binaryTree.unfactor(GrammarFormatType.Berkeley));

        // Ensure we don't mangle tokens while unfactoring
        binaryTree = BinaryTree.read("(ROOT_0 (NP (NN_31 http://foo.com/foo_bar)))", String.class);
        assertEquals(NaryTree.read("(ROOT (NP (NN http://foo.com/foo_bar)))", String.class),
                binaryTree.unfactor(GrammarFormatType.Berkeley));

        binaryTree = BinaryTree.read("(ROOT_0 (S_0 (@S_24 (NP_23 (NN_26 Trouble)) (VP_32 (@VP_10 (VBZ_17 is) (,_0 ,))"
                + " (SBAR_1 (S_5 (NP_36 (PRP_2 she)) (VP_34 (VBZ_16 has) (VP_11 (@VP_28"
                + " (VBN_23 lost) (NP_37 (PRP_1 it))) (ADVP_1 (@ADVP_0 (RB_31 just)"
                + " (RB_32 as)) (RB_2 quickly)))))))) (._3 .)))", String.class);
        assertEquals(NaryTree.read(
                "(ROOT (S (NP (NN Trouble)) (VP (VBZ is) (, ,) (SBAR (S (NP (PRP she)) (VP (VBZ has) (VP (VBN lost) ("
                        + "NP (PRP it)) (ADVP (RB just) (RB as) (RB quickly))))))) (. .)))", String.class),
                binaryTree.unfactor(GrammarFormatType.Berkeley));

        binaryTree = BinaryTree.read("(TOP (S^<TOP> (S|<NP-VP>^<TOP> (NP^<S> (NP^<NP> (JJ Little) (NN chance))"
                + " (PP^<NP> (IN that) (NP^<PP> (NNP Shane) (NNP Longman))))"
                + " (VP^<S> (AUX is) (VP^<VP> (VBG going) (S^<VP> (VP^<S> (TO to)"
                + " (VP^<VP> (VB recoup) (NP^<VP> (NN today)))))))) (. .)))", String.class);
        assertEquals(NaryTree.read("(TOP (S (NP (NP (JJ Little) (NN chance)) (PP (IN that) (NP (NNP Shane)"
                + " (NNP Longman)))) (VP (AUX is) (VP (VBG going) (S (VP (TO to) (VP (VB recoup) "
                + "(NP (NN today))))))) (. .)))", String.class), binaryTree.unfactor(GrammarFormatType.CSLU));
    }

    @Test
    public void testMaxUnaryChainLength() {
        assertEquals(0, BinaryTree.read("(a b)", String.class).maxUnaryChainLength());
        assertEquals(0, BinaryTree.read("(a b c)", String.class).maxUnaryChainLength());
        assertEquals(1, BinaryTree.read("(a (b c))", String.class).maxUnaryChainLength());
        assertEquals(2, BinaryTree.read("(a (b (c d)))", String.class).maxUnaryChainLength());
        assertEquals(2, BinaryTree.read("(a (b (c (d e))) f)", String.class).maxUnaryChainLength());

        /**
         * <pre>
         *             f 
         *             |
         *       --------------
         *       |           |
         *       d           i
         *       |           |
         *       d           h
         *       |           |
         *    -------        g
         *    |     |        |
         *    b     e     --------
         *    |     |     |      |
         *    a     c     h      k
         *                |      |
         *                g      j
         * </pre>
         */
        assertEquals(2, BinaryTree.read("(f (d (d (b a) (e c))) (i (h (g (h g) (k j)))))", String.class)
                .maxUnaryChainLength());
    }

    @Test
    public void testUnaryChainHeight() {
        assertEquals(0, BinaryTree.read("(a b)", String.class).unaryChainHeight());
        assertEquals(0, BinaryTree.read("(a b c)", String.class).unaryChainHeight());
        assertEquals(1, BinaryTree.read("(a (b c))", String.class).unaryChainHeight());
        assertEquals(2, BinaryTree.read("(a (b (c d e)))", String.class).unaryChainHeight());
        assertEquals(2, BinaryTree.read("(a (b (c d (e f))))", String.class).unaryChainHeight());
    }

    @Test
    public void testUnaryChainDepth() {
        BinaryTree<String> tree = BinaryTree.read("(a b)", String.class);
        assertEquals(0, tree.unaryChainDepth());
        assertEquals(1, tree.leftChild().unaryChainDepth());

        tree = BinaryTree.read("(a b c)", String.class);
        assertEquals(0, tree.unaryChainDepth());
        assertEquals(0, tree.leftChild().unaryChainDepth());

        tree = BinaryTree.read("(a (b c))", String.class);
        assertEquals(0, tree.unaryChainDepth());
        assertEquals(1, tree.leftChild().unaryChainDepth());
        assertEquals(2, tree.leftChild().leftChild().unaryChainDepth());

        tree = BinaryTree.read("(a ((b (c d e)))", String.class);
        assertEquals(1, tree.leftChild().unaryChainDepth());
        assertEquals(2, tree.leftChild().leftChild().unaryChainDepth());
        assertEquals(0, tree.leftChild().leftChild().leftChild().unaryChainDepth());
    }

    @Test
    public void testClone() {
        final BinaryTree<String> clone = sampleTree.clone();
        assertFalse(sampleTree == clone);
        assertEquals(sampleTree, clone);
    }

    @Test
    public void testTransform() {
        assertEquals("(F (D (B A C) E) (I (H G) (K J)))", sampleTree.transform(new Tree.LabelTransformer<String>() {
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
        assertEquals(sampleTree.size(), spans.size());
        assertMapContains(spans, "0,1,a", 1);
        assertMapContains(spans, "1,2,c", 1);
        assertMapContains(spans, "0,2,b", 1);
        assertMapContains(spans, "2,3,e", 1);
        assertMapContains(spans, "0,3,d", 1);
        assertMapContains(spans, "3,4,h", 1);
        assertMapContains(spans, "3,4,g", 1);
        assertMapContains(spans, "3,5,i", 1);
        assertMapContains(spans, "4,5,k", 1);
        assertMapContains(spans, "4,5,j", 1);
        assertMapContains(spans, "0,5,f", 1);

        spans = sampleTree.labeledSpans(2);
        assertEquals(6, spans.size());
        assertMapContains(spans, "0,2,b", 1);
        assertMapContains(spans, "0,3,d", 1);
        assertMapContains(spans, "3,5,i", 1);
        assertMapContains(spans, "0,5,f", 1);
        assertMapContains(spans, "3,4,h", 1);
        assertMapContains(spans, "4,5,k", 1);

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
        assertMapContains(spans, "3,4,i", 1);
        assertMapContains(spans, "3,4,g", 1);
        assertMapContains(spans, "3,5,i", 1);
        assertMapContains(spans, "4,5,j", 2);
        assertMapContains(spans, "0,5,f", 1);
    }

    @Test
    public void testLeftmostLeaf() {
        assertEquals("a", sampleTree.leftmostLeaf().label());
        assertEquals("a", sampleTree.child("d").leftmostLeaf().label());
        assertEquals("e", sampleTree.child("d").child("e").leftmostLeaf().label());
        assertEquals("g", sampleTree.child("i").leftmostLeaf().label());
        assertEquals("j", sampleTree.child("i").child("k").leftmostLeaf().label());
    }

    @Test
    public void testRightmostLeaf() {
        assertEquals("j", sampleTree.rightmostLeaf().label());
        assertEquals("e", sampleTree.child("d").rightmostLeaf().label());
        assertEquals("e", sampleTree.child("d").child("e").rightmostLeaf().label());
        assertEquals("j", sampleTree.child("i").rightmostLeaf().label());
        assertEquals("j", sampleTree.child("i").child("k").rightmostLeaf().label());
    }

    @Test
    public void testReplaceLeafLabels() {
        BinaryTree<String> t = sampleTree.clone();
        t.replaceLeafLabels(Arrays.asList(new String[] { "v", "w", "x", "y", "z" }));
        assertEquals("(f (d (b v w) x) (i (h y) (k z)))", t.toString());

        t = sampleTree.clone();
        t.replaceLeafLabels(new String[] { "v", "w", "x", "y", "z" });
        assertEquals("(f (d (b v w) x) (i (h y) (k z)))", t.toString());

        try {
            sampleTree.replaceLeafLabels(new String[] { "u", "v", "w", "x", "y", "z" });
            fail("Expected IllegalArgumentException");
        } catch (final IllegalArgumentException expected) {
        }
    }

    @Test
    public void testLeafLabels() {
        final String[] leafLabels = sampleTree.leafLabels();
        assertArrayEquals(new String[] { "a", "c", "e", "g", "j" }, leafLabels);
    }
}
