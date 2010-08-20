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

import edu.ohsu.cslu.tests.FilteredRunner;

import static junit.framework.Assert.*;

/**
 * Unit tests for {@link BaseNaryTree} with characters.
 * 
 * @author Aaron Dunlop
 * @since Sep 24, 2008
 * 
 *        $Id$
 */
@RunWith(FilteredRunner.class)
public class TestCharacterNaryTree {

    private BaseNaryTree<Character> sampleTree;
    private String stringSampleTree;

    private final static char[] SAMPLE_IN_ORDER_ARRAY = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k' };
    private final static char[] SAMPLE_PRE_ORDER_ARRAY = new char[] { 'f', 'd', 'b', 'a', 'c', 'e', 'g', 'i',
            'h', 'k', 'j' };
    private final static char[] SAMPLE_POST_ORDER_ARRAY = new char[] { 'a', 'c', 'b', 'e', 'd', 'g', 'h',
            'j', 'k', 'i', 'f' };

    private final static BaseNaryTree.LabelParser<Character> labelParser = new BaseNaryTree.LabelParser<Character>() {
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
        sampleTree = new BaseNaryTree<Character>('f');

        BaseNaryTree<Character> tmp1 = new BaseNaryTree<Character>('b');
        tmp1.addChild('a');
        tmp1.addChild('c');

        BaseNaryTree<Character> tmp2 = new BaseNaryTree<Character>('d');
        tmp2.addSubtree(tmp1);
        tmp2.addChild('e');

        sampleTree.addSubtree(tmp2);
        sampleTree.addChild('g');

        tmp1 = new BaseNaryTree<Character>('i');
        tmp1.addChild('h');
        tmp2 = tmp1.addChild('k');
        tmp2.addChild('j');
        sampleTree.addSubtree(tmp1);

        stringSampleTree = "(f (d (b a c) e) g (i h (k j)))";
    }

    @Test
    public void testAddChild() throws Exception {
        final BaseNaryTree<Character> tree = new BaseNaryTree<Character>('a');
        assertEquals(1, tree.size());
        tree.addChild('b');
        assertEquals(2, tree.size());
        assertNull(tree.subtree('a'));
        assertNotNull(tree.subtree('b'));
    }

    @Test
    public void testAddChildren() throws Exception {
        final BaseNaryTree<Character> tree = new BaseNaryTree<Character>('a');
        assertEquals(1, tree.size());
        tree.addChildren(new Character[] { 'b', 'c' });
        assertEquals(3, tree.size());
        assertNull(tree.subtree('a'));
        assertNotNull(tree.subtree('b'));
        assertNotNull(tree.subtree('c'));
    }

    @Test
    public void testAddSubtree() throws Exception {
        final BaseNaryTree<Character> tree = new BaseNaryTree<Character>('a');
        final BaseNaryTree<Character> tmp = new BaseNaryTree<Character>('b');
        tmp.addChildren(new Character[] { 'c', 'd' });
        tree.addSubtree(tmp);
        assertEquals(4, tree.size());
        assertNotNull(tree.subtree('b'));
        assertNotNull(tree.subtree('b').subtree('c'));
        assertNotNull(tree.subtree('b').subtree('d'));
    }

    @Test
    public void testRemoveChild() throws Exception {
        assertEquals(11, sampleTree.size());

        sampleTree.removeChild('g');
        assertEquals(10, sampleTree.size());

        // Removing the 'd' node should move its children up
        assertNull(sampleTree.subtree('b'));
        assertNull(sampleTree.subtree('e'));

        sampleTree.removeChild('d');
        assertEquals(9, sampleTree.size());
        assertNotNull(sampleTree.subtree('b'));
        assertNotNull(sampleTree.subtree('e'));

        // TODO: Validate that the children were inserted at the proper place with iteration order
    }

    @Test
    public void testRemoveChildrenByCharArray() throws Exception {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.subtree('h'));
        assertNull(sampleTree.subtree('j'));

        // Removing the 'i' node should move its children up ('g' has no children)
        sampleTree.removeChildren(new Character[] { 'g', 'i' });
        assertEquals(9, sampleTree.size());

