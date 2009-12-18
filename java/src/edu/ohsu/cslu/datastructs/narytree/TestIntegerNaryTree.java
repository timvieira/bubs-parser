package edu.ohsu.cslu.datastructs.narytree;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

/**
 * Unit tests for {@link IntegerNaryTree} class. Also tests the static inner class
 * {@link BaseNaryTree.PqgramProfile}.
 * 
 * @author Aaron Dunlop
 * @since Sep 19, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestIntegerNaryTree
{
    private IntegerNaryTree sampleTree;
    private String stringSampleTree;

    private final static int[] SAMPLE_IN_ORDER_ARRAY = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    private final static int[] SAMPLE_PRE_ORDER_ARRAY = new int[] {6, 4, 2, 1, 3, 5, 7, 9, 8, 11, 10};
    private final static int[] SAMPLE_POST_ORDER_ARRAY = new int[] {1, 3, 2, 5, 4, 7, 8, 10, 11, 9, 6};

    private IntShiftRegister register1;
    private IntShiftRegister register2;
    private IntShiftRegister register3;
    private IntShiftRegister register4;

    private final BaseNaryTree.PqgramProfile bag1 = new BaseNaryTree.PqgramProfile();
    private final BaseNaryTree.PqgramProfile bag2 = new BaseNaryTree.PqgramProfile();
    private final BaseNaryTree.PqgramProfile bag3 = new BaseNaryTree.PqgramProfile();

    @Before
    public void setUp()
    {
        sampleTree = new IntegerNaryTree(6);

        IntegerNaryTree tmp1 = new IntegerNaryTree(2);
        tmp1.addChild(1);
        tmp1.addChild(3);

        IntegerNaryTree tmp2 = new IntegerNaryTree(4);
        tmp2.addSubtree(tmp1);
        tmp2.addChild(5);

        sampleTree.addSubtree(tmp2);
        sampleTree.addChild(7);

        tmp1 = new IntegerNaryTree(9);
        tmp1.addChild(8);
        tmp2 = tmp1.addChild(11);
        tmp2.addChild(10);
        sampleTree.addSubtree(tmp1);

        stringSampleTree = "(6 (4 (2 1 3) 5) 7 (9 8 (11 10)))";

        register1 = new IntShiftRegister(new int[] {1, 2, 3, 4});
        register2 = new IntShiftRegister(new int[] {11, 12, 13, 14});
        register3 = new IntShiftRegister(new int[] {21, 22, 23, 24});
        register4 = new IntShiftRegister(new int[] {31, 32, 33, 34});

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
    public void testAddChild() throws Exception
    {
        final IntegerNaryTree tree = new IntegerNaryTree(1);
        assertEquals(1, tree.size());
        tree.addChild(2);
        assertEquals(2, tree.size());
        assertNull(tree.subtree(1));
        assertNotNull(tree.subtree(2));
    }

    @Test
    public void testAddChildren() throws Exception
    {
        final IntegerNaryTree tree = new IntegerNaryTree(1);
        assertEquals(1, tree.size());
        tree.addChildren(new int[] {2, 3});
        assertEquals(3, tree.size());
        assertNull(tree.subtree(1));
        assertNotNull(tree.subtree(2));
        assertNotNull(tree.subtree(3));
    }

    @Test
    public void testAddSubtree() throws Exception
    {
        final IntegerNaryTree tree = new IntegerNaryTree(1);
        final IntegerNaryTree tmp = new IntegerNaryTree(2);
        tmp.addChildren(new int[] {3, 4});
        tree.addSubtree(tmp);
        assertEquals(4, tree.size());
        assertNotNull(tree.subtree(2));
        assertNotNull(tree.subtree(2).subtree(3));
        assertNotNull(tree.subtree(2).subtree(4));
    }

    @Test
    public void testRemoveChild() throws Exception
    {
        assertEquals(11, sampleTree.size());

        sampleTree.removeChild(7);
        assertEquals(10, sampleTree.size());

        // Removing the '4' node should move its children up
        assertNull(sampleTree.subtree(2));
        assertNull(sampleTree.subtree(5));

        sampleTree.removeChild(4);
        assertEquals(9, sampleTree.size());
        assertNotNull(sampleTree.subtree(2));
        assertNotNull(sampleTree.subtree(5));

        // TODO: Validate that the children were inserted at the proper place with iteration order
    }

    @Test
    public void testRemoveChildrenByIntArray() throws Exception
    {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.subtree(8));
        assertNull(sampleTree.subtree(10));

        // Removing the '9' node should move its children up ('7' has no children)
        sampleTree.removeChildren(new int[] {7, 9});
        assertEquals(9, sampleTree.size());

        assertNotNull(sampleTree.subtree(8));
        assertNotNull(sampleTree.subtree(11));
    }

    @Test
    public void testRemoveChildrenByIntegerArray() throws Exception
    {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.subtree(8));
        assertNull(sampleTree.subtree(10));

        // Removing the '9' node should move its children up ('7' has no children)
        sampleTree.removeChildren(new Integer[] {new Integer(7), new Integer(9)});
        assertEquals(9, sampleTree.size());

        assertNotNull(sampleTree.subtree(8));
        assertNotNull(sampleTree.subtree(11));
    }

    @Test
    public void testRemoveSubtree() throws Exception
    {
        assertEquals(11, sampleTree.size());

        sampleTree.removeSubtree(7);
        assertEquals(10, sampleTree.size());

        // Removing the '4' node should remove all its children as well
        sampleTree.removeSubtree(4);
        assertEquals(5, sampleTree.size());
        assertNull(sampleTree.subtree(2));
        assertNull(sampleTree.subtree(5));
    }

    @Test
    public void testSubtree() throws Exception
    {
        final BaseNaryTree<Integer> subtree = sampleTree.subtree(4);
        assertEquals(5, subtree.size());
        assertNotNull(subtree.subtree(2));
        assertNotNull(subtree.subtree(5));
        assertEquals(3, subtree.subtree(2).size());

        assertEquals(4, sampleTree.subtree(9).size());
    }

    @Test
    public void testSize() throws Exception
    {
        assertEquals(11, sampleTree.size());
        sampleTree.subtree(9).removeSubtree(11);
        assertEquals(9, sampleTree.size());
        sampleTree.subtree(7).addChild(1000);
        assertEquals(10, sampleTree.size());
    }

    @Test
    public void testDepthFromRoot() throws Exception
    {
        assertEquals(0, sampleTree.depthFromRoot());
        assertEquals(1, sampleTree.subtree(4).depthFromRoot());
        assertEquals(2, sampleTree.subtree(9).subtree(11).depthFromRoot());
        assertEquals(3, sampleTree.subtree(9).subtree(11).subtree(10).depthFromRoot());
    }

    @Test
    public void testLeaves() throws Exception
    {
        assertEquals(6, sampleTree.leaves());
        assertEquals(3, sampleTree.subtree(4).leaves());

        sampleTree.subtree(7).addChild(71);
        assertEquals(6, sampleTree.leaves());

        sampleTree.subtree(7).removeChild(72);
        assertEquals(6, sampleTree.leaves());

        sampleTree.subtree(9).removeSubtree(11);
        assertEquals(5, sampleTree.leaves());

        sampleTree.removeSubtree(4);
        assertEquals(2, sampleTree.leaves());
    }

    @Test
    public void testInOrderIterator() throws Exception
    {
        final Iterator<NaryTree<Integer>> iter = sampleTree.inOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            final BaseNaryTree<Integer> t = (BaseNaryTree<Integer>) iter.next();
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i], t.label);
        }
    }

    @Test
    public void testInOrderLabelIterator() throws Exception
    {
        final Iterator<Integer> iter = sampleTree.inOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i], iter.next().intValue());
        }
    }

    @Test
    public void testInOrderArray() throws Exception
    {
        assertArrayEquals(SAMPLE_IN_ORDER_ARRAY, sampleTree.inOrderArray());
    }

    @Test
    public void testPreOrderIterator() throws Exception
    {
        final Iterator<NaryTree<Integer>> iter = sampleTree.preOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            final BaseNaryTree<Integer> t = (BaseNaryTree<Integer>) iter.next();
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i], t.label);
        }
    }

    @Test
    public void testPreOrderLabelIterator() throws Exception
    {
        final Iterator<Integer> iter = sampleTree.preOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i], iter.next().intValue());
        }
    }

    @Test
    public void testPreOrderArray() throws Exception
    {
        assertArrayEquals(SAMPLE_PRE_ORDER_ARRAY, sampleTree.preOrderArray());
    }

    @Test
    public void testPostOrderIterator() throws Exception
    {
        final Iterator<NaryTree<Integer>> iter = sampleTree.postOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            final BaseNaryTree<Integer> t = (BaseNaryTree<Integer>) iter.next();
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i], t.label);
        }
    }

    @Test
    public void testPostOrderLabelIterator() throws Exception
    {
        final Iterator<Integer> iter = sampleTree.postOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i], iter.next().intValue());
        }
    }

    @Test
    public void testPostOrderArray() throws Exception
    {
        assertArrayEquals(SAMPLE_POST_ORDER_ARRAY, sampleTree.postOrderArray());
    }

    @Test
    public void testReadFromReader() throws Exception
    {

        final String stringSimpleTree = "(1 (2 3) 4)";
        final IntegerNaryTree simpleTree = IntegerNaryTree.read(new StringReader(stringSimpleTree));
        assertEquals(4, simpleTree.size());
        assertEquals(2, simpleTree.subtree(2).size());

        assertEquals(1, simpleTree.intLabel());
        assertEquals(2, simpleTree.subtree(2).intLabel());
        assertEquals(3, simpleTree.subtree(2).subtree(3).intLabel());
        assertEquals(4, simpleTree.subtree(4).intLabel());

        final String stringTestTree = "(1 (2 (3 (4 (5 6) (7 8)) (9 10)) (11 12)))";
        final IntegerNaryTree testTree = IntegerNaryTree.read(new StringReader(stringTestTree));
        assertEquals(12, testTree.size());
        assertEquals(8, testTree.subtree(2).subtree(3).size());

        assertEquals(1, testTree.intLabel());
        assertEquals(2, testTree.subtree(2).intLabel());
        assertEquals(3, testTree.subtree(2).subtree(3).intLabel());
        assertEquals(4, testTree.subtree(2).subtree(3).subtree(4).intLabel());
        assertEquals(5, testTree.subtree(2).subtree(3).subtree(4).subtree(5).intLabel());
        assertEquals(6, testTree.subtree(2).subtree(3).subtree(4).subtree(5).subtree(6).intLabel());
        assertEquals(7, testTree.subtree(2).subtree(3).subtree(4).subtree(7).intLabel());
        assertEquals(8, testTree.subtree(2).subtree(3).subtree(4).subtree(7).subtree(8).intLabel());
        assertEquals(9, testTree.subtree(2).subtree(3).subtree(9).intLabel());
        assertEquals(10, testTree.subtree(2).subtree(3).subtree(9).subtree(10).intLabel());
        assertEquals(11, testTree.subtree(2).subtree(11).intLabel());
        assertEquals(12, testTree.subtree(2).subtree(11).subtree(12).intLabel());

        final IntegerNaryTree tree = IntegerNaryTree.read(new StringReader(stringSampleTree));
        assertEquals(sampleTree, tree);
    }

    @Test
    public void testWriteToWriter() throws Exception
    {
        StringWriter writer = new StringWriter();
        sampleTree.write(writer);
        assertEquals(stringSampleTree, writer.toString());

        final String stringSimpleTree = "(1 (2 (3 (4 (5 6) (7 8)) (9 10)) (11 12)))";
        final IntegerNaryTree tree = new IntegerNaryTree(1);
        tree.addChild(2).addChild(3).addChild(4).addChild(5).addChild(6);
        tree.subtree(2).subtree(3).subtree(4).addChild(7).addChild(8);
        tree.subtree(2).subtree(3).addChild(9).addChild(10);
        tree.subtree(2).addChild(11).addChild(12);

        writer = new StringWriter();
        tree.write(writer);
        assertEquals(stringSimpleTree, writer.toString());
    }

    @Test
    public void testIsLeaf() throws Exception
    {
        assertFalse(sampleTree.isLeaf());
        assertFalse(sampleTree.subtree(4).isLeaf());
        assertTrue(sampleTree.subtree(7).isLeaf());
        assertTrue(sampleTree.subtree(9).subtree(8).isLeaf());
    }

    @Test
    public void testEquals() throws Exception
    {
        final BaseNaryTree<Integer> tree1 = new IntegerNaryTree(1);
        tree1.addChildren(new int[] {2, 3});

        final BaseNaryTree<Integer> tree2 = new IntegerNaryTree(1);
        tree2.addChildren(new int[] {2, 3});

        final BaseNaryTree<Integer> tree3 = new IntegerNaryTree(1);
        tree3.addChildren(new int[] {2, 4});

        assertTrue(tree1.equals(tree2));
        assertFalse(tree1.equals(tree3));

        tree2.subtree(3).addChild(4);
        assertFalse(tree1.equals(tree2));
    }

    /**
     * Tests Java serialization and deserialization of trees
     * 
     * @throws Exception if something bad happens
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testSerialize() throws Exception
    {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(sampleTree);

        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        final BaseNaryTree<Integer> t = (BaseNaryTree<Integer>) ois.readObject();
        assertTrue(sampleTree.equals(t));
    }

    /**
     * Tests the pq-gram tree-edit-distance approximation between two trees
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testPqGramSimilarity() throws Exception
    {
        // Example taken from Augsten, Bohlen, Gamper, 2005, page 304
        final IntegerNaryTree t1 = new IntegerNaryTree(1);
        IntegerNaryTree tmp = t1.addChild(1);
        tmp.addChild(5);
        tmp.addChild(2);
        t1.addChild(2);
        t1.addChild(3);

        final IntegerNaryTree t2 = new IntegerNaryTree(1);
        tmp = t2.addChild(1);
        tmp.addChild(5);
        tmp.addChild(2);
        t2.addChild(2);
        t2.addChild(24);

        assertEquals(.31, t1.pqgramDistance(t2, 2, 3), .01f);

        // Example taken from Augsten, Bohlen, Gamper, 2005, page 308
        final IntegerNaryTree t = new IntegerNaryTree(1);
        t.addChild(2);
        tmp = t.addChild(3);
        tmp.addChild(4);
        tmp.addChild(5);
        tmp.addChild(6);
        tmp.addChild(7);
        tmp = (IntegerNaryTree) tmp.subtree(5);
        tmp.addChild(8);
        tmp.addChild(9);
        tmp.addChild(10);

        final IntegerNaryTree tPrime = new IntegerNaryTree(1);
        tPrime.addChild(2);
        tmp = tPrime.addChild(3);
        tmp.addChild(4);
        tmp.addChild(5);
        tmp.addChild(6);
        tmp = (IntegerNaryTree) tmp.subtree(5);
        tmp.addChild(8);
        tmp.addChild(9);

        final IntegerNaryTree tDoublePrime = new IntegerNaryTree(1);
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
    public void testProfileAdd() throws Exception
    {
        assertFalse(bag1.contains(register3));
        assertEquals(1, bag1.size());
        bag1.add(register3);
        assertTrue(bag1.contains(register3));
        assertEquals(2, bag1.size());
    }

    @Test
    public void testProfileAddAll() throws Exception
    {
        assertFalse(bag1.contains(register2));
        assertFalse(bag1.contains(register3));
        assertEquals(1, bag1.size());

        bag1.addAll(Arrays.asList(new IntShiftRegister[] {register2, register3}));
        assertTrue(bag1.contains(register2));
        assertTrue(bag1.contains(register3));
        assertEquals(3, bag1.size());
    }

    @Test
    public void testProfileContains() throws Exception
    {
        assertTrue(bag1.contains(register1));
        assertTrue(bag1.contains(new IntShiftRegister(new int[] {1, 2, 3, 4})));
        assertFalse(bag1.contains(register2));
    }

    @Test
    public void testProfileClear() throws Exception
    {
        assertTrue(bag1.contains(register1));
        assertEquals(1, bag1.size());
        bag1.clear();
        assertFalse(bag1.contains(register1));
        assertEquals(0, bag1.size());
    }

    @Test
    public void testProfileIsEmpty() throws Exception
    {
        assertFalse(bag1.isEmpty());
        assertEquals(1, bag1.size());
        bag1.clear();
        assertTrue(bag1.isEmpty());
    }

    @Test
    public void testProfileIntersection() throws Exception
    {
        // An bag intersected with itself
        BaseNaryTree.PqgramProfile b = bag3.intersection(bag3);
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
        final BaseNaryTree.PqgramProfile b2 = bag3.intersection(bag1);
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
