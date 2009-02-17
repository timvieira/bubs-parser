package edu.ohsu.cslu.narytree;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.common.Vocabulary;

/**
 * NaryTree implementation which stores integers as labels.
 * 
 * TODO: This implementation needs tuning for performance and memory footprint.
 * 
 * @author Aaron Dunlop
 * @since Sep 19, 2008
 * 
 *        $Id$
 */
public abstract class BaseNaryTree<E> implements Serializable, NaryTree<E>
{
    private final static long serialVersionUID = 369752896212698723L;

    /** Label of the root node */
    protected final int label;

    /** Parent node (if any) */
    protected BaseNaryTree<?> parent;

    /**
     * All the children of this tree
     * 
     * TODO: Store children as an array for fast and compact access?
     */
    protected final LinkedList<BaseNaryTree<E>> childList = new LinkedList<BaseNaryTree<E>>();

    /** Number of nodes in this tree, including this node and any subtrees */
    protected int size;

    /** Number of leaf nodes in this tree */
    protected int leaves;

    public BaseNaryTree(final int label, final BaseNaryTree<E> parent)
    {
        this.label = label;
        size = 1;
        leaves = 1;
        this.parent = parent;
    }

    public BaseNaryTree(final int label)
    {
        this(label, null);
    }

    /**
     * @return the label of the root node
     */
    public int intLabel()
    {
        return label;
    }

    /**
     * Adds a child to the tree
     * 
     * @param childLabel label of the child
     * @return The newly added subtree
     */
    public abstract BaseNaryTree<E> addChild(final String childLabel);

    /**
     * Adds a child to the tree
     * 
     * @param childLabel label of the child
     * @return The newly added subtree
     */
    @SuppressWarnings("unchecked")
    public BaseNaryTree<E> addChild(final int childLabel)
    {
        try
        {
            Class<?> c = getClass();
            BaseNaryTree<E> child = (BaseNaryTree<E>) c.getConstructor(new Class[] {int.class, BaseNaryTree.class})
                .newInstance(new Object[] {childLabel, this});
            addChild(child);
            return child;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected BaseNaryTree<E> addChild(final BaseNaryTree<E> child)
    {
        updateSize(1, isLeaf() ? 0 : 1);
        childList.add(child);
        return child;
    }

    public void addChildren(final int[] childLabels)
    {
        for (int child : childLabels)
        {
            addChild(child);
        }
    }

    @Override
    public void addChildren(final Collection<E> childLabels)
    {
        for (E child : childLabels)
        {
            addChild(child);
        }
    }

    @Override
    public void addChildren(final E[] childLabels)
    {
        for (E child : childLabels)
        {
            addChild(child);
        }
    }

    public void addSubtree(final BaseNaryTree<E> subtree)
    {
        final int leavesAdded = subtree.leaves - (isLeaf() ? 1 : 0);
        childList.add(subtree);
        subtree.parent = this;
        updateSize(subtree.size, leavesAdded);
    }

    /**
     * Used just for static parsing routines
     * 
     * @param subtree tree to add
     */
    @SuppressWarnings("unchecked")
    private void internalAddSubtree(final BaseNaryTree<?> subtree)
    {
        addSubtree((BaseNaryTree<E>) subtree);
    }

    public boolean removeChild(final int childLabel)
    {
        int i = 0;
        for (Iterator<BaseNaryTree<E>> iter = childList.iterator(); iter.hasNext();)
        {
            BaseNaryTree<E> child = iter.next();
            if (child.label == childLabel)
            {
                final int leavesRemoved = (childList.size() == 1 || !child.isLeaf()) ? 0 : 1;

                iter.remove();
                child.parent = null;
                childList.addAll(i, child.childList);
                for (BaseNaryTree<E> t : child.childList)
                {
                    t.parent = this;
                }
                updateSize(-1, -leavesRemoved);
                return true;
            }
            i++;
        }
        return false;
    }

    @Override
    public void removeChildren(final Collection<E> childLabels)
    {
        for (E childLabel : childLabels)
        {
            removeChild(childLabel);
        }
    }

    @Override
    public void removeChildren(final E[] childLabels)
    {
        for (E childLabel : childLabels)
        {
            removeChild(childLabel);
        }
    }

    public void removeChildren(final int[] childLabels)
    {
        for (int childLabel : childLabels)
        {
            removeChild(childLabel);
        }
    }

    public boolean removeSubtree(final int childLabel)
    {
        for (Iterator<BaseNaryTree<E>> i = childList.iterator(); i.hasNext();)
        {
            BaseNaryTree<E> child = i.next();
            if (child.label == childLabel)
            {
                final int leavesRemoved = (childList.size() == 1) ? 0 : child.leaves;
                i.remove();
                updateSize(-child.size, -leavesRemoved);
                return true;
            }
        }
        return false;
    }

    public int[] childArray()
    {
        int[] childArray = new int[childList.size()];
        int i = 0;
        for (BaseNaryTree<E> child : childList)
        {
            childArray[i++] = child.label;
        }
        return childArray;
    }

    public List<NaryTree<E>> children()
    {
        return new LinkedList<NaryTree<E>>(childList);
    }

    /**
     * It's not technically possible to do an 'in-order' traversal of an n-ary tree, since there's
     * no guaranteed position within the children in which the parent belongs. But some tree
     * applications depend on branching in which the parent branches into a left branch and one or
     * more right branches. This iterator follows that assumption and emits the parent node after
     * the first child (if any) and before any other children.
     * 
     * @return 'in-order' {@link Iterator}
     */
    public Iterator<Integer> inOrderIntegerIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderLabelList(this, new ArrayList<Integer>(size)).iterator();
    }

    public Iterator<NaryTree<E>> inOrderIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderList(this, new ArrayList<NaryTree<E>>(size)).iterator();
    }

