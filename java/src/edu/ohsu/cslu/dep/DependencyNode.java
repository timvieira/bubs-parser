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

package edu.ohsu.cslu.dep;

import java.util.LinkedList;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;

/**
 * @author Aaron Dunlop
 * @since Jun 5, 2012
 */
public class DependencyNode {

    public final String token;

    /** The score of the parent arc */
    public final float arcScore;

    /**
     * The (log) product of the scores of all nodes participating in the subtree rooted at this node. <em>Excludes</em>
     * the score of this node, since that score is for the arc from this node to its parent.
     */
    private float subtreeScore = Float.MIN_VALUE;

    public DependencyNode(final String token, final float arcScore) {
        this.token = token;
        this.arcScore = arcScore;
    }

    public DependencyNode(final Arc arc) {
        this.token = arc.token;
        this.arcScore = arc.score;
    }

    /**
     * @return The log product of the scores of all nodes participating in the subtree rooted at this node
     */
    public float subtreeScore(final LinkedList<NaryTree<DependencyNode>> children) {
        if (subtreeScore == Float.MIN_VALUE) {
            if (children == null || children.isEmpty()) {
                subtreeScore = 0f;
            } else {
                subtreeScore = 0f;
                for (final NaryTree<DependencyNode> child : children) {
                    final DependencyNode node = child.label();
                    subtreeScore += node.subtreeScore(child.children()) + Math.log(node.arcScore);
                }
            }
        }
        return subtreeScore;
    }

    @Override
    public String toString() {
        return token + " " + arcScore + " : " + subtreeScore;
    }
}
