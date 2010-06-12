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
 * The parse chart is represented as a 4-way parallel array, segmented into cells 
 *   by uniform offsets. e.g., cell 0 = indices 0..1024, cell 1 = indices 25..2048, etc.
 *
 * -- short* chartNonTerminalIndices: Populated non-terminals
 * -- float* chartInsideProbabilities: Probabilities of each non-terminal (NEGATIVE_INFINITY for unobserved non-terminals)
 * -- int* chartPackedChildren: Child pairs producing each non-terminal --- equivalent to the grammar rule
 * -- short* chartMidpoints: Midpoints at which the probability and child pair were found. Used to 
                             back-trace and construct the parse tree
 * 
 * Those 4 pieces of information allow us to back-trace through the chart and construct the parse tree.
 * 
 * We also maintain int* chartNumNonTerminals, indexed by cell number, containing the number of non-terminals 
 *   populated in each cell
 *
 * Each parallel array entry consumes 2 + 4 + 4 + 2 = 12 bytes
 * 
 * The ancillary data structures are relatively small, so the total size consumed is approximately = n * (n-1)
 * / 2 * V * 12 bytes.
 * 
 * Similar to {@link DenseVectorChart}, but observed non-terminals are packed together in
 * {@link PackedArrayChartCell#finalizeCell()}; this packing scan and the resulting denser access to observed
 * non-terminals may prove beneficial on certain architectures.
 *
 * Note that unlike edu.ohsu.cslu.parser.chart.DenseVectorChart, the observed entries in each cell
 *   must be `packed' at the beginning of the cell's array range. It's unclear 
 *   at this point which storage format will be better on the GPU (and to what degree that might 
 *   depend on the grammar), so we implement both.
 *
 */
 
/**
 * Computes the cartesian product of non-terminals populated in one pair of child cells.
 * Stores the populated child pairs in a parallel array of probabilities and midpoints 
 *   (indexed by child pair)
 *
 * Launch 1 thread for each possible child pair (validLeftChildren * validRightChildren)
 *
 * @param chartNonTerminalIndices  Chart storage (as described in `Data Structures'
 * @param chartInsideProbabilities documentation above)
 * @param chartPackedChildren      
 * @param chartMidpoints
 *      
 * @param leftChildrenStart    The offset into the chart array for the first non-terminal
 *                               observed in the left child cell which is valid as a left child.
 * @param observedLeftChildren The number of non-terminals valid as left children observed in 
 *                               the left child cell
 *      
 * @param righttChildrenStart  The offset into the chart array for the first non-terminal
 *                               observed in the right child cell which is valid as a right 
 *                               child.
 * @param observedLeftChildren The number of non-terminals valid as right children observed in 
 *                               the right child cell
 *        
 * @param cartesianProductProbabilities Cartesian product parallel array to be populated. 
 * @param cartesianProductMidpoints     Probabilities and midpoints. Indexed by packed 
 *                                        child pair.
 *
 * @param midpoint The midpoint which defines the pair of child cells
 *
 * TODO: Is it better to launch O(|V|^2) threads (1+ million for the Berkeley grammar)
 *       or to iterate over subsets (left children perhaps)
 */
