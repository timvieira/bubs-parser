package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.chart.CellChart.ChartCell;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

public class LBFBoundedHeap extends LBFPruneViterbi {

    public LBFBoundedHeap(final ParserOptions opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected void addEdgeCollectionToChart(final ChartCell cell) {
        ChartEdge edge;
        ChartEdge unaryEdge;
        boolean edgeBelowThresh = false;
        int numAdded = 0;
        // final BoundedHeap boundedHeap = new BoundedHeap(maxEdgesToAdd, 3);
        final CircularBoundedHeap boundedHeap = new CircularBoundedHeap(maxEdgesToAdd);

        for (int i = 0; i < grammar.numNonTerms(); i++) {
            if (bestEdges[i] != null) {
                boundedHeap.add(bestEdges[i]);
            }
        }

        while (boundedHeap.isEmpty() == false && numAdded <= maxEdgesToAdd && !edgeBelowThresh) {
            edge = boundedHeap.poll();
            if (edge.fom < bestFOM - logBeamDeltaThresh) {
                edgeBelowThresh = true;
            } else if (edge.inside() > cell.getInside(edge.prod.parent)) {
                cell.updateInside(edge);
                numAdded++;

                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    unaryEdge = chart.new ChartEdge(p, cell);
                    if ((bestEdges[p.parent] == null || bestEdges[p.parent].fom < unaryEdge.fom) && unaryEdge.fom > bestFOM - logBeamDeltaThresh) {
                        boundedHeap.add(unaryEdge);
                    }
                }
            }
        }
    }

    protected class CircularBoundedHeap {
        ChartEdge[] heap;
        int startIndex, endIndex, maxHeapSize, arraySize;

        public CircularBoundedHeap(final int maxHeapSize) {
            this.maxHeapSize = maxHeapSize;
            arraySize = maxHeapSize + 5;
            heap = new ChartEdge[arraySize];
            startIndex = 0;
            endIndex = 0;
        }

        public int size() {
            if (startIndex <= endIndex) {
                return endIndex - startIndex;
            }
            return arraySize - (startIndex - endIndex);
        }

        public boolean isEmpty() {
            return startIndex == endIndex;
        }

        private int nextIndex(final int i) {
            return (i + 1) % arraySize;
        }

        private int prevIndex(final int i) {
            // return (i - 1 + maxHeapSize) % maxHeapSize;
            if (i == 0)
                return arraySize - 1;
            return i - 1;
        }

        public void add(final ChartEdge edge) {
            if (size() < maxHeapSize || edge.fom > heap[prevIndex(endIndex)].fom) {
                if (size() < maxHeapSize) {
                    endIndex = nextIndex(endIndex);
                }
                // replace worst edge with current edge
                heap[prevIndex(endIndex)] = edge;

                // move current edge up the heap until it is in it's sorted order
                ChartEdge tmpEdge;
                int newEdgeIndex = prevIndex(endIndex), nextEdge = prevIndex(newEdgeIndex);
                while (newEdgeIndex != startIndex && heap[newEdgeIndex].fom > heap[nextEdge].fom) {
                    // swap heap[newEdgeIndex], heap[nextEdge]
                    tmpEdge = heap[nextEdge];
                    heap[nextEdge] = heap[newEdgeIndex];
                    heap[newEdgeIndex] = tmpEdge;
                    newEdgeIndex = nextEdge;
                    nextEdge = prevIndex(newEdgeIndex);
                }

                // System.out.println("PUSH: s=" + startIndex + " e=" + endIndex + " size=" + size() + " i=" + newEdgeIndex + " edge=" + edge);
            }
        }

        public ChartEdge poll() {
            if (isEmpty()) {
                return null;
            }
            final ChartEdge edge = heap[startIndex];
            startIndex = nextIndex(startIndex);
            // System.out.println("POLL: " + edge);
            return edge;
        }
    }

    protected class BoundedHeap {
        ChartEdge[] heap;
        int startIndex, endIndex, maxHeapSize;

        // items are kept in an array where the 'heap' is represented as the sorted
        // elements between startIndex and endIndex in the array. If all items are
        // added first, and then polled second, the BoundedHeap will be time and memory
        // efficient. But if items are added and polled in an arbitrary order, the
        // sorted heap portion of the array (between startIndex and endIndex) will start
        // to shift towards the end of the array. This prevents moving the sorted elements
        // to the top of the array when an item is polled.
        // As a result, the actual array size should be larger than the maxHeapSize if the
        // user plans on using the BoundedHeap in this fashion.
        public BoundedHeap(final int maxSize, final int memBufferFactor) {
            maxHeapSize = maxSize;
            heap = new ChartEdge[maxHeapSize * memBufferFactor];
            startIndex = 0;
            endIndex = 0;
        }

        public int size() {
            return endIndex - startIndex;
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public void add(final ChartEdge edge) {
            if (size() < maxHeapSize || edge.fom > heap[endIndex - 1].fom) {
                if (size() < maxHeapSize) {
                    endIndex += 1;
                    assert endIndex < heap.length;
                }
                heap[endIndex - 1] = edge;

                ChartEdge tmpEdge;
                int i = endIndex - 2;
                while (i >= startIndex && heap[i].fom < heap[i + 1].fom) {
                    // swap heap[i], heap[i+1]
                    tmpEdge = heap[i];
                    heap[i] = heap[i + 1];
                    heap[i + 1] = tmpEdge;
                    i--;
                }
            }
        }

        public ChartEdge poll() {
            if (isEmpty()) {
                return null;
            }
            startIndex += 1;
            return heap[startIndex - 1];
        }
    }
}
