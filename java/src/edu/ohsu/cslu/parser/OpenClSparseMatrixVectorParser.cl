/**
 * OpenCL kernels for OpenClSparseMatrixVectorParser
 *
 * Note: All kernels start with something like 'if threadId < n' so that we can start 
 *       thread blocks of an even multiple the warp size even if the data set is 
 *       not an even multiple (the last few threads will just exit)
 */

/*
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


/**
 * Computes the cartesian product of validLeftChildren x validRightChildren
 * Stores the populated child pairs in crossProductProbabilities and crossProductMidpoints
 *   (indexed by child pair)
 *  
 */
__kernel void crossProduct(const __global int* validLeftChildren,
        const __global float* validLeftChildrenProbabilities,
        uint numValidLeftChildren,
        const __global short* validRightChildren,
        const __global float* validRightChildrenProbabilities,
        uint numValidRightChildren,
        __global float* crossProductProbabilities,
        __global short* crossProductMidpoints,
        ushort midpoint) {

    uint threadId = get_global_id(0);

	// Launch one thread for each combination (numValidLeftChildren * numValidRightChildren)
    if (threadId < (numValidLeftChildren * numValidRightChildren)) {
		int leftChildIndex = threadId / numValidRightChildren;
		int rightChildIndex = threadId % numValidRightChildren;
		 
		float jointProbability = validLeftChildrenProbabilities[leftChildIndex] + validRightChildrenProbabilities[rightChildIndex];
		int child = ((validLeftChildren[leftChildIndex] << LEFT_CHILD_SHIFT) | (validRightChildren[rightChildIndex] & MASK));
		crossProductProbabilities[child] = jointProbability;
		crossProductMidpoints[child] = midpoint;
    }
}

/*
 * Merges one cross-product vector into another, choosing the maximum probability midpoint for each 
 *   non-terminal pair. Warning: This union is destructive, overwriting the initial contents of 
 *   crossProductProbabilities0 and crossProductMidpoints0
 */
__kernel void crossProductUnion(__global float* crossProductProbabilities0,
        __global short* crossProductMidpoints0,
        const __global float* crossProductProbabilities1,
        const __global short* crossProductMidpoints1,
        uint size) {

    uint threadId = get_global_id(0);

    if (threadId < size) {
        float probability0 = crossProductProbabilities0[threadId];
        float probability1 = crossProductProbabilities1[threadId];
        
        if (probability1 > probability0) {
            crossProductProbabilities0[threadId] = probability1;
            crossProductMidpoints0[threadId] = crossProductMidpoints1[threadId];
        }
    }
}

__kernel void binarySpmvMultiply(const __global int* binaryRuleMatrixRowIndices,
		const __global int* binaryRuleMatrixColumnIndices,
		const __global float* binaryRuleMatrixProbabilities,
		uint n,
		const __global float* crossProductProbabilities,
		const __global short* crossProductMidpoints,
		__global int* chartCellChildren,
		__global float* chartCellProbabilities,
		__global short* chartCellMidpoints) {

	uint threadId = get_global_id(0);
	uint parent = threadId;

	if (parent < n) {
		// Production winningProduction = null;
		float winningProbability = -INFINITY;
		int winningChildren = 0;
		short winningMidpoint = 0;

		// Iterate over possible children of the parent (columns with non-zero entries)
		for (int i = binaryRuleMatrixRowIndices[parent]; i < binaryRuleMatrixRowIndices[parent + 1]; i++) {
			int grammarChildren = binaryRuleMatrixColumnIndices[i];
			float grammarProbability = binaryRuleMatrixProbabilities[i];

			float crossProductProbability = crossProductProbabilities[grammarChildren];
			float jointProbability = grammarProbability + crossProductProbability;

			if (jointProbability > winningProbability) {
				winningProbability = jointProbability;
				winningChildren = grammarChildren;
				winningMidpoint = crossProductMidpoints[grammarChildren];
			}
		}

		chartCellProbabilities[parent] = winningProbability;
		if (winningProbability != -INFINITY) {
			chartCellChildren[parent] = winningChildren;
			chartCellMidpoints[parent] = winningMidpoint;
		}
	}
}

__kernel void unarySpmvMultiply(const __global int* unaryRuleMatrixRowIndices,
        const __global int* unaryRuleMatrixColumnIndices,
        const __global float* unaryRuleMatrixProbabilities,
        uint n,
        __global int* chartCellChildren,
        __global float* chartCellProbabilities,
        __global short* chartCellMidpoints,
        short chartCellEnd) {

    uint threadId = get_global_id(0);
    uint parent = threadId;

    if (parent < n) {
        float winningProbability = chartCellProbabilities[parent];
        int winningChildren = INT_MIN;
        short winningMidpoint = 0;

        // Iterate over possible children of the parent (columns with non-zero entries)
        for (int i = unaryRuleMatrixRowIndices[parent]; i < unaryRuleMatrixRowIndices[parent + 1]; i++) {
            int grammarChildren = unaryRuleMatrixColumnIndices[i];
            int child = (grammarChildren >> LEFT_CHILD_SHIFT);
            float grammarProbability = unaryRuleMatrixProbabilities[i];

            float jointProbability = grammarProbability + chartCellProbabilities[child];

            if (jointProbability > winningProbability) {
                winningProbability = jointProbability;
                winningChildren = grammarChildren;
                winningMidpoint = chartCellEnd;
            }
        }

        if (winningChildren != INT_MIN) {
            chartCellChildren[parent] = winningChildren;
            chartCellProbabilities[parent] = winningProbability;
            chartCellMidpoints[parent] = winningMidpoint;

// TODO: Count valid left and right children (in shared memory?)
//       See http://www.khronos.org/message_boards/viewtopic.php?f=37&t=2207
//
//            if (currentProbability == -INFINITY) {
//                if (csrSparseMatrixGrammar.isValidLeftChild(parent)) {
//                    chartCell.numValidLeftChildren++;
//                }
//                if (csrSparseMatrixGrammar.isValidRightChild(parent)) {
//                    chartCell.numValidRightChildren++;
//                }
//            }
        }
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
