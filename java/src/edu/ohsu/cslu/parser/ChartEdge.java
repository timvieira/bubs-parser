package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.ArrayGrammar.Production;

public class ChartEdge {
	public Production p;
	public float insideProb;
	public ArrayChartCell leftCell, rightCell;

	public ChartEdge(final Production p, final ArrayChartCell leftCell, final ArrayChartCell rightCell, final float insideProb) {
		this.p = p;
		this.insideProb = insideProb;
		this.leftCell = leftCell;
		this.rightCell = rightCell;

		assert leftCell.end == rightCell.start;
		assert leftCell.start < rightCell.end;
	}

	public ChartEdge(final Production p, final ArrayChartCell childCell, final float insideProb) {
		this.p = p;
		this.insideProb = insideProb;
		this.leftCell = childCell;
		this.rightCell = null;
	}

	public int start() {
		return leftCell.start;
	}

	public int end() {
		if (rightCell == null) {
			return leftCell.end;
		}
		return rightCell.end;
	}

	public int midpt() throws Exception {
		if (rightCell == null) {
			throw new Exception("Midpoint do not exist for unary productions");
		}
		return leftCell.end;
	}

	@Override
	public String toString() {
		// [start,mdpt,end] A -> B C (p=-0.xxx) (e=-0.yyy)
		String start = "-", midpt = "-", end = "-", prod = "null";

		if (leftCell != null) {
			start = "" + leftCell.start;
			midpt = "" + leftCell.end;
		}
		if (rightCell != null) {
			end = "" + rightCell.end;
			midpt = "" + rightCell.start;
		}
		if (p != null) {
			prod = p.toString();
		}

		return "[" + start + "," + midpt + "," + end + "] " + prod + " inside=" + String.valueOf(insideProb) + " ";
	}

	public boolean equals(final ChartEdge otherEdge) {
		if (p != otherEdge.p)
			return false; // Equal productions should have equal pointers
		// if (!p.equals(otherEdge.p)) return false;
		// if (insideProb != otherEdge.insideProb) return false; // there are rounding problems here, but if the productions and right/left cells are equal, then the edges must be
		// equal
		if (leftCell != otherEdge.leftCell)
			return false;
		if (rightCell != otherEdge.rightCell)
			return false;

		// System.out.println(this + " == " + otherEdge);

		return true;
	}
}
