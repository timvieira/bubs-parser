package edu.ohsu.cslu.parser;

import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;

public class ChartEdge implements Comparable<ChartEdge> {
    public Production prod;
    public float fom; // figure of merit
    public float inside;

    // TODO: should we create a getLeftCell() and getRightCell(). We'd need to keep a chart pointer...
    public ChartCell leftCell, rightCell;

    // binary production w/ FOM
    public ChartEdge(final Production prod, final ChartCell leftCell, final ChartCell rightCell, final float inside, final EdgeSelector edgeSelector) {
        this(prod, leftCell, rightCell, inside);
        this.fom = edgeSelector.calcFOM(this);
    }

    // binary production
    public ChartEdge(final Production prod, final ChartCell leftCell, final ChartCell rightCell, final float inside) {
        this.prod = prod;
        this.inside = inside;
        this.leftCell = leftCell;
        this.rightCell = rightCell;

        assert leftCell.end() == rightCell.start();
        assert leftCell.start() < rightCell.end();
    }

    // unary production w/ FOM
    public ChartEdge(final Production prod, final ChartCell childCell, final float inside, final EdgeSelector edgeSelector) {
        this(prod, childCell, inside);
        this.fom = edgeSelector.calcFOM(this);
    }

    // unary production
    public ChartEdge(final Production prod, final ChartCell childCell, final float inside) {
        this.prod = prod;
        this.inside = inside;
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
            if (leftCell == null) {
                throw new Exception("right/leftCell must be set to use start(), end(), and midpt()");
            }
            throw new Exception("Unary productions do not have midpoints");
        }
        return leftCell.end();
    }

    public ChartEdge copy() {
        return new ChartEdge(this.prod, this.leftCell, this.rightCell, this.inside);
    }

    @Override
    public int compareTo(final ChartEdge otherEdge) {
        if (this.equals(otherEdge)) {
            return 0;
        } else if (fom > otherEdge.fom) {
            return -1;
        } else {
            return 1;
        }
    }

    public int spanLength() {
        return end() - start();
    }

    @Override
    public String toString() {
        // [start,mdpt,end] A -> B C (p=-0.xxx) (e=-0.yyy)
        String start = "-", midpt = "-", end = "-", prodStr = "null";

        if (leftCell != null) {
            start = "" + leftCell.start();
            if (rightCell != null) {
                midpt = "" + leftCell.end();
            } else {
                end = "" + leftCell.end();
            }
        }
        if (rightCell != null) {
            end = "" + rightCell.end();
            assert leftCell.end() == rightCell.start();
            // midpt = "" + rightCell.start();
        }
        if (prod != null) {
            prodStr = prod.toString();
        }

        return String.format("[%s,%s,%s] %s inside=%f fom=%f", start, midpt, end, prodStr, inside, fom);
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
            if (prod == null && otherEdge.prod != null) {
                return false;
            }

            if (!prod.equals(otherEdge.prod)) {
                return false;
            }

            // not comparing left/right cell object pointers because I want to be able to compare
            // cells from different charts
            if (start() != otherEdge.start()) {
                return false;
            }

            if (end() != otherEdge.end()) {
                return false;
            }

            if (prod.isBinaryProd() && (midpt() != otherEdge.midpt())) {
                return false;
            }

            return true;
        } catch (final Exception e) {
            return false;
        }
    }
}
