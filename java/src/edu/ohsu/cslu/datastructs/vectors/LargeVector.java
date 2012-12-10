package edu.ohsu.cslu.datastructs.vectors;

/**
 * Interface for large vectors, indexed by longs instead of ints
 * 
 * @author Aaron Dunlop
 */
public interface LargeVector extends Vector {

    /**
     * Retrieves a vector element
     * 
     * @param i index
     * @return the vector element as an integer
     */
    public int getInt(final long i);

    /**
     * Retrieves a vector element
     * 
     * @param i index
     * @return the vector element as a float
     */
    public float getFloat(final long i);

    /**
     * Retrieves a vector element
     * 
     * @param i index
     * @return the vector element as a boolean
     */
    public boolean getBoolean(final long i);

    /**
     * Sets a vector element
     * 
     * @param i index
     * @param value new value
     */
    public void set(final long i, final int value);

    /**
     * Sets a vector element
     * 
     * @param i index
     * @param value new value
     */
    public void set(final long i, final float value);

    /**
     * Sets a vector element
     * 
     * @param i index
     * @param value new value
     */
    public void set(final long i, final boolean value);

    /**
     * Parses and sets a vector element
     * 
     * @param i index
     * @param newValue new value
     */
    public void set(final long i, final String newValue);
}
