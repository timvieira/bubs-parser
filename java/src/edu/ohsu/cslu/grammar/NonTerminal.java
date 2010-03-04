package edu.ohsu.cslu.grammar;

public class NonTerminal {
    public String label;
    public boolean isPOS, isFactored, isLeftChild, isRightChild;

    public NonTerminal(final String label, final boolean isPOS, final boolean isFactored, final boolean isLeftChild, final boolean isRightChild) {
        this.label = label;
        this.isPOS = isPOS;
        this.isFactored = isFactored;
        this.isLeftChild = isLeftChild;
        this.isRightChild = isRightChild;
    }

    public NonTerminal(final String label) {
        this(label, false, false, false, false);
    }

    public String getLabel() {
        return label;
    }

    public boolean isPOS() {
        return isPOS;
    }

    public boolean isWordLevel() {
        return isPOS;
    }

    public boolean isClauseLevel() {
        return !isPOS;
    }

    public boolean isFactored() {
        return isFactored;
    }

    public boolean isLeftChild() {
        return isLeftChild;
    }

    public boolean isRightChild() {
        return isRightChild;
    }
}
