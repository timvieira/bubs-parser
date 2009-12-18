package edu.ohsu.cslu.datastructs.vectors;

import edu.ohsu.cslu.datastructs.matrices.Matrix;

/**
 * Base interface for numeric vector classes (essentially all {@link Vector}s which are not {@link BitVector}
 * s).
 * 
 * The methods defined here are applicable to any numeric representation, but not to a single bit.
 * 
 * @author Aaron Dunlop
 * @since Sep 17, 2008
 * 
 *        $Id$
 */
public interface NumericVector extends Vector {

    /**
     * Divides each element of two {@link Vector}s, returning a new {@link FloatVector}
     * 
     * @param v
     *            divisor, a vector of the same length.
     * @return a new vector in which each element is the quotient of the corresponding elements of the two
     *         supplied {@link Vector}s.
     */
    public FloatVector elementwiseDivide(NumericVector v);

    /**
     * Takes the natural logarithm of each element, returning a new {@link FloatVector}
     * 
     * @return a new vector in which each element is the natural log of the corresponding element of the
     *         original {@link Vector}.
     */
    public FloatVector elementwiseLog();

    /**
     * Returns the result of multiplying the specified matrix by this vector.
     * 
     * @param m
     *            Matrix
     * @return m x v
     */
    public NumericVector multiply(Matrix m);
}
