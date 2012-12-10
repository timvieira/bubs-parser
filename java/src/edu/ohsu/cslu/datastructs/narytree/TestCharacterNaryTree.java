/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
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

import org.cjunit.FilteredRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit tests for {@link NaryTree} with characters.
 * 
 * @author Aaron Dunlop
 * @since Sep 24, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestCharacterNaryTree {

    private NaryTree<Character> sampleTree;
    private String stringSampleTree;

    private final static char[] SAMPLE_IN_ORDER_ARRAY = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k' };
    private final static char[] SAMPLE_PRE_ORDER_ARRAY = new char[] { 'f', 'd', 'b', 'a', 'c', 'e', 'g', 'i', 'h', 'k',
            'j' };
    private final static char[] SAMPLE_POST_ORDER_ARRAY = new char[] { 'a', 'c', 'b', 'e', 'd', 'g', 'h', 'j', 'k',
            'i', 'f' };

    private final static NaryTree.LabelParser<Character> labelParser = new NaryTree.LabelParser<Character>() {
        @Override
        public Character parse(final String label) throws Exception {
            if (label.length() != 1) {
                throw new IllegalArgumentException("Expected single character but found '" + label + "'");
            }
            return label.charAt(0);
        }
    };

    @Before
    public void setUp() {
        sampleTree = new NaryTree<Character>('f');

        NaryTree<Character> tmp1 = new NaryTree<Character>('b');
        tmp1.addChild('a');
        tmp1.addChild('c');

        NaryTree<Character> tmp2 = new NaryTree<Character>('d');
        tmp2.addChild(tmp1);
        tmp2.addChild('e');

        sampleTree.addChild(tmp2);
        sampleTree.addChild('g');

        tmp1 = new NaryTree<Character>('i');
        tmp1.addChild('h');
        tmp2 = tmp1.addChild('k');
        tmp2.addChild('j');
        sampleTree.addChild(tmp1);

        stringSampleTree = "(f (d (b a c) e) g (i h (k j)))";
    }

    @Test
    public void child() throws Exception {
        final NaryTree<Character> tree = new NaryTree<Character>('a');
        assertEquals(1, tree.size());
        tree.addChild('b');
        assertEquals(2, tree.size());
        assertNull(tree.child('a'));
        assertNotNull(tree.child('b'));
    }

    @Test
    public void testAddChildren() throws Exception {
        final NaryTree<Character> tree = new NaryTree<Character>('a');
        assertEquals(1, tree.size());
        tree.addChildren(new Character[] { 'b', 'c' });
        assertEquals(3, tree.size());
        assertNull(tree.child('a'));
        assertNotNull(tree.child('b'));
        assertNotNull(tree.child('c'));
    }

    @Test
    public void testAddChild() throws Exception {
        final NaryTree<Character> tree = new NaryTree<Character>('a');
        final NaryTree<Character> tmp = new NaryTree<Character>('b');
        tmp.addChildren(new Character[] { 'c', 'd' });
        tree.addChild(tmp);
        assertEquals(4, tree.size());
        assertNotNull(tree.child('b'));
        assertNotNull(tree.child('b').child('c'));
        assertNotNull(tree.child('b').child('d'));
    }

    @Test
    public void testRemoveChild() throws Exception {
        assertEquals(11, sampleTree.size());

        sampleTree.removeChild('g');
        assertEquals(10, sampleTree.size());

        // Removing the 'd' node should move its children up
        assertNull(sampleTree.child('b'));
        assertNull(sampleTree.child('e'));

        sampleTree.removeChild('d');
        assertEquals(5, sampleTree.size());
    }

    @Test
    public void testRemoveChildrenByCharArray() throws Exception {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.child('h'));
        assertNull(sampleTree.child('j'));

        // Removing the 'i' node should move its children up ('g' has no children)
        sampleTree.removeChildren(new Character[] { 'g', 'i' });
        assertEquals(6, sampleTree.size());
    }

    @Test
    public void testRemoveChildrenByCharacterArray() throws Exception {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.child('h'));
        assertNull(sampleTree.child('j'));

        // Removing the 'i' node should move its children up ('g' has no children)
        sampleTree.removeChildren(new Character[] { new Character('g'), new Character('i') });
        assertEquals(6, sampleTree.size());
    }

    @Test
    public void testSubtree() throws Exception {
        final NaryTree<Character> subtree = sampleTree.child('d');
        assertEquals(5, subtree.size());
        assertNotNull(subtree.child('b'));
        assertNotNull(subtree.child('e'));
        assertEquals(3, subtree.child('b').size());

        assertEquals(4, sampleTree.child('i').size());
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(11, sampleTree.size());
        sampleTree.child('i').removeChild('k');
        assertEquals(9, sampleTree.size());
        sampleTree.child('g').addChild('l');
        assertEquals(10, sampleTree.size());
    }

    @Test
    public void testDepthFromRoot() throws Exception {
        assertEquals(0, sampleTree.depthFromRoot());
        assertEquals(1, sampleTree.child('d').depthFromRoot());
        assertEquals(2, sampleTree.child('i').child('k').depthFromRoot());
        assertEquals(3, sampleTree.child('i').child('k').child('j').depthFromRoot());
    }

    @Test
    public void testLeaves() throws Exception {
        assertEquals(6, sampleTree.leaves());
        assertEquals(3, sampleTree.child('d').leaves());

        sampleTree.child('g').addChild('y');
        assertEquals(6, sampleTree.leaves());

        sampleTree.child('g').removeChild('z');
        assertEquals(6, sampleTree.leaves());

        sampleTree.child('i').removeChild('k');
        assertEquals(5, sampleTree.leaves());

        sampleTree.removeChild('d');
        assertEquals(2, sampleTree.leaves());
    }

    @Test
    public void testInOrderTraversal() throws Exception {
        int i = 0;
        for (final NaryTree<Character> tree : sampleTree.inOrderTraversal()) {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i++], tree.label().charValue());
        }
    }

    @Test
    public void testInOrderLabelTraversal() throws Exception {
        int i = 0;
        for (final char label : sampleTree.inOrderLabelTraversal()) {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i++], label);
        }
    }

    @Test
    public void testPreOrderTraversal() throws Exception {
        int i = 0;
        for (final NaryTree<Character> tree : sampleTree.preOrderTraversal()) {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i++], tree.label().charValue());
        }
    }

    @Test
    public void testPreOrderLabelTraversal() throws Exception {
        int i = 0;
        for (final char label : sampleTree.preOrderLabelTraversal()) {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i++], label);
        }
    }

    @Test
    public void testPostOrderTraversal() throws Exception {
        int i = 0;
        for (final NaryTree<Character> tree : sampleTree.postOrderTraversal()) {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i++], tree.label().charValue());
        }
    }

    @Test
    public void testPostOrderLabelTraversal() throws Exception {
        int i = 0;
        for (final char label : sampleTree.postOrderLabelTraversal()) {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i++], label);
        }
    }

    @Test
    public void testReadFromReader() throws Exception {

        final String stringSimpleTree = "(a (b c) d)";
        final NaryTree<Character> simpleTree = NaryTree.read(new StringReader(stringSimpleTree), labelParser);
        assertEquals(4, simpleTree.size());
        assertEquals(2, simpleTree.child('b').size());

        assertEquals('a', simpleTree.label().charValue());
        assertEquals('b', simpleTree.child('b').label().charValue());
        assertEquals('c', simpleTree.child('b').child('c').label().charValue());
        assertEquals('d', simpleTree.child('d').label().charValue());

        final String stringTestTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final NaryTree<Character> testTree = NaryTree.read(new StringReader(stringTestTree), labelParser);
        assertEquals(12, testTree.size());
        assertEquals(8, testTree.child('b').child('c').size());

        assertEquals('a', testTree.label().charValue());
        assertEquals('b', testTree.child('b').label().charValue());
        assertEquals('c', testTree.child('b').child('c').label().charValue());
        assertEquals('d', testTree.child('b').child('c').child('d').label().charValue());
        assertEquals('e', testTree.child('b').child('c').child('d').child('e').label().charValue());
        assertEquals('f', testTree.child('b').child('c').child('d').child('e').child('f').label().charValue());
        assertEquals('g', testTree.child('b').child('c').child('d').child('g').label().charValue());
        assertEquals('h', testTree.child('b').child('c').child('d').child('g').child('h').label().charValue());
        assertEquals('i', testTree.child('b').child('c').child('i').label().charValue());
        assertEquals('j', testTree.child('b').child('c').child('i').child('j').label().charValue());
        assertEquals('k', testTree.child('b').child('k').label().charValue());
        assertEquals('l', testTree.child('b').child('k').child('l').label().charValue());

        final NaryTree<Character> tree = NaryTree.read(new StringReader(stringSampleTree), labelParser);
        assertEquals(sampleTree, tree);
    }

    @Test
    public void testWriteToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        sampleTree.write(writer);
        assertEquals(stringSampleTree, writer.toString());

        final String stringSimpleTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final NaryTree<Character> tree = new NaryTree<Character>('a');
        tree.addChild('b').addChild('c').addChild('d').addChild('e').addChild('f');
        tree.child('b').child('c').child('d').addChild('g').addChild('h');
        tree.child('b').child('c').addChild('i').addChild('j');
        tree.child('b').addChild('k').addChild('l');

        writer = new StringWriter();
        tree.write(writer);
        assertEquals(stringSimpleTree, writer.toString());
    }

    @Test
    public void testIsLeaf() throws Exception {
        assertFalse(sampleTree.isLeaf());
        assertFalse(sampleTree.child('d').isLeaf());
        assertTrue(sampleTree.child('g').isLeaf());
        assertTrue(sampleTree.child('i').child('h').isLeaf());
    }

    @Test
    public void testEquals() throws Exception {
        final NaryTree<Character> tree1 = new NaryTree<Character>('a');
        tree1.addChildren(new Character[] { 'b', 'c' });

        final NaryTree<Character> tree2 = new NaryTree<Character>('a');
        tree2.addChildren(new Character[] { 'b', 'c' });

        final NaryTree<Character> tree3 = new NaryTree<Character>('a');
        tree3.addChildren(new Character[] { 'b', 'd' });

        assertTrue(tree1.equals(tree2));
        assertFalse(tree1.equals(tree3));

        tree2.child('c').addChild('d');
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
        final NaryTree<Character> t = (NaryTree<Character>) ois.readObject();
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
        final NaryTree<Character> t1 = new NaryTree<Character>('a');
        NaryTree<Character> tmp = t1.addChild('a');
        tmp.addChild('e');
        tmp.addChild('b');
        t1.addChild('b');
        t1.addChild('c');

        final NaryTree<Character> t2 = new NaryTree<Character>('a');
        tmp = t2.addChild('a');
        tmp.addChild('e');
        tmp.addChild('b');
        t2.addChild('b');
        t2.addChild('x');

        assertEquals(.31, t1.pqgramDistance(t2, 2, 3), .01f);

        // Example taken from Augsten, Bohlen, Gamper, 2005, page 308
        final NaryTree<Character> t = new NaryTree<Character>('a');
        t.addChild('b');
        tmp = t.addChild('c');
        tmp.addChild('d');
        tmp.addChild('e');
        tmp.addChild('f');
        tmp.addChild('g');
        tmp = tmp.child('e');
        tmp.addChild('h');
        tmp.addChild('i');
        tmp.addChild('j');

        final NaryTree<Character> tPrime = new NaryTree<Character>('a');
        tPrime.addChild('b');
        tmp = tPrime.addChild('c');
        tmp.addChild('d');
        tmp.addChild('e');
        tmp.addChild('f');
        tmp = tmp.child('e');
        tmp.addChild('h');
        tmp.addChild('i');

        final NaryTree<Character> tDoublePrime = new NaryTree<Character>('a');
        tDoublePrime.addChild('b');
        tDoublePrime.addChild('d');
        tDoublePrime.addChild('h');
        tDoublePrime.addChild('i');
        tDoublePrime.addChild('j');
        tDoublePrime.addChild('f');
        tDoublePrime.addChild('g');

        assertEquals(.3, t.pqgramDistance(tPrime, 2, 3), .01f);
        assertEquals(.89, t.pqgramDistance(tDoublePrime, 2, 3), .01f);
    }
}