    private List<NaryTree<E>> inOrderList(BaseNaryTree<E> tree, List<NaryTree<E>> list)
    {
        Iterator<BaseNaryTree<E>> i = tree.childList.iterator();
        if (i.hasNext())
        {
            inOrderList(i.next(), list);
        }

        list.add(tree);

        while (i.hasNext())
        {
            inOrderList(i.next(), list);
        }

        return list;
    }

    private List<Integer> inOrderLabelList(BaseNaryTree<E> tree, List<Integer> list)
    {
        Iterator<BaseNaryTree<E>> i = tree.childList.iterator();
        if (i.hasNext())
        {
            inOrderLabelList(i.next(), list);
        }

        list.add(new Integer(tree.label));

        while (i.hasNext())
        {
            inOrderLabelList(i.next(), list);
        }

        return list;
    }

    public int[] inOrderArray()
    {
        return toIntArray(inOrderIntegerIterator());
    }

    public Iterator<Integer> preOrderIntegerIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderLabelList(this, new ArrayList<Integer>(size)).iterator();
    }

    public Iterator<NaryTree<E>> preOrderIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderList(this, new ArrayList<NaryTree<E>>(size)).iterator();
    }

    private List<NaryTree<E>> preOrderList(final BaseNaryTree<E> tree, final List<NaryTree<E>> list)
    {
        list.add(tree);
        for (BaseNaryTree<E> child : tree.childList)
        {
            preOrderList(child, list);
        }

        return list;
    }

    private List<Integer> preOrderLabelList(final BaseNaryTree<E> tree, final List<Integer> list)
    {
        list.add(new Integer(tree.label));
        for (BaseNaryTree<E> child : tree.childList)
        {
            preOrderLabelList(child, list);
        }

        return list;
    }

    public int[] preOrderArray()
    {
        return toIntArray(preOrderIntegerIterator());
    }

    public Iterator<Integer> postOrderIntegerIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderLabelList(this, new ArrayList<Integer>(size)).iterator();
    }

    public Iterator<NaryTree<E>> postOrderIterator()
    {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderList(this, new ArrayList<NaryTree<E>>(size)).iterator();
    }

    private List<NaryTree<E>> postOrderList(final BaseNaryTree<E> tree, final List<NaryTree<E>> list)
    {
        for (BaseNaryTree<E> child : tree.childList)
        {
            postOrderList(child, list);
        }
        list.add(tree);

        return list;
    }

    private List<Integer> postOrderLabelList(final BaseNaryTree<E> tree, final List<Integer> list)
    {
        for (BaseNaryTree<E> child : tree.childList)
        {
            postOrderLabelList(child, list);
        }
        list.add(new Integer(tree.label));

        return list;
    }

    public int[] postOrderArray()
    {
        return toIntArray(postOrderIntegerIterator());
    }

    private int[] toIntArray(final Iterator<Integer> iter)
    {
        int[] array = new int[size];
        for (int i = 0; i < size; i++)
        {
            array[i] = iter.next().intValue();
        }
        return array;
    }

    public boolean isLeaf()
    {
        return size == 1;
    }

    public int size()
    {
        return size;
    }

    public int depthFromRoot()
    {
        int depth = 0;

        for (BaseNaryTree<?> tree = this; tree.parent != null; tree = tree.parent)
        {
            depth++;
        }
        return depth;
    }

    public int leaves()
    {
        return leaves;
    }

    @SuppressWarnings("unchecked")
    public BaseNaryTree<E> parent()
    {
        return (BaseNaryTree<E>) parent;
    }

    public BaseNaryTree<E> subtree(final int childLabel)
    {
        for (BaseNaryTree<E> child : childList)
        {
            if (child.label == childLabel)
            {
                return child;
            }
        }
        return null;
    }

    protected void updateSize(final int childrenAdded, final int leavesAdded)
    {
        size += childrenAdded;
        leaves += leavesAdded;
        if (parent != null)
        {
            parent.updateSize(childrenAdded, leavesAdded);
        }
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static BaseNaryTree<?> read(final InputStream inputStream, final Class<? extends BaseNaryTree<?>> treeClass,
        Vocabulary vocabulary) throws IOException
    {
        return read(new InputStreamReader(inputStream), treeClass, vocabulary);
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @return the tree
     */
    public static BaseNaryTree<?> read(final String string, final Class<? extends BaseNaryTree<?>> treeClass,
        Vocabulary vocabulary)
    {
        try
        {
            return read(new StringReader(string), treeClass, vocabulary);
        }
        catch (IOException e)
        {
            // A StringReader shouldn't ever throw an IOException
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param reader The reader to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static BaseNaryTree<?> read(final Reader reader, final Class<? extends BaseNaryTree<?>> treeClass,
        Vocabulary vocabulary) throws IOException
    {
        char c;

        // Discard any spaces or end-of-line characters
        while ((c = (char) reader.read()) == '\n' || c == '\r' || c == ' ')
        {}

        // We expect the first character to be '('
        if (c != '(')
        {
            throw new IllegalArgumentException("Bad tree format. Expected '(' but found '" + c + "'");
        }

        Constructor<? extends BaseNaryTree<?>> treeConstructor = null;

        try
        {
            treeConstructor = treeClass.getConstructor(new Class[] {String.class, Vocabulary.class});
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return readSubtree(reader, null, treeConstructor, vocabulary);
    }

    /**
     * Recursively reads a subtree from a Reader
     * 
     * @param reader source
     * @param parent parent tree (if any)
     * @return subtree
     * @throws IOException if the read fails
     */
    private static BaseNaryTree<?> readSubtree(final Reader reader, final BaseNaryTree<?> parent,
        final Constructor<? extends BaseNaryTree<?>> treeConstructor, final Vocabulary labelMap) throws IOException
    {
        try
        {
            // Recursively read a tree from the reader.
            // TODO: This could probably be simplified and perhaps optimized
            StringBuilder rootToken = new StringBuilder();

            // Read the root token
            for (char c = (char) reader.read(); c != ' ' && c != ')'; c = (char) reader.read())
            {
                rootToken.append(c);
            }
            BaseNaryTree<?> tree = treeConstructor.newInstance(new Object[] {rootToken.toString(), labelMap});

            StringBuilder childToken = new StringBuilder();

            // Read tokens and add children until we come to a ')'
            for (char c = (char) reader.read(); c != ')'; c = (char) reader.read())
            {
                // Parse any subtrees we find
                if (c == '(')
                {
                    tree.internalAddSubtree(readSubtree(reader, tree, treeConstructor, labelMap));
                }
                // Add any tokens we find
                else if (c == ' ')
                {
                    if (childToken.length() > 0)
                    {
                        tree.addChild(childToken.toString());
                        childToken = new StringBuilder();
                    }
                }
                else
                {
                    childToken.append(c);
                }
            }

            if (childToken.length() > 0)
            {
                tree.addChild(childToken.toString());
            }

            tree.parent = parent;
            return tree;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the tree to a standard parenthesis-bracketed representation
     * 
     * @param outputStream The {@link OutputStream} to write to
     * @throws IOException if the write fails
     */
    public void write(final OutputStream outputStream) throws IOException
    {
        write(new OutputStreamWriter(outputStream));
    }

    /**
     * Writes the tree to a standard parenthesis-bracketed representation
     * 
     * @param writer The {@link Writer} to write to
     * @throws IOException if the write fails
     */
    public void write(final Writer writer) throws IOException
    {
        writeSubtree(writer, this);
    }

    protected void writeSubtree(final Writer writer, final BaseNaryTree<E> tree) throws IOException
    {
        if (tree.size > 1)
        {
            writer.write('(');
            writer.write(tree.stringLabel());
            for (Iterator<BaseNaryTree<E>> i = tree.childList.iterator(); i.hasNext();)
            {
                writer.write(' ');
                writeSubtree(writer, i.next());
            }
            writer.write(')');
        }
        else
        {
            writer.write(tree.stringLabel());
        }
    }

    /**
     * Calculates the pq-gram tree similarity metric between two trees. See Augsten, Bohlen, Gamper,
     * 2005.
     * 
     * @param p parameter
     * @param q parameter
     * @return tree similarity
     */
    public float pqgramDistance(final BaseNaryTree<E> other, final int p, final int q)
    {
        return PqgramProfile.pqgramDistance(pqgramProfile(p, q), other.pqgramProfile(p, q));
    }

    /**
     * Calculates the pq-gram tree similarity metric between two trees. See Augsten, Bohlen, Gamper,
     * 2005.
     * 
     * @param profile The bag profile of the other tree
     * @param p parameter
     * @param q parameter
     * @return tree similarity
     */
    public float pqgramDistance(final PqgramProfile profile, final int p, final int q)
    {
        return PqgramProfile.pqgramDistance(profile, pqgramProfile(p, q));
    }

    /**
     * Implements pq-gram profile as per Augsten, Bohlen, Gamper 2005, page 306 (used to calculate
     * pq-gram distance)
     * 
     * @param p parameter
     * @param q parameter
     * @return profile
     */
    public PqgramProfile pqgramProfile(final int p, final int q)
    {
        PqgramProfile profile = new PqgramProfile();
        pqgramProfile(p, q, profile, this, new IntShiftRegister(p));
        return profile;
    }

    /**
     * Implements pq-gram profile as per Augsten, Bohlen, Gamper 2005, page 306 (used to calculate
     * pq-gram distance)
     * 
     * @param p parameter
     * @param q parameter
     * @param profile Current profile
     * @param r Current tree
     * @param anc Current shift register
     */
    private void pqgramProfile(final int p, final int q, final PqgramProfile profile, final BaseNaryTree<E> r,
        IntShiftRegister anc)
    {
        anc = anc.shift(r.label);
        IntShiftRegister sib = new IntShiftRegister(q);

        if (r.isLeaf())
        {
            profile.add(anc.concat(sib));
        }
        else
        {
            for (BaseNaryTree<E> c : r.childList)
            {
                sib = sib.shift(c.label);
                profile.add(anc.concat(sib));
                pqgramProfile(p, q, profile, c, anc);
            }

            for (int k = 1; k < q; k++)
            {
                sib = sib.shift();
                profile.add(anc.concat(sib));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object o)
    {
        if (!(o instanceof BaseNaryTree))
        {
            return false;
        }

        BaseNaryTree<E> other = (BaseNaryTree<E>) o;

        if ((other.label != label) || (other.childList.size() != childList.size()))
        {
            return false;
        }

        final Iterator<BaseNaryTree<E>> i1 = childList.iterator();
        final Iterator<BaseNaryTree<E>> i2 = other.childList.iterator();
        while (i1.hasNext())
        {
            if (!(i1.next().equals(i2.next())))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString()
    {
        try
        {
            Writer writer = new StringWriter();
            write(writer);
            return writer.toString();
        }
        catch (IOException e)
        {
            return "Exception in toString(): " + e.getMessage();
        }
    }

    protected abstract static class BaseLabelIterator
    {
        protected Iterator<Integer> intIterator;

        public BaseLabelIterator(Iterator<Integer> intIterator)
        {
            this.intIterator = intIterator;
        }

        public boolean hasNext()
        {
            return intIterator.hasNext();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class PqgramProfile implements Cloneable
    {
        private final Object2IntOpenHashMap<ShiftRegister> map;
        private int size;

        public PqgramProfile()
        {
            map = new Object2IntOpenHashMap<ShiftRegister>();
        }

        private PqgramProfile(Object2IntOpenHashMap<ShiftRegister> map)
        {
            this.map = map;
        }

        public void add(ShiftRegister register)
        {
            size++;
            map.put(register, map.getInt(register) + 1);
        }

        public void addAll(Collection<? extends ShiftRegister> c)
        {
            for (ShiftRegister register : c)
            {
                add(register);
            }
        }

        public void clear()
        {
            map.clear();
            size = 0;
        }

        public boolean contains(ShiftRegister register)
        {
            return map.containsKey(register);
        }

        public boolean isEmpty()
        {
            return map.isEmpty();
        }

        public PqgramProfile intersection(PqgramProfile o)
        {
            PqgramProfile intersection = new PqgramProfile();

            for (ShiftRegister r : map.keySet())
            {
                final int count = Math.min(map.getInt(r), o.map.getInt(r));

                if (count > 0)
                {
                    intersection.map.put(r, count);
                    intersection.size += count;
                }
            }

            return intersection;
        }

        public int intersectionSize(PqgramProfile o)
        {
            int intersectionSize = 0;

            for (ShiftRegister r : map.keySet())
            {
                intersectionSize += Math.min(map.getInt(r), o.map.getInt(r));
            }

            return intersectionSize;
        }

        public int size()
        {
            return size;
        }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof PqgramProfile))
            {
                return false;
            }

            PqgramProfile bag = (PqgramProfile) o;

            for (ShiftRegister r : map.keySet())
            {
                if (map.getInt(r) != bag.map.getInt(r))
                {
                    return false;
                }
            }

            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public PqgramProfile clone()
        {
            return new PqgramProfile((Object2IntOpenHashMap<ShiftRegister>) map.clone());
        }

        @Override
        public String toString()
        {
            return map.toString();
        }

        /**
         * Calculates the pq-gram tree similarity metric between two trees. See Augsten, Bohlen,
         * Gamper, 2005.
         * 
         * @param profile1 Bag profile of a tree
         * @param profile2 Bag profile of a tree
         * @return tree similarity
         */
        public final static float pqgramDistance(final PqgramProfile profile1, final PqgramProfile profile2)
        {
            final int bagUnionCardinality = profile1.size() + profile2.size();
            final int bagIntersectionCardinality = profile1.intersectionSize(profile2);

            return 1f - 2f * bagIntersectionCardinality / bagUnionCardinality;
        }
    }
}
