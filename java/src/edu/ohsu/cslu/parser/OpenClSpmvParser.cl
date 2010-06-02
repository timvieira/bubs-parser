/**
 * OpenCL kernels for OpenClSpmvParser.java
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
 * The parse chart is represented as three parallel arrays, each segmented into cells 
 *   by uniform offsets. e.g., cell 0 = indices 0..1024, cell 1 = indices 25..2048, etc.
 *
 * -- float* chartInsideProbabilities: Probabilities of each non-terminal (NEGATIVE_INFINITY for unobserved non-terminals)
 * -- int* chartPackedChildren: Child pairs producing each non-terminal --- equivalent to the grammar rule
 * -- short* chartMidpoints: Midpoints at which the probability and child pair were found. Used to back-trace and construct the parse tree
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
 * Computes the cartesian product of validLeftChildren x validRightChildren
 * Stores the populated child pairs in cartesianProductProbabilities and cartesianProductMidpoints
 *   (indexed by child pair)
 *
 * Launch 1 thread for each possible child pair (leftChildEnd - leftChildStart) * (rightChildEnd - rightChildStart) 
 * TODO Is it better to launch O(n^2) threads (approximately 1 million) or to iterate over subsets (left children perhaps)
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

/*
 * Merges one cross-product vector into another, choosing the maximum probability midpoint for each 
 *   non-terminal pair. Warning: This union is destructive, overwriting the initial contents of 
 *   cartesianProductProbabilities0 and cartesianProductMidpoints0
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

__kernel void binarySpmvMultiply(__global float* chartInsideProbabilities,
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
		int winningChildren = 0;
		short winningMidpoint = 0;

		// Iterate over possible children of the parent (columns with non-zero entries)
		for (int i = binaryRuleMatrixRowIndices[parent]; i < binaryRuleMatrixRowIndices[parent + 1]; i++) {
			int grammarChildren = binaryRuleMatrixColumnIndices[i];
			float grammarProbability = binaryRuleMatrixProbabilities[i];

			float cartesianProductProbability = cartesianProductProbabilities[grammarChildren];
			float jointProbability = grammarProbability + cartesianProductProbability;

			if (jointProbability > winningProbability) {
				winningProbability = jointProbability;
				winningChildren = grammarChildren;
				winningMidpoint = cartesianProductMidpoints[grammarChildren];
			}
		}

        int index = targetCellOffset + parent;
        
		chartInsideProbabilities[index] = winningProbability;
		if (winningProbability != -INFINITY) {
			chartPackedChildren[index] = winningChildren;
			chartMidpoints[index] = winningMidpoint;
		}
	}
}

__kernel void unarySpmvMultiply(__global float* chartInsideProbabilities,
        __global int* chartPackedChildren,
        __global short* chartMidpoints,
        const uint targetCellOffset,
        const __global int* unaryRuleMatrixRowIndices,
        const __global int* unaryRuleMatrixColumnIndices,
        const __global float* unaryRuleMatrixProbabilities,
        uint unaryRuleMatrixRows,
        short chartCellEnd) {

    uint threadId = get_global_id(0);
    uint parent = threadId;

    if (parent < unaryRuleMatrixRows) {
        int index = targetCellOffset + parent;
        float winningProbability = chartInsideProbabilities[index];
        int winningChildren = INT_MIN;
        short winningMidpoint = 0;

        // Iterate over possible children of the parent (columns with non-zero entries)
        for (int i = unaryRuleMatrixRowIndices[parent]; i < unaryRuleMatrixRowIndices[parent + 1]; i++) {
            int grammarChildren = unaryRuleMatrixColumnIndices[i];
            int leftChild = unpackLeftChild(grammarChildren);
            float grammarProbability = unaryRuleMatrixProbabilities[i];

            float jointProbability = grammarProbability + chartInsideProbabilities[targetCellOffset + leftChild];

            if (jointProbability > winningProbability) {
                winningProbability = jointProbability;
                winningChildren = grammarChildren;
                winningMidpoint = chartCellEnd;
            }
        }

        if (winningChildren != INT_MIN) {
            chartInsideProbabilities[index] = winningProbability;
            chartPackedChildren[index] = winningChildren;
            chartMidpoints[index] = winningMidpoint;
        }


/*
 TODO: Count valid left and right children (in shared memory?) and collapse down to just observed non-terminals

   --Write a flag array as well as our probability array
   --Perform prefix sum (scan) operation (producing s, below)

     i 0   1   2   3   4   5   6   7   8   9   10 
     p -  .5   -   -  .7   -  .9   2   -   1   -
  flag 0   1   0   0   1   0   1   1   0   1   0
     s 0   1   1   1   2   2   3   4   4   5   5

   Allocate an array the size of s[|V|] (5)
       

   Populate with a variant of the following kernel (from NVIDIA Marching Cubes example). 

 compact voxel array
__global__ void
compactVoxels(uint *compactedVoxelArray, uint *voxelOccupied, uint *voxelOccupiedScan, uint numVoxels)
{
    uint blockId = __mul24(blockIdx.y, gridDim.x) + blockIdx.x;
    uint i = __mul24(blockId, blockDim.x) + threadIdx.x;

    if (voxelOccupied[i] && (i < numVoxels)) {
        compactedVoxelArray[ voxelOccupiedScan[i] ] = i;
    }
}

            if (currentProbability == -INFINITY) {
                if (csrSparseMatrixGrammar.isValidLeftChild(parent)) {
                    chartCell.numValidLeftChildren++;
                }
                if (csrSparseMatrixGrammar.isValidRightChild(parent)) {
                    chartCell.numValidRightChildren++;
                }
            }
 */

    }
}

// TODO: It seems like there should be a built-in function to do this.
//       Maybe vector operations would be better?
__kernel void fillFloat(__global float* buffer,
        uint size,
        float value) {

    uint threadId = get_global_id(0);

    if (threadId < size) {
        buffer[threadId] = value;
    }
}

int unpackLeftChild(const int childPair) {
    if (childPair < 0) {
        // Unary or lexical production
        if (childPair <= MAX_PACKED_LEXICAL_PRODUCTION) {
            // Lexical production
            return -childPair + MAX_PACKED_LEXICAL_PRODUCTION;
        }
        // Unary production
        return -childPair - 1;
    }
    
    // Left child of binary production
    return childPair >> PACKING_SHIFT;
}

/*

Another possibile algorithm:

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


