/**
 * OpenCL kernels for OpenClSparseMatrixVectorParser
 */

__kernel void BinarySpmvMultiply(const __global int* binaryRuleMatrixRowIndices,
		const __global int* binaryRuleMatrixColumnIndices,
		const __global float* binaryRuleMatrixProbabilities,
		uint n,
		const __global float* crossProductProbabilities,
		const __global short* crossProductMidpoints,
		__global int* chartCellChildren,
		__global float* chartCellProbabilities,
		__global short* chartCellMidpoints) {

	uint threadId = get_global_id(0);
//	uint blockId = get_global_size(0);
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
