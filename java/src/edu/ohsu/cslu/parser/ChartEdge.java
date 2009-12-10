package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar.Production;

public class ChartEdge implements Comparable<ChartEdge> {
    public Production p;
    public double insideProb;
    public ChartCell leftCell, rightCell;

    public ChartEdge(Production p, double insideProb, ChartCell leftCell, ChartCell rightCell) {
        this.p = p;
        this.insideProb = insideProb;
        this.leftCell = leftCell;
        this.rightCell = rightCell;

        assert leftCell.end == rightCell.start;
        assert leftCell.start < rightCell.end;
    }
    
    public ChartEdge(Production p, double insideProb, ChartCell childCell) {
    	this.p = p;
    	this.insideProb = insideProb;
    	this.leftCell = childCell;
    	this.rightCell = null;
    }

    public String toString() {
        // [start,mdpt,end] A -> B C (p=-0.xxx) (e=-0.yyy)
    	String start="-", midpt="-", end="-",prod="null";
    	
    	if (leftCell != null) { start=""+leftCell.start; midpt=""+leftCell.end; }
    	if (rightCell != null) { end=""+rightCell.end; midpt=""+rightCell.start; }
    	if (p != null) { prod = p.toString(); }
    	
    	return "["+start+","+midpt+","+end+"] "+prod+" (e="+String.valueOf(insideProb)+") ";
    }

	@Override
	public int compareTo(ChartEdge otherEdge) {
		if (this.equals(otherEdge)) {
			return 0;
		} else if (insideProb > otherEdge.insideProb) {
			return -1;
		} else {
			return 1;
		} 
	}
	
	public boolean equals(ChartEdge otherEdge) {
		if (p != otherEdge.p) return false; // Equal productions should have equal pointers
		//if (!p.equals(otherEdge.p)) return false;
		//if (insideProb != otherEdge.insideProb) return false; // there are rounding problems here, but if the productions and right/left cells are equal, then the edges must be equal
		if (leftCell != otherEdge.leftCell) return false;
		if (rightCell != otherEdge.rightCell) return false;
		
		//System.out.println(this + " == " + otherEdge);
	
		return true;
	}
}
