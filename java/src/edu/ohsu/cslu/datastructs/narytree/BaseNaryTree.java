package edu.ohsu.cslu.datastructs.narytree;

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

/**
 * NaryTree implementation
 * 
 * @author Aaron Dunlop
 * @since Sep 19, 2008
 * 
 *        $Id$
 */
public class BaseNaryTree<E> implements Serializable, NaryTree<E> {

    private final static long serialVersionUID = 369752896212698723L;

    /** Label of the root node */
    protected final E label;

    /** Parent node (if any) */
    protected BaseNaryTree<E> parent;

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

    public BaseNaryTree(final E label, final BaseNaryTree<E> parent) {
        this.label = label;
        size = 1;
        leaves = 1;
        this.parent = parent;
    }

    public BaseNaryTree(final E label) {
        this(label, null);
    }

    /**
     * Adds a child to the tree
     * 
     * @param childLabel label of the child
     * @return The newly added subtree
     */
    public BaseNaryTree<E> addChild(final E childLabel) {
        return addChild(new BaseNaryTree<E>(childLabel));
    }

    @Override
    public E label() {
        return label;
    }

    @Override
    public String stringLabel() {
        return label.toString();
    }

    protected BaseNaryTree<E> addChild(final BaseNaryTree<E> child) {
        child.parent = this;
        updateSize(1, isLeaf() ? 0 : 1);
        childList.add(child);
        return child;
    }

    @Override
    public void addChildren(final Collection<E> childLabels) {
        for (final E child : childLabels) {
            addChild(child);
        }
    }

    @Override
    public void addChildren(final E[] childLabels) {
        for (final E child : childLabels) {
            addChild(child);
        }
    }

    public void addSubtree(final BaseNaryTree<E> subtree) {
        final int leavesAdded = subtree.leaves - (isLeaf() ? 1 : 0);
        childList.add(subtree);
        subtree.parent = this;
        updateSize(subtree.size, leavesAdded);
    }

    @Override
    public void addSubtree(final Tree<E> subtree) {
        addSubtree((BaseNaryTree<E>) subtree);
    }

    @Override
    public List<E> childLabels() {
        return inOrderLabelList(new LinkedList<E>());
    }

    /**
     * Used just for static parsing routines
     * 
     * @param subtree tree to add
     */
    @SuppressWarnings("unchecked")
    private void internalAddSubtree(final BaseNaryTree<?> subtree) {
        addSubtree((BaseNaryTree<E>) subtree);
    }

