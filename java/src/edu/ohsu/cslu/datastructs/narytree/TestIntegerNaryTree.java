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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;

import org.cjunit.FilteredRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link NaryTree} class with integers. Also tests the static inner class {@link NaryTree.PqgramProfile}
 * .
 * 
 * @author Aaron Dunlop
 * @since Sep 19, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestIntegerNaryTree {

    private NaryTree<Integer> sampleTree;
    private String stringSampleTree;

    private final static int[] SAMPLE_IN_ORDER_ARRAY = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
    private final static int[] SAMPLE_PRE_ORDER_ARRAY = new int[] { 6, 4, 2, 1, 3, 5, 7, 9, 8, 11, 10 };
    private final static int[] SAMPLE_POST_ORDER_ARRAY = new int[] { 1, 3, 2, 5, 4, 7, 8, 10, 11, 9, 6 };

    private ShiftRegister<Integer> register1;
    private ShiftRegister<Integer> register2;
    private ShiftRegister<Integer> register3;
    private ShiftRegister<Integer> register4;

    private final NaryTree.PqgramProfile<Integer> bag1 = new NaryTree.PqgramProfile<Integer>();
    private final NaryTree.PqgramProfile<Integer> bag2 = new NaryTree.PqgramProfile<Integer>();
    private final NaryTree.PqgramProfile<Integer> bag3 = new NaryTree.PqgramProfile<Integer>();

    @Before
    public void setUp() {
        sampleTree = new NaryTree<Integer>(6);

        NaryTree<Integer> tmp1 = new NaryTree<Integer>(2);
        tmp1.addChild(1);
        tmp1.addChild(3);

        NaryTree<Integer> tmp2 = new NaryTree<Integer>(4);
        tmp2.addChild(tmp1);
        tmp2.addChild(5);

        sampleTree.addChild(tmp2);
        sampleTree.addChild(7);

        tmp1 = new NaryTree<Integer>(9);
        tmp1.addChild(8);
        tmp2 = tmp1.addChild(11);
        tmp2.addChild(10);
        sampleTree.addChild(tmp1);

        stringSampleTree = "(6 (4 (2 1 3) 5) 7 (9 8 (11 10)))";

        register1 = new ShiftRegister<Integer>(new Integer[] { 1, 2, 3, 4 });
        register2 = new ShiftRegister<Integer>(new Integer[] { 11, 12, 13, 14 });
        register3 = new ShiftRegister<Integer>(new Integer[] { 21, 22, 23, 24 });
        register4 = new ShiftRegister<Integer>(new Integer[] { 31, 32, 33, 34 });

        bag1.add(register1);

        bag2.add(register2);
        bag2.add(register3);

        bag3.add(register1);
        bag3.add(register1);
        bag3.add(register3);
        bag3.add(register3);
        bag3.add(register4);
    }

    @Test
    public void testAddChild() throws Exception {
        // Test adding children by label
        NaryTree<Integer> tree = new NaryTree<Integer>(1);
        assertEquals(1, tree.size());
        tree.addChild(2);
        assertEquals(2, tree.size());
        assertNull(tree.child(1));
        assertNotNull(tree.child(2));

        // Test adding subtrees
        tree = new NaryTree<Integer>(1);
        final NaryTree<Integer> tmp = new NaryTree<Integer>(2);
        tmp.addChildren(new Integer[] { new Integer(3), new Integer(4) });
        tree.addChild(tmp);
        assertEquals(4, tree.size());
        assertNotNull(tree.child(2));
        assertNotNull(tree.child(2).child(3));
        assertNotNull(tree.child(2).child(4));
    }

    @Test
    public void testAddChildren() throws Exception {
        final NaryTree<Integer> tree = new NaryTree<Integer>(1);
        assertEquals(1, tree.size());
        tree.addChildren(new Integer[] { new Integer(2), new Integer(3) });
        assertEquals(3, tree.size());
        assertNull(tree.child(1));
        assertNotNull(tree.child(2));
        assertNotNull(tree.child(3));
    }

    @Test
    public void testRemoveChild() throws Exception {
        assertEquals(11, sampleTree.size());

        sampleTree.removeChild(7);
        assertEquals(10, sampleTree.size());

        sampleTree.removeChild(4);
        assertEquals(5, sampleTree.size());
    }

    @Test
    public void testRemoveChildrenByIntegerArray() throws Exception {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.child(8));
        assertNull(sampleTree.child(10));

        // Removing the '9' node should move its children up ('7' has no children)
        sampleTree.removeChildren(new Integer[] { new Integer(7), new Integer(9) });
        assertEquals(6, sampleTree.size());
    }

    @Test
    public void testSubtree() throws Exception {
        final NaryTree<Integer> subtree = sampleTree.child(4);
        assertEquals(5, subtree.size());
        assertNotNull(subtree.child(2));
        assertNotNull(subtree.child(5));
        assertEquals(3, subtree.child(2).size());

        assertEquals(4, sampleTree.child(9).size());
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(11, sampleTree.size());
        sampleTree.child(9).removeChild(11);
        assertEquals(9, sampleTree.size());
        sampleTree.child(7).addChild(1000);
        assertEquals(10, sampleTree.size());
    }

    @Test
    public void testDepthFromRoot() throws Exception {
        assertEquals(0, sampleTree.depthFromRoot());
        assertEquals(1, sampleTree.child(4).depthFromRoot());
        assertEquals(2, sampleTree.child(9).child(11).depthFromRoot());
        assertEquals(3, sampleTree.child(9).child(11).child(10).depthFromRoot());
    }

    @Test
    public void testLeaves() throws Exception {
        assertEquals(6, sampleTree.leaves());
        assertEquals(3, sampleTree.child(4).leaves());

        sampleTree.child(7).addChild(71);
        assertEquals(6, sampleTree.leaves());

        sampleTree.child(7).removeChild(72);
        assertEquals(6, sampleTree.leaves());

        sampleTree.child(9).removeChild(11);
        assertEquals(5, sampleTree.leaves());

        sampleTree.removeChild(4);
        assertEquals(2, sampleTree.leaves());
    }

    @Test
    public void testInOrderTraversal() throws Exception {
        int i = 0;
        for (final NaryTree<Integer> tree : sampleTree.inOrderTraversal()) {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i++], tree.label().intValue());
        }
    }

    @Test
    public void testInOrderLabelTraversal() throws Exception {
        int i = 0;
        for (final int label : sampleTree.inOrderLabelTraversal()) {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i++], label);
        }
    }

    @Test
    public void testPreOrderTraversal() throws Exception {
        int i = 0;
        for (final NaryTree<Integer> tree : sampleTree.preOrderTraversal()) {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i++], tree.label().intValue());
        }
    }

    @Test
    public void testPreOrderLabelTraversal() throws Exception {
        int i = 0;
        for (final int label : sampleTree.preOrderLabelTraversal()) {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i++], label);
        }
    }

    @Test
    public void testPostOrderTraversal() throws Exception {
        int i = 0;
        for (final NaryTree<Integer> tree : sampleTree.postOrderTraversal()) {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i++], tree.label().intValue());
        }
    }

    @Test
    public void testPostOrderLabelTraversal() throws Exception {
        int i = 0;
        for (final int label : sampleTree.postOrderLabelTraversal()) {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i++], label);
        }
    }

    @Test
    public void testReadFromReader() throws Exception {

        final String stringSimpleTree = "(1 (2 3) 4)";
        final NaryTree<Integer> simpleTree = NaryTree.read(new StringReader(stringSimpleTree), Integer.class);
        assertEquals(4, simpleTree.size());
        assertEquals(2, simpleTree.child(2).size());

        assertEquals(1, simpleTree.label().intValue());
        assertEquals(2, simpleTree.child(2).label().intValue());
        assertEquals(3, simpleTree.child(2).child(3).label().intValue());
        assertEquals(4, simpleTree.child(4).label().intValue());

        final String stringTestTree = "(1 (2 (3 (4 (5 6) (7 8)) (9 10)) (11 12)))";
        final NaryTree<Integer> testTree = NaryTree.read(new StringReader(stringTestTree), Integer.class);
        assertEquals(12, testTree.size());
        assertEquals(8, testTree.child(2).child(3).size());

        assertEquals(1, testTree.label().intValue());
        assertEquals(2, testTree.child(2).label().intValue());
        assertEquals(3, testTree.child(2).child(3).label().intValue());
        assertEquals(4, testTree.child(2).child(3).child(4).label().intValue());
        assertEquals(5, testTree.child(2).child(3).child(4).child(5).label().intValue());
        assertEquals(6, testTree.child(2).child(3).child(4).child(5).child(6).label().intValue());
        assertEquals(7, testTree.child(2).child(3).child(4).child(7).label().intValue());
        assertEquals(8, testTree.child(2).child(3).child(4).child(7).child(8).label().intValue());
        assertEquals(9, testTree.child(2).child(3).child(9).label().intValue());
        assertEquals(10, testTree.child(2).child(3).child(9).child(10).label().intValue());
        assertEquals(11, testTree.child(2).child(11).label().intValue());
        assertEquals(12, testTree.child(2).child(11).child(12).label().intValue());

        final NaryTree<Integer> tree = NaryTree.read(new StringReader(stringSampleTree), Integer.class);
        assertEquals(sampleTree, tree);
    }

    @Test
    public void testWriteToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        sampleTree.write(writer);
        assertEquals(stringSampleTree, writer.toString());

        final String stringSimpleTree = "(1 (2 (3 (4 (5 6) (7 8)) (9 10)) (11 12)))";
        final NaryTree<Integer> tree = new NaryTree<Integer>(1);
        tree.addChild(2).addChild(3).addChild(4).addChild(5).addChild(6);
        tree.child(2).child(3).child(4).addChild(7).addChild(8);
        tree.child(2).child(3).addChild(9).addChild(10);
        tree.child(2).addChild(11).addChild(12);

        writer = new StringWriter();
        tree.write(writer);
        assertEquals(stringSimpleTree, writer.toString());
    }

    @Test
    public void testIsLeaf() throws Exception {
        assertFalse(sampleTree.isLeaf());
        assertFalse(sampleTree.child(4).isLeaf());
        assertTrue(sampleTree.child(7).isLeaf());
        assertTrue(sampleTree.child(9).child(8).isLeaf());
    }

    @Test
    public void testEquals() throws Exception {
        final NaryTree<Integer> tree1 = new NaryTree<Integer>(new Integer(1));
        tree1.addChildren(new Integer[] { new Integer(2), new Integer(3) });

        final NaryTree<Integer> tree2 = new NaryTree<Integer>(1);
        tree2.addChildren(new Integer[] { new Integer(2), new Integer(3) });

        final NaryTree<Integer> tree3 = new NaryTree<Integer>(1);
        tree3.addChildren(new Integer[] { new Integer(2), new Integer(4) });

        assertTrue(tree1.equals(tree2));
        assertFalse(tree1.equals(tree3));

        tree2.child(3).addChild(4);
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
        final NaryTree<Integer> t = (NaryTree<Integer>) ois.readObject();
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
        final NaryTree<Integer> t1 = new NaryTree<Integer>(1);
        NaryTree<Integer> tmp = t1.addChild(1);
        tmp.addChild(5);
        tmp.addChild(2);
        t1.addChild(2);
        t1.addChild(3);

        final NaryTree<Integer> t2 = new NaryTree<Integer>(1);
        tmp = t2.addChild(1);
        tmp.addChild(5);
        tmp.addChild(2);
        t2.addChild(2);
        t2.addChild(24);

        assertEquals(.31, t1.pqgramDistance(t2, 2, 3), .01f);

        // Example taken from Augsten, Bohlen, Gamper, 2005, page 308
        final NaryTree<Integer> t = new NaryTree<Integer>(1);
        t.addChild(2);
        tmp = t.addChild(3);
        tmp.addChild(4);
        tmp.addChild(5);
        tmp.addChild(6);
        tmp.addChild(7);
        tmp = tmp.child(5);
        tmp.addChild(8);
        tmp.addChild(9);
        tmp.addChild(10);

        final NaryTree<Integer> tPrime = new NaryTree<Integer>(1);
        tPrime.addChild(2);
        tmp = tPrime.addChild(3);
        tmp.addChild(4);
        tmp.addChild(5);
        tmp.addChild(6);
        tmp = tmp.child(5);
        tmp.addChild(8);
        tmp.addChild(9);

        final NaryTree<Integer> tDoublePrime = new NaryTree<Integer>(1);
        tDoublePrime.addChild(2);
        tDoublePrime.addChild(4);
        tDoublePrime.addChild(8);
        tDoublePrime.addChild(9);
        tDoublePrime.addChild(10);
        tDoublePrime.addChild(6);
        tDoublePrime.addChild(7);

        assertEquals(.3, t.pqgramDistance(tPrime, 2, 3), .01f);
        assertEquals(.89, t.pqgramDistance(tDoublePrime, 2, 3), .01f);
    }

    @Test
    public void testProfileAdd() throws Exception {
        assertFalse(bag1.contains(register3));
        assertEquals(1, bag1.size());
        bag1.add(register3);
        assertTrue(bag1.contains(register3));
        assertEquals(2, bag1.size());
    }

    @Test
    public void testProfileAddAll() throws Exception {
        assertFalse(bag1.contains(register2));
        assertFalse(bag1.contains(register3));
        assertEquals(1, bag1.size());

        final LinkedList<ShiftRegister<Integer>> registers = new LinkedList<ShiftRegister<Integer>>();
        registers.add(register2);
        registers.add(register3);
        bag1.addAll(registers);
        assertTrue(bag1.contains(register2));
        assertTrue(bag1.contains(register3));
        assertEquals(3, bag1.size());
    }

    @Test
    public void testProfileContains() throws Exception {
        assertTrue(bag1.contains(register1));
        assertTrue(bag1.contains(new ShiftRegister<Integer>(new Integer[] { 1, 2, 3, 4 })));
        assertFalse(bag1.contains(register2));
    }

    @Test
    public void testProfileClear() throws Exception {
        assertTrue(bag1.contains(register1));
        assertEquals(1, bag1.size());
        bag1.clear();
        assertFalse(bag1.contains(register1));
        assertEquals(0, bag1.size());
    }

    @Test
    public void testProfileIsEmpty() throws Exception {
        assertFalse(bag1.isEmpty());
        assertEquals(1, bag1.size());
        bag1.clear();
        assertTrue(bag1.isEmpty());
    }

    @Test
    public void testProfileIntersection() throws Exception {
        // An bag intersected with itself
        NaryTree.PqgramProfile<Integer> b = bag3.intersection(bag3);
        assertEquals(5, b.size());
        assertTrue(b.contains(register1));
        assertTrue(b.contains(register3));
        assertTrue(b.contains(register4));

        // An intersection that should be empty
        b = bag1.intersection(bag2);
        assertEquals(0, b.size());
        assertFalse(b.contains(register1));
        assertFalse(b.contains(register2));
        assertFalse(b.contains(register3));

        b = bag1.intersection(bag3);
        assertEquals(1, b.size());
        assertTrue(b.contains(register1));
        assertFalse(b.contains(register2));
        assertFalse(b.contains(register3));
        final NaryTree.PqgramProfile<Integer> b2 = bag3.intersection(bag1);
        assertEquals(1, b2.size());
        assertEquals(b, b2);

        bag2.add(register3);
        b = bag2.intersection(bag3);
        assertEquals(2, b.size());
        assertFalse(b.contains(register1));
        assertFalse(b.contains(register2));
        assertTrue(b.contains(register3));
    }
}
