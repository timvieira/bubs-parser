package edu.ohsu.cslu.common;

import java.io.IOException;
import java.io.Writer;

/**
 * Represents a set of tokens legal in a sequence. Used for alignment and matching tasks.
 * 
 * {@link Vocabulary} implementations map the actual tokens contained to ints for efficient storage.
 * Special tokens such as 'TOP' (for parsing) and 'gap' (for alignment) should be mapped to
 * well-known ints (Usually 0 or negative ints).
 * 
 * @author Aaron Dunlop
 * @since Oct 4, 2008
 * 
 *        $Id$
 */
public interface Vocabulary
{
    /**
     * @return tokens supported by this vocabulary.
     */
    public String[] tokens();

    /**
     * Returns the size of this vocabulary
     * 
     * @return size of this vocabulary
     */
    public int size();

    /**
     * Returns the token represented by the specified index
     * 
     * @param index integer representation
     * @return the token represented by the specified index
     */
    public String map(int index);

    /**
     * Returns the tokens represented by the specified indices
     * 
     * @param index integer representation
     * @return the tokens represented by the specified indices
     */
    public String[] map(int[] indices);

    /**
     * Returns the index representing the specified token
     * 
     * @param token String token
     * @return the index representing the specified token
     */
    public int map(String token);

    /**
     * Returns the indices representing the specified tokens
     * 
     * @param token String token
     * @return the indices representing the specified tokens
     */
    public int[] map(String[] labels);

    /**
     * Writes a human-readable representation of this vocabulary out to the specified writer
     * 
     * @param writer
     * @throws IOException if the write fails
     */
    public void write(Writer writer) throws IOException;
}
