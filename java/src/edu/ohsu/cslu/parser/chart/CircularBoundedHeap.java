package edu.ohsu.cslu.parser.chart;

import java.util.PriorityQueue;

import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

public class CircularBoundedHeap extends PriorityQueue<ChartEdge> {
    int arraySize, maxHeapSize, startIndex = 0, endIndex = 0;
    ChartEdge[] heap;

    public CircularBoundedHeap(final int maxSize) {
        maxHeapSize = maxSize;
        arraySize = maxSize + 5;
        heap = new ChartEdge[arraySize];
    }

    @Override
    public int size() {
        if (startIndex <= endIndex) {
            return endIndex - startIndex;
        }
        return arraySize - (startIndex - endIndex);
    }

    @Override
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

    @Override
    public boolean add(final ChartEdge edge) {
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

            // System.out.println("PUSH: s=" + startIndex + " e=" + endIndex + " size=" + size() + " i=" +
            // newEdgeIndex + " edge=" + edge);
            return true;
        }
        return false;
    }

    @Override
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