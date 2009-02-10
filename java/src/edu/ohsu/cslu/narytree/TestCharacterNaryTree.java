package edu.ohsu.cslu.narytree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Unit tests for {@link CharacterNaryTree}
 * 
 * @author Aaron Dunlop
 * @since Sep 24, 2008
 * 
 *        $Id$
 */
public class TestCharacterNaryTree
{
    private CharacterNaryTree sampleTree;
    private String stringSampleTree;

    private final static char[] SAMPLE_IN_ORDER_ARRAY = new char[] {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                                                                    'k'};
    private final static char[] SAMPLE_PRE_ORDER_ARRAY = new char[] {'f', 'd', 'b', 'a', 'c', 'e', 'g', 'i', 'h', 'k',
                                                                     'j'};
    private final static char[] SAMPLE_POST_ORDER_ARRAY = new char[] {'a', 'c', 'b', 'e', 'd', 'g', 'h', 'j', 'k', 'i',
                                                                      'f'};

    @Before
    public void setUp()
    {
        sampleTree = new CharacterNaryTree('f');

        CharacterNaryTree tmp1 = new CharacterNaryTree('b');
        tmp1.addChild('a');
        tmp1.addChild('c');

        CharacterNaryTree tmp2 = new CharacterNaryTree('d');
        tmp2.addSubtree(tmp1);
        tmp2.addChild('e');

        sampleTree.addSubtree(tmp2);
        sampleTree.addChild('g');

        tmp1 = new CharacterNaryTree('i');
        tmp1.addChild('h');
        tmp2 = tmp1.addChild('k');
        tmp2.addChild('j');
        sampleTree.addSubtree(tmp1);

        stringSampleTree = "(f (d (b a c) e) g (i h (k j)))";
    }

    @Test
    public void testAddChild() throws Exception
    {
        CharacterNaryTree tree = new CharacterNaryTree('a');
        assertEquals(1, tree.size());
        tree.addChild('b');
        assertEquals(2, tree.size());
        assertNull(tree.subtree('a'));
        assertNotNull(tree.subtree('b'));
    }

    @Test
    public void testAddChildren() throws Exception
    {
        CharacterNaryTree tree = new CharacterNaryTree('a');
        assertEquals(1, tree.size());
        tree.addChildren(new int[] {'b', 'c'});
        assertEquals(3, tree.size());
        assertNull(tree.subtree('a'));
        assertNotNull(tree.subtree('b'));
        assertNotNull(tree.subtree('c'));
    }

    @Test
    public void testAddSubtree() throws Exception
    {
        CharacterNaryTree tree = new CharacterNaryTree('a');
        CharacterNaryTree tmp = new CharacterNaryTree('b');
        tmp.addChildren(new char[] {'c', 'd'});
        tree.addSubtree(tmp);
        assertEquals(4, tree.size());
        assertNotNull(tree.subtree('b'));
        assertNotNull(tree.subtree('b').subtree('c'));
        assertNotNull(tree.subtree('b').subtree('d'));
    }

    @Test
    public void testRemoveChild() throws Exception
    {
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
    public void testRemoveChildrenByCharArray() throws Exception
    {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.subtree('h'));
        assertNull(sampleTree.subtree('j'));

        // Removing the 'i' node should move its children up ('g' has no children)
        sampleTree.removeChildren(new char[] {'g', 'i'});
        assertEquals(9, sampleTree.size());

        assertNotNull(sampleTree.subtree('h'));
        assertNotNull(sampleTree.subtree('k'));
    }

    @Test
    public void testRemoveChildrenByCharacterArray() throws Exception
    {
        assertEquals(11, sampleTree.size());

        assertNull(sampleTree.subtree('h'));
        assertNull(sampleTree.subtree('j'));

        // Removing the 'i' node should move its children up ('g' has no children)
        sampleTree.removeChildren(new Character[] {new Character('g'), new Character('i')});
        assertEquals(9, sampleTree.size());

        assertNotNull(sampleTree.subtree('h'));
        assertNotNull(sampleTree.subtree('k'));
    }

