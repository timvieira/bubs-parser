package edu.berkeley.nlp.PCFGLA;


/**
 * Parent class for unary and binary rules.
 * 
 * @author Dan Klein
 */
public class Rule implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public short parentState = -1;

    public short getParentState() {
        return parentState;
    }

    public boolean isUnary() {
        return false;
    }
}
