/**
 * Format.java
 *   Copyright 2009 Aaron Dunlop
 */
package edu.ohsu.cslu.tools;

enum FileFormat
{
    /** Penn Treebank parenthesis-bracketed format */
    BracketedTree,
    /** Square-bracketed tree format */
    SquareBracketedTree,
    /** Parenthesis-bracketed flat format */
    Bracketed,
    /** Square-bracketed flat format */
    SquareBracketed,
    /** Slash-delimited flat format */
    Stanford;

    public boolean isTreeFormat()
    {
        return this == BracketedTree || this == SquareBracketedTree;
    }

    public static FileFormat forString(String s)
    {
        if ("bracketed-tree".equalsIgnoreCase(s) || "tree".equalsIgnoreCase(s))
        {
            return BracketedTree;
        }
        if ("square-bracketed-tree".equalsIgnoreCase(s))
        {
            return SquareBracketedTree;
        }
        else if ("bracketed".equalsIgnoreCase(s))
        {
            return Bracketed;
        }
        else if ("square-bracketed".equalsIgnoreCase(s))
        {
            return SquareBracketed;
        }
        else if ("stanford".equalsIgnoreCase(s))
        {
            return Stanford;
        }
        else
        {
            throw new IllegalArgumentException("Unknown input format: " + s);
        }
    }

    public static FileFormat outputFormat(String s)
    {
        if ("bracketed".equalsIgnoreCase(s))
        {
            return Bracketed;
        }
        else if ("square-bracketed".equalsIgnoreCase(s))
        {
            return SquareBracketed;
        }
        else if ("stanford".equalsIgnoreCase(s))
        {
            return Stanford;
        }
        else
        {
            throw new IllegalArgumentException("Unknown or illegal output format: " + s);
        }
    }
}