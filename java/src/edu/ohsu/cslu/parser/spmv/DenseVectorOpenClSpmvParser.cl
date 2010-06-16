/**
 * OpenCL kernels for DenseVectorOpenClSpmvParser.java
 *
 * Note: All kernels start with something like 'if threadId < n' so that we can start 
 *       thread blocks of an even multiple the warp size even if the data set is 
 *       not an even multiple (the last few threads will just exit)
 *
 * ====================================================================
 *
 * Algorithm Description
 *
 * --For each target cell:
 *
 *   --Compute the cartesian product of all observed child pairs across all midpoints
 *
 *     --Compute the cartesian product at midpoint 0:
 *         Each `midpoint' is defined by a left and right child cell pair
 *         If the left child cell contains `S' and `DT' and the right child cell contains
 *         `NP' and `VP', the cartesian product is S/NP, S/VP, DT/NP, DT/VP, each with an 
 *         associated probability (p(S) * p(NP), p(S) * p(VP), etc.)
 *
 *     --Compute the cartesian product at midpoint 1
 *
 *     --Take the union of the two cartesian products, choosing the maximum probability 
 *       child pair when duplicates are found
 *
 *     --Repeat for all remaining midpoints
 *
 *   --Perform Sparse Matrix x Vector multiplication of the grammar matrix (G) and the 
 *     cartesian product vector (X). The resulting vector is the non-terminal population 
 *     of the target cell.
 *
 * ====================================================================
 *
 * Data Structures (Documentation adapted from edu.ohsu.cslu.parser.chart.DenseVectorChart)
 *
 * The parse chart is represented as a three-way parallel array, segmented into cells 
 *   by uniform offsets. e.g., cell 0 = indices 0..1024, cell 1 = indices 25..2048, etc.
 *
 * -- float* chartInsideProbabilities: Probabilities of each non-terminal (NEGATIVE_INFINITY for unobserved non-terminals)
 * -- int* chartPackedChildren: Child pairs producing each non-terminal --- equivalent to the grammar rule
 * -- short* chartMidpoints: Midpoints at which the probability and child pair were found. Used to back-trace 
 *                           and construct the parse tree
 * 
 * Each parallel array entry consumes 4 + 4 + 2 = 10 bytes
 * 
 * Individual cells in the parallel array are indexed by cell offsets of fixed length (the number of
 * non-terminals in the grammar).
 * 
 * The ancillary data structures are relatively small, so the total size consumed is approximately = n * (n-1)
 * / 2 * V * 10 bytes.
 *
 * Note that unlike edu.ohsu.cslu.parser.chart.PackedArrayChart, the observed entries in each cell
 *   are not `packed' at the beginning of the cell's array range. This saves an additional packing
 *   scan after populating the cell, but is more expensive to access later on a CPU. It's unclear 
 *   at this point which storage format will be better on the GPU.
 *
 */
 
/**
 * Computes the cartesian product of non-terminals populated in one pair of child cells.
 * Stores the populated child pairs in a parallel array of probabilities and midpoints 
 *   (indexed by child pair)
 *
 * Launch 1 thread for each possible child pair (validLeftChildren * validRightChildren)
 *
 * @param chartInsideProbabilities Chart storage (as described in `Data Structures'
 * @param chartPackedChildren      documentation above)
 * @param chartMidpoints
 *      
 * @param leftCellOffset Offset into the chart array of the left child cell
 * @param leftChildrenStart The first non-terminal index valid as a left child
 * @param validLeftChildren The number of non-terminals valid as left children
 *      
 * @param rightCellOffset Offset into the chart array of the right child cell
 * @param rightChildrenStart The first non-terminal index valid as a right child
 * @param validRightChildren The number of non-terminals valid as right children
 *        
 * @param cartesianProductProbabilities Cartesian product parallel array to be populated. 
 * @param cartesianProductMidpoints     Probabilities and midpoints. Indexed by packed child pair.
 *
 * @param midpoint The midpoint which defines the pair of child cells
 *
 * TODO: Is it better to launch O(|V|^2) threads (approximately 1 million for the Berkeley grammar)
 *       or to iterate over subsets (left children perhaps)
 */
