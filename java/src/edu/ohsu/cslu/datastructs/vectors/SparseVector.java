package edu.ohsu.cslu.datastructs.vectors;

public interface SparseVector extends Vector {

    /**
     * Rehashes the underlying sparse table, making it as small as possible, while still satisfying the load factor. If
     * the underlying datastore is not a hash table, this method is a noop.
     */
    public void trim();
}
