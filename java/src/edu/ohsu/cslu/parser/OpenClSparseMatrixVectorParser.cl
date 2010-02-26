/**
 * OpenCL kernels for OpenClSparseMatrixVectorParser
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
    int leftChildIndex = threadId / numValidRightChildren;
    int rightChildIndex = threadId % numValidRightChildren;
     
    float jointProbability = validLeftChildrenProbabilities[leftChildIndex] + validRightChildrenProbabilities[rightChildIndex];
    int child = ((validLeftChildren[leftChildIndex] << LEFT_CHILD_SHIFT) | (validRightChildren[rightChildIndex] & MASK));
    crossProductProbabilities[child] = jointProbability;
    crossProductMidpoints[child] = midpoint;
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

	float probability0 = crossProductProbabilities0[threadId];
	float probability1 = crossProductProbabilities1[threadId];
	
	if (probability1 > probability0) {
		crossProductProbabilities0[threadId] = probability1;
		crossProductMidpoints0[threadId] = crossProductMidpoints1[threadId];
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
//        if (currentProbability == -INFINITY) {
//            if (csrSparseMatrixGrammar.isValidLeftChild(parent)) {
//                chartCell.numValidLeftChildren++;
//            }
//            if (csrSparseMatrixGrammar.isValidRightChild(parent)) {
//                chartCell.numValidRightChildren++;
//            }
//        }
    }
}
