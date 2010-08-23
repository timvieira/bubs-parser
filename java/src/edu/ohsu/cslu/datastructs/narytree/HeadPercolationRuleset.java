package edu.ohsu.cslu.datastructs.narytree;

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
public class HeadPercolationRuleset {

    Map<String, List<CategorySet>> rules = new HashMap<String, List<CategorySet>>();

    public HeadPercolationRuleset(final Reader rulesetReader) throws IOException {
        init(rulesetReader);
    }

    protected HeadPercolationRuleset(final String ruleset) {
        try {
            init(new StringReader(ruleset));
        } catch (final IOException ignore) {
            // StringReader won't IOException
        }
    }

    private void init(final Reader rulesetReader) throws IOException {
        final BufferedReader br = new BufferedReader(rulesetReader);

        // The first line is the 'default'
        final String defaultLine = br.readLine();
        final LinkedList<CategorySet> defaultCategorySets = parse(defaultLine.substring(defaultLine
            .indexOf(' ')));

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final int split = line.indexOf(' ');
            final String category = line.substring(0, split);
            final LinkedList<CategorySet> categorySets = parse(line.substring(split));
            categorySets.addAll(defaultCategorySets);
            rules.put(category, categorySets);
        }
    }

    private LinkedList<CategorySet> parse(final String line) {
        final LinkedList<CategorySet> categorySets = new LinkedList<CategorySet>();

        final String s = line.replaceAll("\\)", "");
        final String[] split = s.split(" *\\(");
        for (int i = 1; i < split.length; i++) {
            final CategorySet cs = new CategorySet(split[i].split(" +"));
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
    public int headChild(final String parentProduction, final List<String> childProductions) {
        final List<CategorySet> preferences = rules.get(parentProduction);
        for (final CategorySet preference : preferences) {
            switch (preference.direction) {
                case Rightmost:
                    for (int i = childProductions.size() - 1; i >= 0; i--) {
                        if (preference.categories.contains(childProductions.get(i))) {
                            return i;
                        }
                    }
                    break;
                case Leftmost:
                    for (int i = 0; i < childProductions.size(); i++) {
                        if (preference.categories.contains(childProductions.get(i))) {
                            return i;
                        }
                    }
                    break;
            }
        }
        throw new IllegalArgumentException("Unable to find head child for " + parentProduction);
    }

    private class CategorySet {

        private final Preference direction;
        private final Set<String> categories = new HashSet<String>();

        public CategorySet(final String[] split) {
            direction = Preference.forString(split[0]);
            for (int i = 1; i < split.length; i++) {
                categories.add(split[i]);
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(128);
            sb.append(direction.toString());
            sb.append(" : ");
            for (final String category : categories) {
                sb.append(category);
                sb.append(", ");
            }
            // Delete the final ', '
            sb.delete(sb.length() - 3, sb.length() - 1);
            return sb.toString();
        }
    }

    private enum Preference {
        Rightmost, Leftmost;

        public static Preference forString(final String s) {
            if (s.equals("r")) {
                return Rightmost;
            } else if (s.equals("l")) {
                return Leftmost;
            }

            throw new IllegalArgumentException("Unknown string representation: " + s);
        }

        @Override
        public String toString() {
            switch (this) {
                case Rightmost:
                    return "Rightmost";

                case Leftmost:
                    return "Leftmost";

                default:
                    return "Unknown";
            }
        }
    }
}