    @Test
    public void testRemoveSubtree() throws Exception
    {
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
    public void testSubtree() throws Exception
    {
        BaseNaryTree<Character> subtree = sampleTree.subtree('d');
        assertEquals(5, subtree.size());
        assertNotNull(subtree.subtree('b'));
        assertNotNull(subtree.subtree('e'));
        assertEquals(3, subtree.subtree('b').size());

        assertEquals(4, sampleTree.subtree('i').size());
    }

    @Test
    public void testSize() throws Exception
    {
        assertEquals(11, sampleTree.size());
        sampleTree.subtree('i').removeSubtree('k');
        assertEquals(9, sampleTree.size());
        sampleTree.subtree('g').addChild(1000);
        assertEquals(10, sampleTree.size());
    }

    @Test
    public void testDepthFromRoot() throws Exception
    {
        assertEquals(0, sampleTree.depthFromRoot());
        assertEquals(1, sampleTree.subtree('d').depthFromRoot());
        assertEquals(2, sampleTree.subtree('i').subtree('k').depthFromRoot());
        assertEquals(3, sampleTree.subtree('i').subtree('k').subtree('j').depthFromRoot());
    }

    @Test
    public void testLeaves() throws Exception
    {
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
    public void testInOrderIterator() throws Exception
    {
        Iterator<NaryTree<Character>> iter = sampleTree.inOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            BaseNaryTree<Character> tree = (BaseNaryTree<Character>) iter.next();
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i], (char) tree.label);
        }
    }

    @Test
    public void testInOrderLabelIterator() throws Exception
    {
        Iterator<Character> iter = sampleTree.inOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            assertEquals(SAMPLE_IN_ORDER_ARRAY[i], iter.next().charValue());
        }
    }

    @Test
    public void testPreOrderIterator() throws Exception
    {
        Iterator<NaryTree<Character>> iter = sampleTree.preOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            BaseNaryTree<Character> tree = (BaseNaryTree<Character>) iter.next();
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i], (char) tree.label);
        }
    }

    @Test
    public void testPreOrderLabelIterator() throws Exception
    {
        Iterator<Character> iter = sampleTree.preOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            assertEquals(SAMPLE_PRE_ORDER_ARRAY[i], iter.next().charValue());
        }
    }

    @Test
    public void testPostOrderIterator() throws Exception
    {
        Iterator<NaryTree<Character>> iter = sampleTree.postOrderIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            BaseNaryTree<Character> tree = (BaseNaryTree<Character>) iter.next();
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i], (char) tree.label);
        }
    }

    @Test
    public void testPostOrderLabelIterator() throws Exception
    {
        Iterator<Character> iter = sampleTree.postOrderLabelIterator();
        for (int i = 0; i < sampleTree.size(); i++)
        {
            assertEquals(SAMPLE_POST_ORDER_ARRAY[i], iter.next().charValue());
        }
    }

    @Test
    public void testReadFromReader() throws Exception
    {

        String stringSimpleTree = "(a (b c) d)";
        CharacterNaryTree simpleTree = CharacterNaryTree.read(new StringReader(stringSimpleTree));
        assertEquals(4, simpleTree.size());
        assertEquals(2, simpleTree.subtree('b').size());

        assertEquals('a', (char) simpleTree.intLabel());
        assertEquals('b', (char) simpleTree.subtree('b').intLabel());
        assertEquals('c', (char) simpleTree.subtree('b').subtree('c').intLabel());
        assertEquals('d', (char) simpleTree.subtree('d').intLabel());

        String stringTestTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        CharacterNaryTree testTree = CharacterNaryTree.read(new StringReader(stringTestTree));
        assertEquals(12, testTree.size());
        assertEquals(8, testTree.subtree('b').subtree('c').size());

        assertEquals('a', testTree.intLabel());
        assertEquals('b', (char) testTree.subtree('b').intLabel());
        assertEquals('c', (char) testTree.subtree('b').subtree('c').intLabel());
        assertEquals('d', (char) testTree.subtree('b').subtree('c').subtree('d').intLabel());
        assertEquals('e', (char) testTree.subtree('b').subtree('c').subtree('d').subtree('e').intLabel());
        assertEquals('f', (char) testTree.subtree('b').subtree('c').subtree('d').subtree('e').subtree('f').intLabel());
        assertEquals('g', (char) testTree.subtree('b').subtree('c').subtree('d').subtree('g').intLabel());
        assertEquals('h', (char) testTree.subtree('b').subtree('c').subtree('d').subtree('g').subtree('h').intLabel());
        assertEquals('i', (char) testTree.subtree('b').subtree('c').subtree('i').intLabel());
        assertEquals('j', (char) testTree.subtree('b').subtree('c').subtree('i').subtree('j').intLabel());
        assertEquals('k', (char) testTree.subtree('b').subtree('k').intLabel());
        assertEquals('l', (char) testTree.subtree('b').subtree('k').subtree('l').intLabel());

        CharacterNaryTree tree = CharacterNaryTree.read(new StringReader(stringSampleTree));
        assertEquals(sampleTree, tree);
    }

    @Test
    public void testWriteToWriter() throws Exception
    {
        StringWriter writer = new StringWriter();
        sampleTree.write(writer);
        assertEquals(stringSampleTree, writer.toString());

        String stringSimpleTree = "(a (b (c (d (e f) (g h)) (i j)) (k l)))";
        CharacterNaryTree tree = new CharacterNaryTree('a');
        tree.addChild('b').addChild('c').addChild('d').addChild('e').addChild('f');
        tree.subtree('b').subtree('c').subtree('d').addChild('g').addChild('h');
        tree.subtree('b').subtree('c').addChild('i').addChild('j');
        tree.subtree('b').addChild('k').addChild('l');

        writer = new StringWriter();
        tree.write(writer);
        assertEquals(stringSimpleTree, writer.toString());
    }

    @Test
    public void testIsLeaf() throws Exception
    {
        assertFalse(sampleTree.isLeaf());
        assertFalse(sampleTree.subtree('d').isLeaf());
        assertTrue(sampleTree.subtree('g').isLeaf());
        assertTrue(sampleTree.subtree('i').subtree('h').isLeaf());
    }

    @Test
    public void testEquals() throws Exception
    {
        BaseNaryTree<Character> tree1 = new CharacterNaryTree('a');
        tree1.addChildren(new int[] {'b', 'c'});

        BaseNaryTree<Character> tree2 = new CharacterNaryTree('a');
        tree2.addChildren(new int[] {'b', 'c'});

        BaseNaryTree<Character> tree3 = new CharacterNaryTree('a');
        tree3.addChildren(new int[] {'b', 'd'});

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
    public void testSerialize() throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);

        oos.writeObject(sampleTree);

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        BaseNaryTree<Character> t = (BaseNaryTree<Character>) ois.readObject();
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
        CharacterNaryTree t1 = new CharacterNaryTree('a');
        CharacterNaryTree tmp = t1.addChild('a');
        tmp.addChild('e');
        tmp.addChild('b');
        t1.addChild('b');
        t1.addChild('c');

        CharacterNaryTree t2 = new CharacterNaryTree('a');
        tmp = t2.addChild('a');
        tmp.addChild('e');
        tmp.addChild('b');
        t2.addChild('b');
        t2.addChild('x');

        assertEquals(.31, t1.pqgramDistance(t2, 2, 3), .01f);

        // Example taken from Augsten, Bohlen, Gamper, 2005, page 308
        CharacterNaryTree t = new CharacterNaryTree('a');
        t.addChild('b');
        tmp = t.addChild('c');
        tmp.addChild('d');
        tmp.addChild('e');
        tmp.addChild('f');
        tmp.addChild('g');
        tmp = (CharacterNaryTree) tmp.subtree('e');
        tmp.addChild('h');
        tmp.addChild('i');
        tmp.addChild('j');

        CharacterNaryTree tPrime = new CharacterNaryTree('a');
        tPrime.addChild('b');
        tmp = tPrime.addChild('c');
        tmp.addChild('d');
        tmp.addChild('e');
        tmp.addChild('f');
        tmp = (CharacterNaryTree) tmp.subtree('e');
        tmp.addChild('h');
        tmp.addChild('i');

        CharacterNaryTree tDoublePrime = new CharacterNaryTree('a');
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
