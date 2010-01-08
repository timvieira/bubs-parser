package edu.ohsu.cslu.parser;

import java.util.Arrays;
import java.util.LinkedList;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.Production;

public class ChartCell {

	public int start, end, numEdgesAdded, numEdgesConsidered;
	public ChartEdge[] bestEdge;
	private LinkedList<ChartEdge> bestLeftEdges, bestRightEdges;
	boolean bestEdgesHaveChanged = true;
	Grammar grammar;

	public ChartCell(final int start, final int end, final Grammar grammar) {
		this.start = start;
		this.end = end;
		this.numEdgesAdded = 0;
		this.numEdgesConsidered = 0;
		this.grammar = grammar;

		bestEdge = new ChartEdge[grammar.numNonTerms()];
		Arrays.fill(bestEdge, null);
	}

	public ChartEdge getBestEdge(final int nonTermIndex) {
		return bestEdge[nonTermIndex];
	}

	public LinkedList<ChartEdge> getBestLeftEdges() {
		buildLeftRightEdgeLists();
		return bestLeftEdges;
	}

	public LinkedList<ChartEdge> getBestRightEdges() {
		buildLeftRightEdgeLists();
		return bestRightEdges;
	}

	private void buildLeftRightEdgeLists() {
		ChartEdge tmpEdge;
		if (bestEdgesHaveChanged) {
			bestLeftEdges = new LinkedList<ChartEdge>();
			bestRightEdges = new LinkedList<ChartEdge>();
			for (int i = 0; i < bestEdge.length; i++) {
				tmpEdge = bestEdge[i];
				if (tmpEdge != null) {
					if (grammar.isLeftChild(tmpEdge.p.parent))
						bestLeftEdges.add(tmpEdge);
					if (grammar.isRightChild(tmpEdge.p.parent))
						bestRightEdges.add(tmpEdge);
				}
			}
			bestEdgesHaveChanged = false;
		}
	}

	public boolean addEdge(final ChartEdge edge) {
		final int parent = edge.p.parent;
		numEdgesConsidered += 1;
		// System.out.println("Considering: " + edge);
		if (bestEdge[parent] == null || edge.insideProb > bestEdge[parent].insideProb) {
			bestEdge[parent] = edge;
			bestEdgesHaveChanged = true;
			numEdgesAdded += 1;
			return true;
		}

		return false;
	}

	// alternate addEdge function so we aren't required to create a new ChartEdge object
	// in the CYK inner loop for every potential new edge entry
	public boolean addEdge(final Production p, final float insideProb, final ChartCell leftCell, final ChartCell rightCell) {
		numEdgesConsidered += 1;
		// System.out.println("Considering: " + new ChartEdge(p, leftCell, rightCell, insideProb));

		final ChartEdge prevBestEdge = bestEdge[p.parent];
		if (prevBestEdge == null) {
			bestEdge[p.parent] = new ChartEdge(p, leftCell, rightCell, insideProb);
			bestEdgesHaveChanged = true;
			numEdgesAdded += 1;
			return true;
		} else if (prevBestEdge.insideProb < insideProb) {
			prevBestEdge.p = p;
			prevBestEdge.insideProb = insideProb;
			prevBestEdge.leftCell = leftCell;
			prevBestEdge.rightCell = rightCell;
			// bestLeftEdgesHasChanged = true; // pointer to old edge will still be correct
			numEdgesAdded += 1;
			return true;
		}

		return false;
	}

	public int getNumEdgesAdded() {
		return numEdgesAdded;
	}

	public int getNumEdgeEntries() {
		int numEntries = 0;
		for (int i = 0; i < bestEdge.length; i++) {
			if (bestEdge[i] != null)
				numEntries++;
		}
		return numEntries;
	}

	@Override
	public String toString() {
		return "ChartCell[" + start + "][" + end + "] with " + getNumEdgeEntries() + " (of " + grammar.numNonTerms() + ") edges";
	}
}
