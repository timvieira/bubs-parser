/**
 * Format.java
 *   Copyright 2009 Aaron Dunlop
 */
package edu.ohsu.cslu.tools;

import org.kohsuke.args4j.EnumAliasMap;

/**
 * Enumeration of text input and output formats.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public enum FileFormat {

    /** Penn Treebank format parenthesis-bracketed hierarchical tree */
    BracketedTree("tree", "bracketed-tree"),

    /** Square-bracketed tree format (same as {@link #BracketedTree} but using square brackets) */
    SquareBracketedTree("square-bracketed-tree"),

    /** Parenthesis-bracketed flat format: (feature1 feature2 feature3...) (feature1 feature2 feature3...)... */
    Bracketed("bracketed"),

    /** Square-bracketed flat format (same as {@link #Bracketed} but using square brackets) */
    SquareBracketed("square-bracketed"),

    /** Parenthesis-bracketed flat format: (POS word feature3 ...) (POS word feature3 ...) ... */
    PosTagged("pos-tagged"),

    /** Parenthesis-bracketed flat format: (same as {@link #PosTagged} but using square brackets) */
    SquarePosTagged("square-pos-tagged"),

    /** Slash-delimited flat format: feature1/feature2/feature3 feature1/feature2/feature3 ... */
    Stanford("stanford");

    private FileFormat(final String... aliases) {
        EnumAliasMap.singleton().addAliases(this, aliases);
    }

    public boolean isTreeFormat() {
        return this == BracketedTree || this == SquareBracketedTree;
    }

    public boolean isFlatFormat() {
        return this == Bracketed || this == SquareBracketed || this == PosTagged || this == SquarePosTagged || this == Stanford;
    }
}