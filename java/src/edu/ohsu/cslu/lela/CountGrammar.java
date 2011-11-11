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
package edu.ohsu.cslu.lela;

/**
 * A Grammar computed from (possibly fractional) observation counts.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 */
public interface CountGrammar {

    /**
     * Returns the number of observations of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return the number of observations of a binary rule.
     */
    public double binaryRuleObservations(final String parent, final String leftChild, final String rightChild);

    /**
     * Returns the number of observations of a unary rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a unary rule.
     */
    public double unaryRuleObservations(final String parent, final String child);

    /**
     * Returns the number of observations of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return the number of observations of a lexical rule.
     */
    public double lexicalRuleObservations(final String parent, final String child);

    /**
     * Returns the total number of times a non-terminal was observed as a parent (optional operation).
     * 
     * @param parent
     * @return the total number of times a non-terminal was observed as a parent
     */
    public double observations(final String parent);

    /**
     * @return The total number of observed rules
     */
    public int totalRules();

    /**
     * @return The number of observed binary rules
     */
    public int binaryRules();

    /**
     * @return The number of observed unary rules
     */
    public int unaryRules();

    /**
     * @return The number of observed lexical rules
     */
    public int lexicalRules();
}
