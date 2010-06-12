/*
 * OpenCL Kernels shared by all OpenCL parser implementations
 */

/*
 * Merges one cartesian-product vector into another, choosing the maximum probability and midpoint for each 
 *   non-terminal pair. We've separated the cartesian product and the union of those products across
 *   midpoints into separate kernels. This should help avoid thread divergence, but there's probably a 
 *   more efficient approach.
 *
 * Warning: This union is destructive, overwriting the initial contents of 
 *   cartesianProductProbabilities0 and cartesianProductMidpoints0
 *
 * @param cartesianProductProbabilities0 The cartesian-product vector to merge into, stored as a 
 * @param cartesianProductMidpoints0     parallel array of probabilities and midpoints
 *
 * @param cartesianProductProbabilities1 The cartesian-product vector to merge from
 * @param cartesianProductMidpoints1
 *
 * @param packedArraySize Size of the cartesian-product arrays. Sizes range from 7k all the way to 
 *                          35 million for the grammars we use. For the Berkeley grammar, it's around
 *                          1-2 million.
 *
 * TODO: We currently start one thread for each element. Would it be better 
 *       to iterate over sections of the arrays?
 */
__kernel void cartesianProductUnion(__global float* cartesianProductProbabilities0,
        __global short* cartesianProductMidpoints0,
        const __global float* cartesianProductProbabilities1,
        const __global short* cartesianProductMidpoints1,
        uint packedArraySize) {

    uint threadId = get_global_id(0);

    if (threadId < packedArraySize) {
        float probability0 = cartesianProductProbabilities0[threadId];
        float probability1 = cartesianProductProbabilities1[threadId];
        
        if (probability1 > probability0) {
            cartesianProductProbabilities0[threadId] = probability1;
            cartesianProductMidpoints0[threadId] = cartesianProductMidpoints1[threadId];
        }
    }
}

/**
 * Fills a float* array with the specified value
 *
 * TODO: It seems like there should be a built-in function to do this.
 *       Maybe vector operations would be better?
 *
 * TODO: We currently start one thread for each element. Would it be better 
 *       to iterate over sections of the array?
 */
__kernel void fillFloat(__global float* buffer,
        uint size,
        float value) {

    uint threadId = get_global_id(0);

    if (threadId < size) {
        buffer[threadId] = value;
    }
}
