/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
package edu.ohsu.cslu.grammar;

import java.io.Serializable;

public class NonTerminal implements Serializable {
    public String label;
    public boolean isPOS, isFactored, isLeftChild, isRightChild;

    public NonTerminal(final String label, final boolean isPOS, final boolean isFactored, final boolean isLeftChild,
            final boolean isRightChild) {
        this.label = label;
        this.isPOS = isPOS;
        this.isFactored = isFactored;
        this.isLeftChild = isLeftChild;
        this.isRightChild = isRightChild;
    }

    public NonTerminal(final String label) {
        this(label, false, false, false, false);
    }

    public String getLabel() {
        return label;
    }

    public boolean isPOS() {
        return isPOS;
    }

    public boolean isWordLevel() {
        return isPOS;
    }

    public boolean isClauseLevel() {
        return !isPOS;
    }

    public boolean isFactored() {
        return isFactored;
    }

    public boolean isLeftChild() {
        return isLeftChild;
    }

    public boolean isRightChild() {
        return isRightChild;
    }
}