    public boolean removeChild(final E childLabel) {
        int i = 0;
        for (final Iterator<BaseNaryTree<E>> iter = childList.iterator(); iter.hasNext();) {
            final BaseNaryTree<E> child = iter.next();
            if (child.label.equals(childLabel)) {
                final int leavesRemoved = (childList.size() == 1 || !child.isLeaf()) ? 0 : 1;

                iter.remove();
                child.parent = null;
                childList.addAll(i, child.childList);
                for (final BaseNaryTree<E> t : child.childList) {
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
    public void removeChildren(final Collection<E> childLabels) {
        for (final E childLabel : childLabels) {
            removeChild(childLabel);
        }
    }

    @Override
    public void removeChildren(final E[] childLabels) {
        for (final E childLabel : childLabels) {
            removeChild(childLabel);
        }
    }

    public boolean removeSubtree(final E childLabel) {
        for (final Iterator<BaseNaryTree<E>> i = childList.iterator(); i.hasNext();) {
            final BaseNaryTree<E> child = i.next();
            if (child.label == childLabel) {
                final int leavesRemoved = (childList.size() == 1) ? 0 : child.leaves;
                i.remove();
                updateSize(-child.size, -leavesRemoved);
                return true;
            }
        }
        return false;
    }

    public List<NaryTree<E>> children() {
        return new LinkedList<NaryTree<E>>(childList);
    }

    /**
     * It's not technically possible to do an 'in-order' traversal of an n-ary tree, since there's no
     * guaranteed position within the children in which the parent belongs. But some tree applications depend
     * on branching in which the parent branches into a left branch and one or more right branches. This
     * iterator follows that assumption and emits the parent node after the first child (if any) and before
     * any other children.
     * 
     * @return 'in-order' {@link Iterator}
     */
    public Iterator<NaryTree<E>> inOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return inOrderList(this, new ArrayList<NaryTree<E>>(size)).iterator();
    }

    private List<NaryTree<E>> inOrderList(final BaseNaryTree<E> tree, final List<NaryTree<E>> list) {
        final Iterator<BaseNaryTree<E>> i = tree.childList.iterator();
        if (i.hasNext()) {
            inOrderList(i.next(), list);
        }

        list.add(tree);

        while (i.hasNext()) {
            inOrderList(i.next(), list);
        }

        return list;
    }

    private List<E> inOrderLabelList(final List<E> list) {
        final Iterator<BaseNaryTree<E>> i = childList.iterator();
        if (i.hasNext()) {
            i.next().inOrderLabelList(list);
        }

        list.add(label);

        while (i.hasNext()) {
            i.next().inOrderLabelList(list);
        }

        return list;
    }

    @Override
    public Iterator<E> inOrderLabelIterator() {
        return inOrderLabelList(new LinkedList<E>()).iterator();
    }

    public Iterator<NaryTree<E>> preOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return preOrderList(new ArrayList<NaryTree<E>>(size)).iterator();
    }

    private List<NaryTree<E>> preOrderList(final List<NaryTree<E>> list) {
        list.add(this);
        for (final BaseNaryTree<E> child : childList) {
            child.preOrderList(list);
        }

        return list;
    }

    private List<E> preOrderLabelList(final List<E> list) {
        list.add(label);
        for (final BaseNaryTree<E> child : childList) {
            child.preOrderLabelList(list);
        }

        return list;
    }

    @Override
    public Iterator<E> preOrderLabelIterator() {
        return preOrderLabelList(new ArrayList<E>(size)).iterator();
    }

    public Iterator<NaryTree<E>> postOrderIterator() {
        // A simple and stupid implementation, but we can tune for performance if needed
        return postOrderList(new ArrayList<NaryTree<E>>(size)).iterator();
    }

    private List<NaryTree<E>> postOrderList(final List<NaryTree<E>> list) {
        for (final BaseNaryTree<E> child : childList) {
            child.postOrderList(list);
        }
        list.add(this);

        return list;
    }

    private List<E> postOrderLabelList(final List<E> list) {
        for (final BaseNaryTree<E> child : childList) {
            child.postOrderLabelList(list);
        }
        list.add(label);

        return list;
    }

    @Override
    public Iterator<E> postOrderLabelIterator() {
        return postOrderLabelList(new LinkedList<E>()).iterator();
    }

    public boolean isLeaf() {
        return size == 1;
    }

    public int size() {
        return size;
    }

    public int depthFromRoot() {
        int depth = 0;

        for (BaseNaryTree<?> tree = this; tree.parent != null; tree = tree.parent) {
            depth++;
        }
        return depth;
    }

    public int leaves() {
        return leaves;
    }

    public BaseNaryTree<E> parent() {
        return parent;
    }

    public BaseNaryTree<E> subtree(final E childLabel) {
        for (final BaseNaryTree<E> child : childList) {
            if (child.label.equals(childLabel)) {
                return child;
            }
        }
        return null;
    }

    protected void updateSize(final int childrenAdded, final int leavesAdded) {
        size += childrenAdded;
        leaves += leavesAdded;
        if (parent != null) {
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
    public static <T> BaseNaryTree<T> read(final InputStream inputStream, final Class<T> type)
            throws IOException {
        return read(new InputStreamReader(inputStream), type);
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return the tree
     * @throws IOException if the read fails
     */
    public static <T> BaseNaryTree<T> read(final InputStream inputStream, final LabelParser<T> labelParser)
            throws IOException {
        return read(new InputStreamReader(inputStream), labelParser);
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @return the tree
     */
    public static <T> BaseNaryTree<T> read(final String string, final Class<T> type) {
        try {
            return read(new StringReader(string), type);
        } catch (final IOException e) {
            // A StringReader shouldn't ever throw an IOException
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads in an n-ary tree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return the tree
     */
    public static <T> BaseNaryTree<T> read(final String string, final LabelParser<T> labelParser) {
        try {
            return read(new StringReader(string), labelParser);
        } catch (final IOException e) {
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
    public static <T> BaseNaryTree<T> read(final Reader reader, final Class<T> type) throws IOException {

        try {
            final Constructor<T> labelConstructor = type.getConstructor(new Class[] { String.class });
            final LabelParser<T> labelParser = new LabelParser<T>() {
                @Override
                public T parse(final String label) throws Exception {
                    return labelConstructor.newInstance(new Object[] { label });
                }
            };
            return read(reader, labelParser);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Recursively reads a subtree from a Reader
     * 
     * @param reader source
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return the tree
     * @throws IOException if the read fails
     */
    public static <T> BaseNaryTree<T> read(final Reader reader, final LabelParser<T> labelParser)
            throws IOException {
        char c;

        // Discard any spaces or end-of-line characters
        while ((c = (char) reader.read()) == '\n' || c == '\r' || c == ' ') {
        }

        // We expect the first character to be '('
        if (c != '(') {
            throw new IllegalArgumentException("Bad tree format. Expected '(' but found '" + c + "'");
        }
        return readSubtree(reader, null, labelParser);
    }

    /**
     * Recursively reads a subtree from a Reader
     * 
     * @param reader source
     * @param parent parent tree (if any)
     * @param labelParser Parser appropriate for the labels contained in the tree
     * @return subtree
     * @throws IOException if the read fails
     */
    private static <T> BaseNaryTree<T> readSubtree(final Reader reader, final BaseNaryTree<T> parent,
            final LabelParser<T> labelParser) throws IOException {

        try {
            // Recursively read a tree from the reader.
            // TODO: This could probably be simplified and perhaps optimized
            final StringBuilder rootToken = new StringBuilder();

            // Read the root token
            for (char c = (char) reader.read(); c != ' ' && c != ')'; c = (char) reader.read()) {
                rootToken.append(c);
            }

            final BaseNaryTree<T> tree = new BaseNaryTree<T>(labelParser.parse(rootToken.toString()));

            StringBuilder childToken = new StringBuilder();

            // Read tokens and add children until we come to a ')'
            for (char c = (char) reader.read(); c != ')'; c = (char) reader.read()) {
                // Parse any subtrees we find
                if (c == '(') {
                    tree.internalAddSubtree(readSubtree(reader, tree, labelParser));
                }
                // Add any tokens we find
                else if (c == ' ') {
                    if (childToken.length() > 0) {
                        tree.addChild(new BaseNaryTree<T>(labelParser.parse(childToken.toString())));
                        childToken = new StringBuilder();
                    }
                } else {
                    childToken.append(c);
                }
            }

            if (childToken.length() > 0) {
                tree.addChild(new BaseNaryTree<T>(labelParser.parse(childToken.toString())));
            }

            tree.parent = parent;
            return tree;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes the tree to a standard parenthesis-bracketed representation
     * 
     * @param outputStream The {@link OutputStream} to write to
     * @throws IOException if the write fails
     */
    public void write(final OutputStream outputStream) throws IOException {
        write(new OutputStreamWriter(outputStream));
    }

    /**
     * Writes the tree to a standard parenthesis-bracketed representation
     * 
     * @param writer The {@link Writer} to write to
     * @throws IOException if the write fails
     */
    public void write(final Writer writer) throws IOException {
        writeSubtree(writer, this);
    }

    protected void writeSubtree(final Writer writer, final BaseNaryTree<E> tree) throws IOException {
        if (tree.size > 1) {
            writer.write('(');
            writer.write(tree.stringLabel());
            for (final Iterator<BaseNaryTree<E>> i = tree.childList.iterator(); i.hasNext();) {
                writer.write(' ');
                writeSubtree(writer, i.next());
            }
            writer.write(')');
        } else {
            writer.write(tree.stringLabel());
        }
    }

    /**
     * Calculates the pq-gram tree similarity metric between two trees. See Augsten, Bohlen, Gamper, 2005.
     * 
     * @param p parameter
     * @param q parameter
     * @return tree similarity
     */
    public float pqgramDistance(final BaseNaryTree<E> other, final int p, final int q) {
        return PqgramProfile.pqgramDistance(pqgramProfile(p, q), other.pqgramProfile(p, q));
    }

    /**
     * Implements pq-gram profile as per Augsten, Bohlen, Gamper 2005, page 306 (used to calculate pq-gram
     * distance)
     * 
     * @param p parameter
     * @param q parameter
     * @return profile
     */
    public PqgramProfile<E> pqgramProfile(final int p, final int q) {
        final PqgramProfile<E> profile = new PqgramProfile<E>();
        pqgramProfile(p, q, profile, this, new ShiftRegister<E>(p));
        return profile;
    }

    /**
     * Implements pq-gram profile as per Augsten, Bohlen, Gamper 2005, page 306 (used to calculate pq-gram
     * distance)
     * 
     * @param p parameter
     * @param q parameter
     * @param profile Current profile
     * @param r Current tree
     * @param anc Current shift register
     */
    private void pqgramProfile(final int p, final int q, final PqgramProfile<E> profile,
            final BaseNaryTree<E> r, ShiftRegister<E> anc) {
        anc = anc.shift(r.label);
        ShiftRegister<E> sib = new ShiftRegister<E>(q);

        if (r.isLeaf()) {
            profile.add(anc.concat(sib));
        } else {
            for (final BaseNaryTree<E> c : r.childList) {
                sib = sib.shift(c.label);
                profile.add(anc.concat(sib));
                pqgramProfile(p, q, profile, c, anc);
            }

            for (int k = 1; k < q; k++) {
                sib = sib.shift();
                profile.add(anc.concat(sib));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(final Object o) {
        if (!(o instanceof BaseNaryTree)) {
            return false;
        }

        final BaseNaryTree<E> other = (BaseNaryTree<E>) o;

        if (!other.label.equals(label) || (other.childList.size() != childList.size())) {
            return false;
        }

        final Iterator<BaseNaryTree<E>> i1 = childList.iterator();
        final Iterator<BaseNaryTree<E>> i2 = other.childList.iterator();
        while (i1.hasNext()) {
            if (!(i1.next().equals(i2.next()))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        try {
            final Writer writer = new StringWriter();
            write(writer);
            return writer.toString();
        } catch (final IOException e) {
            return "Exception in toString(): " + e.getMessage();
        }
    }

    protected abstract static class BaseLabelIterator {

        protected Iterator<Integer> intIterator;

        public BaseLabelIterator(final Iterator<Integer> intIterator) {
            this.intIterator = intIterator;
        }

        public boolean hasNext() {
            return intIterator.hasNext();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class PqgramProfile<E> implements Cloneable {

        private final Object2IntOpenHashMap<ShiftRegister<E>> map;
        private int size;

        public PqgramProfile() {
            map = new Object2IntOpenHashMap<ShiftRegister<E>>();
        }

        private PqgramProfile(final Object2IntOpenHashMap<ShiftRegister<E>> map) {
            this.map = map;
        }

        public void add(final ShiftRegister<E> register) {
            size++;
            map.put(register, map.getInt(register) + 1);
        }

        public void addAll(final Collection<? extends ShiftRegister<E>> c) {
            for (final ShiftRegister<E> register : c) {
                add(register);
            }
        }

        public void clear() {
            map.clear();
            size = 0;
        }

        public boolean contains(final ShiftRegister<E> register) {
            return map.containsKey(register);
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public PqgramProfile<E> intersection(final PqgramProfile<E> o) {
            final PqgramProfile<E> intersection = new PqgramProfile<E>();

            for (final ShiftRegister<E> r : map.keySet()) {
                final int count = Math.min(map.getInt(r), o.map.getInt(r));

                if (count > 0) {
                    intersection.map.put(r, count);
                    intersection.size += count;
                }
            }

            return intersection;
        }

        public int intersectionSize(final PqgramProfile<E> o) {
            int intersectionSize = 0;

            for (final ShiftRegister<E> r : map.keySet()) {
                intersectionSize += Math.min(map.getInt(r), o.map.getInt(r));
            }

            return intersectionSize;
        }

        public int size() {
            return size;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof PqgramProfile<?>)) {
                return false;
            }

            final PqgramProfile<E> bag = (PqgramProfile<E>) o;

            for (final ShiftRegister<E> r : map.keySet()) {
                if (map.getInt(r) != bag.map.getInt(r)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public PqgramProfile clone() {
            return new PqgramProfile((Object2IntOpenHashMap<ShiftRegister>) map.clone());
        }

        @Override
        public String toString() {
            return map.toString();
        }

        /**
         * Calculates the pq-gram tree similarity metric between two trees. See Augsten, Bohlen, Gamper, 2005.
         * 
         * @param profile1 Bag profile of a tree
         * @param profile2 Bag profile of a tree
         * @return tree similarity
         */
        public final static <E> float pqgramDistance(final PqgramProfile<E> profile1,
                final PqgramProfile<E> profile2) {
            final int bagUnionCardinality = profile1.size() + profile2.size();
            final int bagIntersectionCardinality = profile1.intersectionSize(profile2);

            return 1f - 2f * bagIntersectionCardinality / bagUnionCardinality;
        }
    }

    public static interface LabelParser<T extends Object> {
        public T parse(String label) throws Exception;
    }
}