__kernel void cartesianProduct(const __global short* chartNonTerminalIndices,
        const __global float* chartInsideProbabilities,
        const __global int* chartPackedChildren,
        const __global short* chartMidpoints,
        
        const uint leftChildrenStart,
        const uint observedLeftChildren,
        
        const uint rightChildrenStart,
        const uint observedRightChildren,
        
        __global float* cartesianProductProbabilities,
        __global short* cartesianProductMidpoints,
        ushort midpoint) {

    uint threadId = get_global_id(0);
    
    if (threadId < (observedLeftChildren * observedRightChildren)) {
        
        int leftIndex = leftChildrenStart + threadId / observedRightChildren;
        int rightIndex =  rightChildrenStart + threadId % observedRightChildren;
        
        int leftNonTerminal = chartNonTerminalIndices[leftIndex];

        float leftProbability = chartInsideProbabilities[leftIndex];

        if (leftProbability > -INFINITY) {
            int rightNonTerminal = chartNonTerminalIndices[rightIndex];
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
 * @param targetCellInsideProbabilities Temporary target cell storage. Indexed by 
 * @param targetCellPackedChildren      non-terminal
 * @param targetCellMidpoints
 *
 * @param cartesianProductProbabilities Cartesian product parallel array. Probabilities
 * @param cartesianProductMidpoints     and midpoints. Indexed by packed child pair
 *
 * @param binaryRuleMatrixRowIndices    Binary rule matrix in CSR format
 * @param binaryRuleMatrixColumnIndices
 * @param binaryRuleMatrixProbabilities
 * @param binaryRuleMatrixRows
 */        
 __kernel void binarySpmvMultiply(__global float* targetCellInsideProbabilities,
    __global int* targetCellPackedChildren,
    __global short* targetCellMidpoints,
    const __global float* cartesianProductProbabilities,
    const __global short* cartesianProductMidpoints,
    const __global int* binaryRuleMatrixRowIndices,
    const __global int* binaryRuleMatrixColumnIndices,
    const __global float* binaryRuleMatrixProbabilities,
    uint binaryRuleMatrixRows) {
    
    uint parent = get_global_id(0);

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

        if (winningProbability != -INFINITY) {
            targetCellInsideProbabilities[parent] = winningProbability;
            targetCellPackedChildren[parent] = winningPackedChildren;
            targetCellMidpoints[parent] = winningMidpoint;
        }
    }
}

/**
 * Performs Sparse Matrix X Vector multiplication between the unary grammar rule matrix
 * and the observed non-terminals in a cell. The unary grammar rule matrix is 
 * stored in the same format as the binary rule matrix, but is generally much smaller.
 *
 * This operation is performed _after_ the binary multiplication, potentially overwriting
 * the probabilities of non-terminal parents populated in that step if a unary rule is 
 * found which produces the same non-terminal with higher probability.
 *
 * Because the unary rule matrix is fairly small, this operation is generally quite fast.
 *
 * @param targetCellInsideProbabilities Temporary target cell storage. Indexed by 
 * @param targetCellPackedChildren      non-terminal
 * @param targetCellMidpoints
 *
 * @param unaryRuleMatrixRowIndices     Binary rule matrix in CSR format
 * @param unaryRuleMatrixColumnIndices
 * @param unaryRuleMatrixProbabilities
 * @param unaryRuleMatrixRows
 *
 * @param chartCellEnd If we populate a non-terminal probability with a unary production,
 *                     we label it thus by storing the cell's end span as the midpoint
 *                     so we can reproduce the unary production when we back-trace and
 *                     create the chart.
 */        
__kernel void unarySpmvMultiply(__global float* targetCellInsideProbabilities,
        __global int* targetCellPackedChildren,
        __global short* targetCellMidpoints,
        const __global int* unaryRuleMatrixRowIndices,
        const __global int* unaryRuleMatrixColumnIndices,
        const __global float* unaryRuleMatrixProbabilities,
        uint unaryRuleMatrixRows,
        short chartCellEnd) {

    uint threadId = get_global_id(0);
    uint parent = threadId;

    if (parent < unaryRuleMatrixRows) {
        float winningProbability = targetCellInsideProbabilities[parent];
        int winningPackedChildren = INT_MIN;
        short winningMidpoint = 0;

        // Iterate over possible children of the parent (columns with non-zero entries)
        for (int i = unaryRuleMatrixRowIndices[parent]; i < unaryRuleMatrixRowIndices[parent + 1]; i++) {
            int grammarChildren = unaryRuleMatrixColumnIndices[i];
            int leftChild = unpackLeftChild(grammarChildren);
            float grammarProbability = unaryRuleMatrixProbabilities[i];

            float jointProbability = grammarProbability + targetCellInsideProbabilities[leftChild];

            if (jointProbability > winningProbability) {
                winningProbability = jointProbability;
                winningPackedChildren = grammarChildren;
                winningMidpoint = chartCellEnd;
            }
        }

        if (winningPackedChildren != INT_MIN) {
            targetCellInsideProbabilities[parent] = winningProbability;
            targetCellPackedChildren[parent] = winningPackedChildren;
            targetCellMidpoints[parent] = winningMidpoint;
        }
    }
}

/**
 * An incredibly naive prefix-sum, which simply `adds' one for each non-zero log probability
 * in a target cell. Execute single-threaded.
 *
 * TODO Replace ASAP with a custom parallel prefix scan
 *
 * @param targetCellInsideProbabilities Temporary target cell probabilities. Indexed by 
 *                                      non-terminal
 * @param prefixSum                     Target `sum' array
 * @param numNonTerminals               The number of observed non-terminals in each cell
 * @param cellIndex                     Index of the target cell
 * @param n                             Size of targetCellInsideProbabilities and prefixSum
 *
 */        
__kernel void prefixSum(const __global float* targetCellInsideProbabilities,
        __global short* prefixSum,
        __global int* numNonTerminals,
        uint cellIndex,
        uint n) {

    uint sum = 0;
    int i;
    for (i = 0; i < n; i++) {
        if (targetCellInsideProbabilities[i] != -INFINITY) {
            prefixSum[i] = sum++;
        } else {
            prefixSum[i] = -1;
        }
    }
    numNonTerminals[cellIndex] = sum;
}

/*
            minLeftChildIndex[cellIndex] = tmpMinLeftChildIndex;
            maxLeftChildIndex[cellIndex] = tmpMaxLeftChildIndex;
            maxRightChildIndex[cellIndex] = tmpMaxRightChildIndex;
*/

/**
 * Packs temporary cell storage into the chart. Uses the prefix sum computed by the 
 *   prefixSum kernel.
 *
 * @param targetCellInsideProbabilities Temporary target cell storage. Indexed by 
 * @param targetCellPackedChildren      non-terminal
 * @param targetCellMidpoints
 * @param prefixSum                     `sum' array populated in 
 *
 * @param chartNonTerminalIndices       Chart storage (as described in `Data Structures'
 * @param chartInsideProbabilities      documentation above)
 * @param chartPackedChildren      
 * @param chartMidpoints
 *
 * @param cellOffset                    Offset of the target cell in the chart array
 * @param n                             Size of targetCellInsideProbabilities and prefixSum
 *
 */        
__kernel void pack(const __global float* targetCellInsideProbabilities,
        const __global int* targetCellPackedChildren,
        const __global short* targetCellMidpoints,
        const __global short* prefixSum,
        __global short* chartNonTerminalIndices,
        __global float* chartInsideProbabilities,
        __global int* chartPackedChildren,
        __global short* chartMidpoints,
        uint cellOffset,
        uint n) {

    uint threadId = get_global_id(0);
    if (threadId < n) {
        if (prefixSum[threadId] >= 0) {
            int index = prefixSum[threadId] + cellOffset;
            chartNonTerminalIndices[index] = threadId;
            chartInsideProbabilities[index] = targetCellInsideProbabilities[threadId];
            chartPackedChildren[index] = targetCellPackedChildren[threadId];
            chartMidpoints[index] = targetCellMidpoints[threadId];
        }
    }
}



/* TODO This is the tricky part of packing in parallel

  Note: isValidLeftChild consists of:
    nonTerminal >= cartesianProductFunction.leftChildStart()
       && nonTerminal < cartesianProductFunction.leftChildEnd() && nonTerminal != nullSymbol
       
    We should be able to pass in leftChildStart, leftChildEnd, 
                                 rightChildStart, rightChildEnd, and nullSymbol   

  We need to get back 
            numNonTerminals
            minLeftChildIndex
            maxLeftChildIndex
            maxRightChildIndex
            
            (we assume that minRightChildIndex == 0)

                
                    if (sparseMatrixGrammar.isValidLeftChild(nonTerminal)) {
                        if (tmpMinLeftChildIndex == Integer.MAX_VALUE) {
                            tmpMinLeftChildIndex = nonTerminalOffset;
                        }
                        tmpMaxLeftChildIndex = nonTerminalOffset;
                    }

                    if (sparseMatrixGrammar.isValidRightChild(nonTerminal)) {
                        tmpMaxRightChildIndex = nonTerminalOffset;
                    }

                    nonTerminalOffset++;
                }
            }

            numNonTerminals[cellIndex] = nonTerminalOffset - offset;
            minLeftChildIndex[cellIndex] = tmpMinLeftChildIndex;
            maxLeftChildIndex[cellIndex] = tmpMaxLeftChildIndex;
            maxRightChildIndex[cellIndex] = tmpMaxRightChildIndex;
 */
