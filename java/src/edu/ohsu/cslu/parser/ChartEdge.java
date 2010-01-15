package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.BaseGrammar.Production;

public class ChartEdge {
    public Production p;
    public float insideProb;
    public ChartCell leftCell, rightCell;

    public ChartEdge(final Production p, final ChartCell leftCell, final ChartCell rightCell, final float insideProb) {
        this.p = p;
        this.insideProb = insideProb;
        this.leftCell = leftCell;
        this.rightCell = rightCell;

        assert leftCell.end() == rightCell.start();
        assert leftCell.start() < rightCell.end();
    }

    public ChartEdge(final Production p, final ChartCell childCell, final float insideProb) {
        this.p = p;
        this.insideProb = insideProb;
        this.leftCell = childCell;
        this.rightCell = null;
    }

    public final int start() {
        return leftCell.start();
    }

    public final int end() {
        if (rightCell == null) {
            return leftCell.end();
        }
        return rightCell.end();
    }

    public final int midpt() throws Exception {
        if (rightCell == null) {
            throw new Exception("Midpoint do not exist for unary productions");
        }
        return leftCell.end();
    }

    @Override
    public String toString() {
        // [start,mdpt,end] A -> B C (p=-0.xxx) (e=-0.yyy)
        String start = "-", midpt = "-", end = "-", prod = "null";

        if (leftCell != null) {
            start = "" + leftCell.start();
            midpt = "" + leftCell.end();
        }
        if (rightCell != null) {
            end = "" + rightCell.end();
            midpt = "" + rightCell.start();
        }
        if (p != null) {
            prod = p.toString();
        }

        return "[" + start + "," + midpt + "," + end + "] " + prod + " inside=" + String.valueOf(insideProb) + " ";
    }

    @Override
    public boolean equals(final Object other) {
        try {
            if (this == other) {
                return true;
            }

            if (other == null) {
                return false;
            }

            final ChartEdge otherEdge = (ChartEdge) other;
            if (p == null && otherEdge.p != null) {
                return false;
            }

            if (!p.equals(otherEdge.p)) {
                return false;
            }
            // if (!p.equals(otherEdge.p)) return false;
            // if (insideProb != otherEdge.insideProb) return false; // there are rounding problems here, but
            // if
            // the productions and right/left cells are equal, then the edges must be equal
            if (!leftCell.equals(otherEdge.leftCell)) {
                return false;
            }
            if ((rightCell == null && otherEdge.rightCell != null)) {
                return false;
            }
            if (rightCell != null && !rightCell.equals(otherEdge.rightCell)) {
                return false;
            }
            // System.out.println(this + " == " + otherEdge);

            return true;
        } catch (final ClassCastException e) {
            return false;
        }
    }
}
