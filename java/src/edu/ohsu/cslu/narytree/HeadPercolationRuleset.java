package edu.ohsu.cslu.narytree;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements a set of head-percolation rules.
 * 
 * @author Aaron Dunlop
 * @since Oct 24, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class HeadPercolationRuleset
{
    Map<String, List<CategorySet>> rules = new HashMap<String, List<CategorySet>>();

    public HeadPercolationRuleset(Reader rulesetReader) throws IOException
    {
        init(rulesetReader);
    }

    protected HeadPercolationRuleset(String ruleset)
    {
        try
        {
            init(new StringReader(ruleset));
        }
        catch (IOException ignore)
        {
            // StringReader won't IOException
        }
    }

    private void init(Reader rulesetReader) throws IOException
    {
        BufferedReader br = new BufferedReader(rulesetReader);

        // The first line is the 'default'
        String defaultLine = br.readLine();
        LinkedList<CategorySet> defaultCategorySets = parse(defaultLine.substring(defaultLine.indexOf(' ')));

        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            int split = line.indexOf(' ');
            String category = line.substring(0, split);
            LinkedList<CategorySet> categorySets = parse(line.substring(split));
            categorySets.addAll(defaultCategorySets);
            rules.put(category, categorySets);
        }
    }

    private LinkedList<CategorySet> parse(String line)
    {
        LinkedList<CategorySet> categorySets = new LinkedList<CategorySet>();

        String s = line.replaceAll("\\)", "");
        String[] split = s.split(" *\\(");
        for (int i = 1; i < split.length; i++)
        {
            CategorySet cs = new CategorySet(split[i].split(" +"));
            categorySets.add(cs);
        }

        return categorySets;
    }

    /**
     * Returns the index of the child preferred as the head of a production
     * 
     * @param parentProduction
     * @param childProductions
     * @return the index of the head child
     */
    public int headChild(String parentProduction, String[] childProductions)
    {
        List<CategorySet> preferences = rules.get(parentProduction);
        for (CategorySet preference : preferences)
        {
            switch (preference.direction)
            {
                case Rightmost :
                    for (int i = childProductions.length - 1; i >= 0; i--)
                    {
                        if (preference.categories.contains(childProductions[i]))
                        {
                            return i;
                        }
                    }
                    break;
                case Leftmost :
                    for (int i = 0; i < childProductions.length; i++)
                    {
                        if (preference.categories.contains(childProductions[i]))
                        {
                            return i;
                        }
                    }
                    break;
            }
        }
        throw new IllegalArgumentException("Unable to find head child for " + parentProduction);
    }

    private class CategorySet
    {
        private final Preference direction;
        private final Set<String> categories = new HashSet<String>();

        public CategorySet(String[] split)
        {
            direction = Preference.forString(split[0]);
            for (int i = 1; i < split.length; i++)
            {
                categories.add(split[i]);
            }
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder(128);
            sb.append(direction.toString());
            sb.append(" : ");
            for (String category : categories)
            {
                sb.append(category);
                sb.append(", ");
            }
            // Delete the final ', '
            sb.delete(sb.length() - 3, sb.length() - 1);
            return sb.toString();
        }
    }

    private enum Preference
    {
        Rightmost, Leftmost;

        public static Preference forString(String s)
        {
            if (s.equals("r"))
            {
                return Rightmost;
            }
            else if (s.equals("l"))
            {
                return Leftmost;
            }

            throw new IllegalArgumentException("Unknown string representation: " + s);
        }

        @Override
        public String toString()
        {
            switch (this)
            {
                case Rightmost :
                    return "Rightmost";

                case Leftmost :
                    return "Leftmost";

                default :
                    return "Unknown";
            }
        }
    }
}
