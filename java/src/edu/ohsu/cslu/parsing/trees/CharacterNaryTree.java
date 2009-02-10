package edu.ohsu.cslu.parsing.trees;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.ohsu.cslu.common.Vocabulary;


/**
 * N-Ary tree implementation using Characters as node labels.
 * 
 * @author Aaron Dunlop
 * @since Sep 22, 2008
 * 
 *        $Id$
 */

public final class CharacterNaryTree extends BaseNaryTree<Character>
{
    public CharacterNaryTree(final char label, final BaseNaryTree<Character> parent)
    {
        super(label, parent);
    }

    public CharacterNaryTree(final int label, final BaseNaryTree<Character> parent)
    {
        this((char) label, parent);
    }

    public CharacterNaryTree(final char label)
    {
        super(label);
    }

    public CharacterNaryTree(final int label)
    {
        this((char) label);
    }

    public CharacterNaryTree(final String label, final Vocabulary vocabulary)
    {
        this(label.charAt(0));
    }

    public Character label()
    {
        return new Character(charLabel());
    }

    public char charLabel()
    {
        return (char) intLabel();
    }

    public String stringLabel()
    {
        return Character.toString(charLabel());
    }

    /**
     * Type-strengthen return-type
     */
    @Override
    public CharacterNaryTree addChild(final String childLabel)
    {
        return addChild((int) childLabel.charAt(0));
    }

    /**
     * Type-strengthen return-type
     */
    @Override
    public CharacterNaryTree addChild(final int childLabel)
    {
        return (CharacterNaryTree) super.addChild(childLabel);
    }

    public CharacterNaryTree addChild(final char childLabel)
    {
        return addChild((int) childLabel);
    }

    @Override
    public NaryTree<Character> addChild(final Character childLabel)
    {
        return addChild((int) childLabel.charValue());
    }

    public void addChildren(final char[] childLabels)
    {
        for (int child : childLabels)
        {
            addChild(child);
        }
    }

    @Override
    public void addSubtree(final NaryTree<Character> subtree)
    {
        super.addSubtree((BaseNaryTree<Character>) subtree);
    }

    @Override
    public Iterator<Character> inOrderLabelIterator()
    {
        return new CharacterIterator(inOrderIntegerIterator());
    }

    @Override
    public Iterator<Character> postOrderLabelIterator()
    {
        return new CharacterIterator(postOrderIntegerIterator());
    }

    @Override
    public Iterator<Character> preOrderLabelIterator()
    {
        return new CharacterIterator(preOrderIntegerIterator());
    }

    @Override
    public boolean removeChild(final Character childLabel)
    {
        return removeChild(childLabel.charValue());
    }

    public void removeChildren(final char[] childLabels)
    {
        for (int childLabel : childLabels)
        {
            removeChild(childLabel);
        }
    }

    @Override
    public boolean removeSubtree(final Character childLabel)
    {
        return removeSubtree(childLabel.charValue());
    }

    @Override
    public CharacterNaryTree subtree(final Character childLabel)
    {
        return (CharacterNaryTree) subtree(childLabel.charValue());
    }

    @Override
    public List<Character> childLabels()
    {
        ArrayList<Character> list = new ArrayList<Character>(childList.size());
        for (BaseNaryTree<Character> child : childList)
        {
            list.add(new Character((char) child.label));
        }
        return list;
    }

    /**
     * Implements pq-gram profile as per Augsten, Bohlen, and Gamper 2005, page 306 (used to
     * calculate pq-gram distance.
     * 
     * Overrides BaseNaryTree implementation purely for debugging purposes - uses CharShiftRegister
     * instead of IntShiftRegister
     * 
     * @param p parameter
     * @param q parameter
     * @return profile
     */
    @Override
    public PqgramProfile pqgramProfile(final int p, final int q)
    {
        PqgramProfile profile = new PqgramProfile();
        pqgramProfile(p, q, profile, this, new CharShiftRegister(p));
        return profile;
    }

    /**
     * Implements pq-gram profile as per Augsten, Bohlen, and Gamper 2005, page 306 (used to
     * calculate pq-gram distance
     * 
     * Overrides BaseNaryTree implementation purely for debugging purposes - uses CharShiftRegister
     * instead of IntShiftRegister
     * 
     * @param p parameter
     * @param q parameter
     * @param profile Current profile
     * @param r Current tree
     * @param anc Current shift register
     * @return profile
     */
    protected void pqgramProfile(final int p, final int q, PqgramProfile profile, final CharacterNaryTree r,
        CharShiftRegister anc)
    {
        anc = anc.shift(r.charLabel());
        CharShiftRegister sib = new CharShiftRegister(q);

        if (r.isLeaf())
        {
            final CharShiftRegister concat = anc.concat(sib);
            profile.add(concat);
        }
        else
        {
            for (BaseNaryTree<Character> c0 : r.childList)
            {
                final CharacterNaryTree c = (CharacterNaryTree) c0;
                sib = sib.shift(c.charLabel());
                final CharShiftRegister concat = anc.concat(sib);
                profile.add(concat);
                pqgramProfile(p, q, profile, c, anc);
            }

            for (int k = 1; k < q; k++)
            {
                sib = sib.shift();
                final CharShiftRegister concat = anc.concat(sib);
                profile.add(concat);
            }
        }
    }

    /**
     * Reads in an CharacterNaryTree from a standard parenthesis-bracketed representation
     * 
     * @param inputStream The stream to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static CharacterNaryTree read(InputStream inputStream) throws IOException
    {
        return (CharacterNaryTree) read(new InputStreamReader(inputStream), CharacterNaryTree.class, null);
    }

    /**
     * Reads in an CharacterNaryTree from a standard parenthesis-bracketed representation
     * 
     * @param string String representation of the tree
     * @return the tree
     */
    public static CharacterNaryTree read(String string)
    {
        return (CharacterNaryTree) read(string, CharacterNaryTree.class, null);
    }

    /**
     * Reads in an CharacterNaryTree from a standard parenthesis-bracketed representation
     * 
     * @param reader The reader to read from
     * @return the tree
     * @throws IOException if the read fails
     */
    public static CharacterNaryTree read(Reader reader) throws IOException
    {
        return (CharacterNaryTree) read(reader, CharacterNaryTree.class, null);
    }

    @Override
    public String toString()
    {
        return charLabel() + " (" + childList.size() + " children)";
    }

    private static class CharacterIterator extends BaseLabelIterator implements Iterator<Character>
    {
        public CharacterIterator(final Iterator<Integer> intIterator)
        {
            super(intIterator);
        }

        @Override
        public Character next()
        {
            return new Character((char) intIterator.next().intValue());
        }
    }
}
