package edu.ohsu.cslu.grammar;

public class NonTerminal {
    private String label;
    private boolean isPOS, isFactored, isLeftChild, isRightChild;

    public NonTerminal(final String label) {
        this.label = label;
        isPOS = false;
        isFactored = false;
        isLeftChild = false;
        isRightChild = false;
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
