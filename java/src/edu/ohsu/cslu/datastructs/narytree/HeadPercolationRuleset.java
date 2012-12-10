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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Implements a set of head-percolation rules.
 * 
 * The file format is one head nonterminal per line, followed by a bracketed set of equivalence classes and a 'r' or 'l'
 * denoting whether that class prefers the rightmost or leftmost matching child. The first line of the file is a default
 * ruleset denoted by '*default*'
 * 
 * e.g.:
 * 
 * PP (l IN RP TO) (r PP)
 * 
 * This rule indicates that the head child of a PP node will be the leftmost IN, RP, or TO child. If no child matches,
 * the rightmost PP will be chosen. Failing that test, the default rules will be applied.
 * 
 * For further details, see section 2.2.3 of Roark et al., 2006, 'SParseval: Evaluation Metrics for Parsing Speech'.
 * 
 * @author Aaron Dunlop
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
        final LinkedList<CategorySet> defaultCategorySets = parse(defaultLine.substring(defaultLine.indexOf(' ')));

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
                for (final ListIterator<String> it = childProductions.listIterator(childProductions.size()); it
                        .hasPrevious();) {
                    if (preference.categories.contains(it.previous())) {
                        return it.previousIndex() + 1;
                    }
                }
                break;
            case Leftmost:
                for (final ListIterator<String> it = childProductions.listIterator(); it.hasNext();) {
                    if (preference.categories.contains(it.next())) {
                        return it.nextIndex() - 1;
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