        assertNotNull(sampleTree.subtree('h'));
        assertNotNull(sampleTree.subtree('k'));
    }

    @Test
    public void testRemoveChildrenByCharacterArray() throws Exception {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.subtree('h'));
        assertNull(sampleTree.subtree('j'));

        // Removing the 'i' node should move its children up ('g' has no children)
        sampleTree.removeChildren(new Character[] { new Character('g'), new Character('i') });
        assertEquals(9, sampleTree.size());

        assertNotNull(sampleTree.subtree('h'));
        assertNotNull(sampleTree.subtree('k'));
    }

    @Test
    public void testRemoveSubtree() throws Exception {
        assertEquals(11, sampleTree.size());

        sampleTree.removeSubtree('g');
        assertEquals(10, sampleTree.size());

        // Removing the '4' node should remove all its children as well
        sampleTree.removeSubtree('d');
        assertEquals(5, sampleTree.size());
        assertNull(sampleTree.subtree('b'));
        assertNull(sampleTree.subtree('e'));
    }

    @Test
    public void testSubtree() throws Exception {
        final BaseNaryTree<Character> subtree = sampleTree.subtree('d');
        assertEquals(5, subtree.size());
        assertNotNull(subtree.subtree('b'));
        assertNotNull(subtree.subtree('e'));
        assertEquals(3, subtree.subtree('b').size());

        assertEquals(4, sampleTree.subtree('i').size());
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(11, sampleTree.size());
        sampleTree.subtree('i').removeSubtree('k');
        assertEquals(9, sampleTree.size());
        sampleTree.subtree('g').addChild('l');
        assertEquals(10, sampleTree.size());
    }

    @Test
    public void testDepthFromRoot() throws Exception {
        assertEquals(0, sampleTree.depthFromRoot());
        assertEquals(1, sampleTree.subtree('d').depthFromRoot());
        assertEquals(2, sampleTree.subtree('i').subtree('k').depthFromRoot());
        assertEquals(3, sampleTree.subtree('i').subtree('k').subtree('j').depthFromRoot());
    }

    @Test
    public void testLeaves() throws Exception {
        assertEquals(6, sampleTree.leaves());
        assertEquals(3, sampleTree.subtree('d').leaves());

        sampleTree.subtree('g').addChild('y');
        assertEquals(6, sampleTree.leaves());

        sampleTree.subtree('g').removeChild('z');
        assertEquals(6, sampleTree.leaves());

        sampleTree.subtree('i').removeSubtree('k');
        assertEquals(5, sampleTree.leaves());

        sampleTree.removeSubtree('d');
        assertEquals(2, sampleTree.leaves());
    }

    @Test
    public void testInOrderIterator() throws Exception {
        final Iterator<NaryTree<Character>> iter = sampleTree.inOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            final BaseNaryTree<Character> tree = (BaseNaryTree<Character>) iter.next();
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i], (char) tree.label);
        }
    }

    @Test
    public void testInOrderLabelIterator() throws Exception {
        final Iterator<Character> iter = sampleTree.inOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i], iter.next().charValue());
        }
    }

    @Test
    public void testPreOrderIterator() throws Exception {
        final Iterator<NaryTree<Character>> iter = sampleTree.preOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            final BaseNaryTree<Character> tree = (BaseNaryTree<Character>) iter.next();
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i], (char) tree.label);
        }
    }

    @Test
    public void testPreOrderLabelIterator() throws Exception {
        final Iterator<Character> iter = sampleTree.preOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i], iter.next().charValue());
        }
    }

    @Test
    public void testPostOrderIterator() throws Exception {
        final Iterator<NaryTree<Character>> iter = sampleTree.postOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            final BaseNaryTree<Character> tree = (BaseNaryTree<Character>) iter.next();
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i], (char) tree.label);
        }
    }

    @Test
    public void testPostOrderLabelIterator() throws Exception {
        final Iterator<Character> iter = sampleTree.postOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++) {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i], iter.next().charValue());
        }
    }

    @Test
    public void testReadFromReader() throws Exception {

        final String stringSimpleTree = "(a (b c) d)";
        final BaseNaryTree<Character> simpleTree = BaseNaryTree.read(new StringReader(stringSimpleTree),
            labelParser);
        assertEquals(4, simpleTree.size());
        assertEquals(2, simpleTree.subtree('b').size());

        assertEquals('a', simpleTree.label().charValue());
        assertEquals('b', simpleTree.subtree('b').label().charValue());
        assertEquals('c', simpleTree.subtree('b').subtree('c').label().charValue());
        assertEquals('d', simpleTree.subtree('d').label().charValue());

        final String stringTestTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final BaseNaryTree<Character> testTree = BaseNaryTree.read(new StringReader(stringTestTree),
            labelParser);
        assertEquals(12, testTree.size());
        assertEquals(8, testTree.subtree('b').subtree('c').size());

        assertEquals('a', testTree.label().charValue());
        assertEquals('b', testTree.subtree('b').label().charValue());
        assertEquals('c', testTree.subtree('b').subtree('c').label().charValue());
        assertEquals('d', testTree.subtree('b').subtree('c').subtree('d').label().charValue());
        assertEquals('e', testTree.subtree('b').subtree('c').subtree('d').subtree('e').label().charValue());
        assertEquals('f', testTree.subtree('b').subtree('c').subtree('d').subtree('e').subtree('f').label()
            .charValue());
        assertEquals('g', testTree.subtree('b').subtree('c').subtree('d').subtree('g').label().charValue());
        assertEquals('h', testTree.subtree('b').subtree('c').subtree('d').subtree('g').subtree('h').label()
            .charValue());
        assertEquals('i', testTree.subtree('b').subtree('c').subtree('i').label().charValue());
        assertEquals('j', testTree.subtree('b').subtree('c').subtree('i').subtree('j').label().charValue());
        assertEquals('k', testTree.subtree('b').subtree('k').label().charValue());
        assertEquals('l', testTree.subtree('b').subtree('k').subtree('l').label().charValue());

        final BaseNaryTree<Character> tree = BaseNaryTree.read(new StringReader(stringSampleTree),
            labelParser);
        assertEquals(sampleTree, tree);
    }

    @Test
    public void testWriteToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        sampleTree.write(writer);
        assertEquals(stringSampleTree, writer.toString());

        final String stringSimpleTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        final BaseNaryTree<Character> tree = new BaseNaryTree<Character>('a');
        tree.addChild('b').addChild('c').addChild('d').addChild('e').addChild('f');
        tree.subtree('b').subtree('c').subtree('d').addChild('g').addChild('h');
        tree.subtree('b').subtree('c').addChild('i').addChild('j');
        tree.subtree('b').addChild('k').addChild('l');

        writer = new StringWriter();
        tree.write(writer);
        assertEquals(stringSimpleTree, writer.toString());
    }

    @Test
    public void testIsLeaf() throws Exception {
        assertFalse(sampleTree.isLeaf());
        assertFalse(sampleTree.subtree('d').isLeaf());
        assertTrue(sampleTree.subtree('g').isLeaf());
        assertTrue(sampleTree.subtree('i').subtree('h').isLeaf());
    }

    @Test
    public void testEquals() throws Exception {
        final BaseNaryTree<Character> tree1 = new BaseNaryTree<Character>('a');
        tree1.addChildren(new Character[] { 'b', 'c' });

        final BaseNaryTree<Character> tree2 = new BaseNaryTree<Character>('a');
        tree2.addChildren(new Character[] { 'b', 'c' });

        final BaseNaryTree<Character> tree3 = new BaseNaryTree<Character>('a');
        tree3.addChildren(new Character[] { 'b', 'd' });

        assertTrue(tree1.equals(tree2));
        assertFalse(tree1.equals(tree3));

        tree2.subtree('c').addChild('d');
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
        final BaseNaryTree<Character> t1 = new BaseNaryTree<Character>('a');
        BaseNaryTree<Character> tmp = t1.addChild('a');
        tmp.addChild('e');
        tmp.addChild('b');
        t1.addChild('b');
        t1.addChild('c');

        final BaseNaryTree<Character> t2 = new BaseNaryTree<Character>('a');
        tmp = t2.addChild('a');
        tmp.addChild('e');
        tmp.addChild('b');
        t2.addChild('b');
        t2.addChild('x');

        assertEquals(.31, t1.pqgramDistance(t2, 2, 3), .01f);

        // Example taken from Augsten, Bohlen, Gamper, 2005, page 308
        final BaseNaryTree<Character> t = new BaseNaryTree<Character>('a');
        t.addChild('b');
        tmp = t.addChild('c');
        tmp.addChild('d');
        tmp.addChild('e');
        tmp.addChild('f');
        tmp.addChild('g');
        tmp = tmp.subtree('e');
        tmp.addChild('h');
        tmp.addChild('i');
        tmp.addChild('j');

        final BaseNaryTree<Character> tPrime = new BaseNaryTree<Character>('a');
        tPrime.addChild('b');
        tmp = tPrime.addChild('c');
        tmp.addChild('d');
        tmp.addChild('e');
        tmp.addChild('f');
        tmp = tmp.subtree('e');
        tmp.addChild('h');
        tmp.addChild('i');

        final BaseNaryTree<Character> tDoublePrime = new BaseNaryTree<Character>('a');
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
