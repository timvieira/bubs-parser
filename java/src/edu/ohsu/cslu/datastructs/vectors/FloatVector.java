package edu.ohsu.cslu.datastructs.vectors;

public interface FloatVector extends NumericVector {

    /**
     * Divides each element of two {@link Vector}s, returning a new {@link DenseFloatVector}
     * 
     * @param v divisor, a vector of the same length.
     * @return a new vector in which each element is the quotient of the corresponding elements of the two supplied
     *         {@link Vector}s.
     */
    public FloatVector elementwiseDivide(NumericVector v);

    /**
     * Takes the natural logarithm of each element, returning a new {@link DenseFloatVector}
     * 
     * @return a new vector in which each element is the natural log of the corresponding element of the original
     *         {@link Vector}.
     */
    public FloatVector elementwiseLog();

    /**
     * Adds a {@link Vector} to this vector, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param v addend, a vector of the same length.
     * @return a reference to this vector.
     */
    public FloatVector inPlaceAdd(final Vector v);

    /**
     * Adds an addend to the elements of this vector indicated by the populated indices of a bit vector, returning a
     * reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param v a {@link BitVector} of the same length.
     * @param addend the float to be added to the elements indicated by populated elements in v
     * @return a reference to this vector.
     */
    public FloatVector inPlaceAdd(final BitVector v, final float addend);

    /**
     * Multiplies this {@link Vector} by another {@link Vector}, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param v multiplicand, a vector of the same length.
     * @return a reference to this vector.
     */
    public FloatVector inPlaceElementwiseMultiply(final Vector v);

    /**
     * Divides this {@link Vector} by another {@link Vector}, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param v divisor, a vector of the same length.
     * @return a reference to this vector.
     */
    public FloatVector inPlaceElementwiseDivide(final Vector v);

    /**
     * Adds the provided scalar to this {@link Vector}, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param addend Scalar value.
     * @return a reference to this vector.
     */
    public FloatVector inPlaceScalarAdd(final float addend);

    /**
     * Multiplies this {@link Vector} by the provided scalar, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @param multiplicand Scalar value.
     * @return a reference to this vector.
     */
    public FloatVector inPlaceScalarMultiply(final float multiplicand);

    /**
     * Takes the log of this {@link Vector}, returning a reference to this vector
     * 
     * Caution: This method changes the contents of this vector
     * 
     * @return a reference to this vector.
     */
    public FloatVector inPlaceElementwiseLog();
}
