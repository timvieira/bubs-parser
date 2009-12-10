package edu.ohsu.cslu.parser.util;

import java.util.LinkedList;

import edu.ohsu.cslu.parser.ChartEdge;


public class ParseTree {

    public ChartEdge chartEdge;
    public LinkedList<ParseTree> children;

    public ParseTree(ChartEdge node) {
        this.chartEdge = node;
        children = new LinkedList<ParseTree>();
    }

    public String toString() { return this.toString(false); }
    
    public String toString(boolean printInsideProb) {
        String s;
        
        // leaf nodes have to be lexical productions
        if (children.size() == 0) {
            s = chartEdge.p.childrenToString();
        } else {
            s = "(" + chartEdge.p.parentToString();
            //if (printInsideProb == true) s += " " + String.format("%f", chartEdge.insideProb);
            if (printInsideProb == true) s += " " + Double.toString(chartEdge.insideProb);
            for (ParseTree child : children) {
                s += " " + child.toString(printInsideProb);
            }
            s += ")";
        }

        return s;
    }
}
