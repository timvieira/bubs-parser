/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */

package edu.ohsu.cslu.dep;

import java.util.LinkedList;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;

/**
 * @author Aaron Dunlop
 * @since Jun 5, 2012
 */
public class DependencyNode {

    final short index;
    private short start = -1;
    private short span = -1;

    public final String token;

    /** The score of the parent arc */
    public final float arcScore;

    /**
     * The (log) product of the scores of all nodes participating in the subtree rooted at this node. <em>Excludes</em>
     * the score of this node, since that score is for the arc from this node to its parent.
     */
    float subtreeScore = Float.MIN_VALUE;

    public DependencyNode(final Arc arc) {
        this.index = (short) arc.index;
        this.token = arc.token;
        this.arcScore = arc.score;
    }

    /**
     * @return The log product of the scores of all nodes participating in the subtree rooted at this node
     */
    public float subtreeScore() {
        return subtreeScore;
    }

    float internalSubtreeScore(final LinkedList<NaryTree<DependencyNode>> children) {
        if (children == null || children.isEmpty()) {
            subtreeScore = 0f;
        } else {
            subtreeScore = 0f;
            for (final NaryTree<DependencyNode> child : children) {
                final DependencyNode node = child.label();
                subtreeScore += node.internalSubtreeScore(child.children()) + Math.log(node.arcScore);
            }
        }
        return subtreeScore;
    }

    public short span() {
        return span;
    }

    public short start() {
        return start;
    }

    short internalSpan(final LinkedList<NaryTree<DependencyNode>> children) {
        if (span < 0) {
            span = (short) (token != DependencyGraph.ROOT.token ? 1 : 0);

            if (children != null) {
                for (final NaryTree<DependencyNode> child : children) {
                    span += child.label().internalSpan(child.children());
                }
            }
        }

        return span;
    }

    short internalStart(final LinkedList<NaryTree<DependencyNode>> children) {
        if (start < 0) {
            if (children.isEmpty()) {
                start = (short) (index - 1);
            } else {
                final NaryTree<DependencyNode> child = children.getFirst();
                start = (short) Math.max(0, Math.min(index - 1, child.label().internalStart(child.children())));
            }
        }

        return start;
    }

    @Override
    public String toString() {
        return token + " " + arcScore + " : " + subtreeScore;
    }
}
