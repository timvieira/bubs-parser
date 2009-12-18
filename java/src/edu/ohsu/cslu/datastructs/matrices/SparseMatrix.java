package edu.ohsu.cslu.datastructs.matrices;

/**
 * Base interface for all sparse matrix implementations.
 * 
 * @author Aaron Dunlop
 * @since Dec 16, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface SparseMatrix extends Matrix {
    // TODO Add getters and setters with columns indexed by longs instead of ints (clearly those
    // methods aren't applicable for dense matrices)
}
