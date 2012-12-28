/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */

package edu.ohsu.cslu.tools;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;

/**
 * Counts parent nodes of specified tokens in a treebank.
 */
public class ParentNodes extends BaseCommandlineTool {

    @Option(name = "-l", metaVar = "label", usage = "Label(s) to report parents")
    private Set<String> targetLabels = new HashSet<String>();

    @Override
    protected void run() throws Exception {
        final HashMap<String, Object2IntOpenHashMap<String>> map = new HashMap<String, Object2IntOpenHashMap<String>>();
        for (final String label : targetLabels) {
            map.put(label, new Object2IntOpenHashMap<String>());
        }

        for (final String line : inputLines()) {
            final NaryTree<String> tree = NaryTree.read(line, String.class);
            for (final NaryTree<String> node : tree.inOrderTraversal()) {
                final Object2IntOpenHashMap<String> counts = map.get(node.label());
                if (counts != null && node.parent() != null) {
                    counts.add(node.parentLabel(), 1);
                }
            }
        }

        if (targetLabels.size() > 1) {
            for (final String label : targetLabels) {
                System.out.println("=== " + label + " ===");
                final Object2IntOpenHashMap<String> counts = map.get(label);
                for (final String parent : counts.keySet()) {
                    System.out.println(parent + " : " + counts.getInt(parent));
                }
            }
        } else {
            final Object2IntOpenHashMap<String> counts = map.get(targetLabels.iterator().next());
            for (final String parent : counts.keySet()) {
                System.out.println(parent + " : " + counts.getInt(parent));
            }

        }
    }

    public static void main(final String[] args) {
        run(args);
    }

}