__kernel void cartesianProduct(const __global float* chartInsideProbabilities,
        const __global int* chartPackedChildren,
        const __global short* chartMidpoints,
        
        const uint leftCellOffset,
        const uint leftChildrenStart,
        const uint validLeftChildren,
        
        const uint rightCellOffset,
        const uint rightChildrenStart,
        const uint validRightChildren,
        
        __global float* cartesianProductProbabilities,
        __global short* cartesianProductMidpoints,
        ushort midpoint) {

    uint threadId = get_global_id(0);
    
    if (threadId < (validLeftChildren * validRightChildren)) {
        
        int leftNonTerminal = threadId / validRightChildren + leftChildrenStart;
        int rightNonTerminal = threadId % validRightChildren + rightChildrenStart;
        
        int leftIndex = leftCellOffset + leftNonTerminal;
        int rightIndex = rightCellOffset + rightNonTerminal;

        float leftProbability = chartInsideProbabilities[leftIndex];

        if (leftProbability > -INFINITY) {
            float rightProbability = chartInsideProbabilities[rightIndex];

            if (rightProbability > -INFINITY) {
                // Compute the packed child index
                int packedChild = PACK;

                // We're operating in the log domain, so we sum probabilities
                cartesianProductProbabilities[packedChild] = leftProbability + rightProbability;
                cartesianProductMidpoints[packedChild] = midpoint;
            }
        }
    }
}

/**
 * Performs Sparse Matrix X Vector multiplication between the binary grammar rule matrix
 * and the cartesian product of observed non-terminal child pairs.
 *
 * Populates the target cell with the resulting vector.
 *
 * @param chartInsideProbabilities Chart storage (as described in `Data Structures'
 * @param chartPackedChildren      documentation above)
 * @param chartMidpoints
 * @param targetCellOffset
 *
 * @param cartesianProductProbabilities Cartesian product parallel array. Probabilities
 * @param cartesianProductMidpoints     and midpoints. Indexed by packed child pair
 *
 * @param binaryRuleMatrixRowIndices Binary rule matrix in CSR format
 * @param binaryRuleMatrixColumnIndices
 * @param binaryRuleMatrixProbabilities
 * @param binaryRuleMatrixRows
 */        
 __kernel void binarySpmv(__global float* chartInsideProbabilities,
    __global int* chartPackedChildren,
    __global short* chartMidpoints,
    const uint targetCellOffset,
    const __global float* cartesianProductProbabilities,
    const __global short* cartesianProductMidpoints,
    const __global int* binaryRuleMatrixRowIndices,
    const __global int* binaryRuleMatrixColumnIndices,
    const __global float* binaryRuleMatrixProbabilities,
    uint binaryRuleMatrixRows) {
    uint threadId = get_global_id(0);
    uint parent = threadId;

    if (parent < binaryRuleMatrixRows) {
        // Production winningProduction = null;
        float winningProbability = -INFINITY;
        int winningPackedChildren = 0;
        short winningMidpoint = 0;

        // Iterate over possible children of the parent (columns with non-zero entries)
        for (int i = binaryRuleMatrixRowIndices[parent]; i < binaryRuleMatrixRowIndices[parent + 1]; i++) {
            int grammarChildren = binaryRuleMatrixColumnIndices[i];
            float grammarProbability = binaryRuleMatrixProbabilities[i];

            float cartesianProductProbability = cartesianProductProbabilities[grammarChildren];
            float jointProbability = grammarProbability + cartesianProductProbability;

            if (jointProbability > winningProbability) {
                winningProbability = jointProbability;
                winningPackedChildren = grammarChildren;
                winningMidpoint = cartesianProductMidpoints[grammarChildren];
            }
        }

        int index = targetCellOffset + parent;
        
        chartInsideProbabilities[index] = winningProbability;
        if (winningProbability != -INFINITY) {
            chartPackedChildren[index] = winningPackedChildren;
            chartMidpoints[index] = winningMidpoint;
        }
    }
}

/**
 * Applies the unary rule matrix to the observed non-terminals in a cell. Like the binary rule matrix, 
 * the unary grammar rule matrix is stored in a CSR format, but is generally much smaller.
 *
 * This operation is performed _after_ the binary multiplication, potentially overwriting
 * the probabilities of non-terminal parents populated in that step if a unary rule is 
 * found which produces the same non-terminal with higher probability.
 *
 * Because the unary rule matrix is fairly small, this operation is generally quite fast.
 *
 * @param chartInsideProbabilities Chart storage (as described in `Data Structures'
 * @param chartPackedChildren      documentation above)
 * @param chartMidpoints
 * @param targetCellOffset
 *
 * @param csrUnaryRowStartIndices Unary rule matrix in CSR format
 * @param csrUnaryColumnIndices
 * @param csrUnaryProbabilities
 * @param unaryRuleMatrixRows
 *
 * @param chartCellEnd If we populate a non-terminal probability with a unary production,
 *                     we label it thus by storing the cell's end span as the midpoint
 *                     so we can reproduce the unary production when we back-trace and
 *                     create the chart.
 */        
__kernel void unarySpmv(__global float* chartInsideProbabilities,
        __global int* chartPackedChildren,
        __global short* chartMidpoints,
        const uint targetCellOffset,
        const __global int* csrUnaryRowStartIndices,
        const __global short* csrUnaryColumnIndices,
        const __global float* csrUnaryProbabilities,
        uint unaryRuleMatrixRows,
        short chartCellEnd) {

    uint threadId = get_global_id(0);
    uint parent = threadId;

    if (parent < unaryRuleMatrixRows) {
        int index = targetCellOffset + parent;
        float winningProbability = chartInsideProbabilities[index];
        int winningChild = INT_MIN;
        short winningMidpoint = 0;

        // Iterate over possible children of the parent (columns with non-zero entries)
        for (int i = csrUnaryRowStartIndices[parent]; i < csrUnaryRowStartIndices[parent + 1]; i++) {
            int child = csrUnaryColumnIndices[i];
            float grammarProbability = csrUnaryProbabilities[i];

            float jointProbability = grammarProbability + chartInsideProbabilities[targetCellOffset + child];

            if (jointProbability > winningProbability) {
                winningProbability = jointProbability;
                winningChild = child;
                winningMidpoint = chartCellEnd;
            }
        }

        if (winningChild != INT_MIN) {
            chartInsideProbabilities[index] = winningProbability;
            chartPackedChildren[index] = PACK_UNARY;
            chartMidpoints[index] = winningMidpoint;
        }
    }
}

/*

Another possibile cartesian-product algorithm:

Store p, m for each V_r in OpenCL local memory (CUDA shared memory).

Create a thread-group for each left non-terminal. (O(V_l) thread groups of 32 threads each (1 warp). 
  Total ~35k threads for Berkeley grammar, 85k for R2, and 190k for R2-p1). 
  Note: We could combine these by iterating within the thread if we need to reduce thread count.
  
  --Iterate through midpoints (left child cells)
    Synchronize threads on each loop?

    --If NT found in left child cell: (1 global read)
      We need to somehow pass in references to _all_ child cells. 
      Pre-allocate a single huge array for all cell contents? 2D array?
      
      --Iterate through observed right cell non-terminals (O(V_r) global reads)
        --Store midpoint, and probability in shared memory (choosing most probable if already present).
          This requires an if (x > v[i]) ..., which will cause thread divergence. sync threads after if?
        
          We only need to store p and m, indexed by V_r, so space is 6 * V_r bytes 
          (~6k for Berkeley grammar, ~.5k for R2 and ~1.7k for R2-p1). 

          We have 16 KB of shared memory in each multiprocessor, so we should be able to 
          hold the entire array in shared memory (2 thread blocks per SM for Berkeley grammar).
          
      --Write all NT pairs from shared memory to global RAM (O(V_r) global writes; should coalesce nicely)

Total: O(V_l * V_r) global memory writes (1 time through the V^2 vector)
*/
