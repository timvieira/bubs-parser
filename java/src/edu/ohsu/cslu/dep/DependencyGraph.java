package edu.ohsu.cslu.dep;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;

/**
 * Represents a sentence with dependency arcs
 */
public class DependencyGraph implements Cloneable {

    final static Arc ROOT = new Arc("ROOT", "ROOT", "ROOT", 0, 0, "ROOT");
    final static String NULL = "<null>";

    final Arc[] arcs;

    private DerivationAction[] derivation = null;

    int shiftReduceClassifications = 0, correctShiftReduceClassifications = 0, reduceDirectionClassifications = 0,
            correctReduceDirectionClassifications = 0;

    public DependencyGraph(final int sentenceLength) {
        arcs = new Arc[sentenceLength + 1];
        arcs[sentenceLength] = ROOT;
    }

    /**
     * Reads a CoNLL-format dependency graph structure
     * 
     * @param reader Source of the CoNLL-format data
     * @return {@link DependencyGraph} or null if <code>reader</code> contains no more graphs
     * @throws IOException if the reader fails
     * @throws IllegalArgumentException if the input is malformed
     */
    public static DependencyGraph readConll(final BufferedReader reader) throws IOException {
        final ArrayList<String> lines = new ArrayList<String>();
        for (String line = reader.readLine(); line != null && !line.isEmpty(); line = reader.readLine()) {
            lines.add(line);
        }

        if (lines.isEmpty()) {
            return null;
        }

        final DependencyGraph g = new DependencyGraph(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final String[] split = line.split("\t");

            final int index = Integer.parseInt(split[0]);
            if (index != (i + 1)) {
                throw new IllegalArgumentException("Mismatched index. Expected " + i + " but was " + index);
            }

            final int head = Integer.parseInt(split[6]);
            if (head > lines.size()) {
                throw new IllegalArgumentException("Illegal head. Sentence length is " + lines.size() + " but was "
                        + head);
            }

            g.arcs[i] = new Arc(split[1], split[3], split[4], index, head, split[7]);
            if (head < index && head > 0) {
                g.arcs[head - 1].incomingBackwardArcs++;
            }
        }
        return g;
    }

    public static DependencyGraph readConll(final String s) throws IOException {
        return DependencyGraph.readConll(new BufferedReader(new StringReader(s)));
    }

    public void printDerivation() {
        final DerivationAction[] d = derivation();

        final LinkedList<Arc> stack = new LinkedList<Arc>();
        int step = 0;

        for (int i = 0; i < arcs.length || (stack.size() != 1 || stack.peek() != ROOT);) {
            if (stack.size() < 2) {
                stack.addFirst(arcs[i++]);
                d[step++] = DerivationAction.SHIFT;
                System.out.println("Shifting " + stack.peek());
            } else {
                final Arc last = stack.get(0);
                final Arc previous = stack.get(1);

                // Can we reduce the top two on the stack?
                if (last.head == previous.index && last.incomingBackwardArcs == 0) {
                    // Reduce left
                    System.out.println("Reducing (left) " + last + " into " + previous);
                    previous.incomingBackwardArcs--;
                    stack.removeFirst();
                } else if (previous.head == last.index && previous.incomingBackwardArcs == 0) {
                    // Reduce right
                    System.out.println("Reducing (right) " + previous + " into " + last);
                    final Arc tmp = stack.removeFirst();
                    stack.removeFirst();
                    stack.addFirst(tmp);
                } else {
                    stack.addFirst(arcs[i++]);
                    System.out.println("Shifting " + stack.peek());
                }
            }
        }
    }

    /**
     * @return Ordered shift-reduce derivation of the dependency graph
     * @throws IllegalArgumentException if the graph encodes one or more non-projective dependencies
     */
    public DerivationAction[] derivation() {

        if (derivation != null) {
            return derivation;
        }

        final DerivationAction[] tmpDerivation = new DerivationAction[arcs.length * 2 - 1];
        final LinkedList<Arc> stack = new LinkedList<Arc>();
        int step = 0;

        for (int i = 0; i < arcs.length || (stack.size() != 1 || stack.peek() != ROOT);) {
            if (stack.size() < 2) {
                stack.addFirst(arcs[i++]);
                tmpDerivation[step++] = DerivationAction.SHIFT;
            } else {
                final Arc last = stack.get(0);
                final Arc previous = stack.get(1);

                // Can we reduce the top two on the stack?
                if (last.head == previous.index && last.incomingBackwardArcs == 0) {
                    // Reduce left
                    tmpDerivation[step++] = DerivationAction.REDUCE_LEFT;
                    previous.incomingBackwardArcs--;
                    stack.removeFirst();
                } else if (previous.head == last.index && previous.incomingBackwardArcs == 0) {
                    // Reduce right
                    tmpDerivation[step++] = DerivationAction.REDUCE_RIGHT;
                    final Arc tmp = stack.removeFirst();
                    stack.removeFirst();
                    stack.addFirst(tmp);
                } else {
                    if (i >= arcs.length) {
                        throw new IllegalArgumentException("Unable to derive shift-reduce derivation");
                    }
                    stack.addFirst(arcs[i++]);
                    tmpDerivation[step++] = DerivationAction.SHIFT;
                }
            }
        }

        this.derivation = tmpDerivation;
        return derivation;
    }

    public NaryTree<String> tokenTree() {
        final NaryTree<String>[] nodes = new NaryTree[arcs.length];
        nodes[0] = new NaryTree<String>(ROOT.token);
        for (int i = 1; i < nodes.length; i++) {
            nodes[i] = new NaryTree<String>(arcs[i - 1].token);
        }

        for (int i = 1; i < nodes.length; i++) {
            final NaryTree<String> child = nodes[i];
            final NaryTree<String> parent = nodes[arcs[i - 1].head];
            parent.addChild(child);
        }

        // Return root node
        return nodes[0];
    }

    public NaryTree<DependencyNode> tree() {
        @SuppressWarnings("unchecked")
        final NaryTree<DependencyNode>[] nodes = new NaryTree[arcs.length];
        nodes[0] = new NaryTree<DependencyNode>(new DependencyNode(ROOT));
        for (int i = 1; i < nodes.length; i++) {
            nodes[i] = new NaryTree<DependencyNode>(new DependencyNode(arcs[i - 1]));
        }

        for (int i = 1; i < nodes.length; i++) {
            final NaryTree<DependencyNode> child = nodes[i];
            final NaryTree<DependencyNode> parent = nodes[arcs[i - 1].head];
            parent.addChild(child);
        }

        // Compute span, and score for each subtree
        nodes[0].label().internalSubtreeScore(nodes[0].children());
        nodes[0].label().internalSpan(nodes[0].children());

        // Compute start for each subtree
        for (final NaryTree<DependencyNode> node : nodes[0].inOrderTraversal()) {
            node.label().internalStart(node.children());
        }

        // Return root node
        return nodes[0];
    }

    public void setConfidenceScores(final float confidence) {
        for (int i = 0; i < arcs.length; i++) {
            arcs[i].score = confidence;
        }
    }

    public int size() {
        return arcs.length;
    }

    public int correctArcs() {
        int correctArcs = 0;
        for (int i = 0; i < arcs.length; i++) {
            if (arcs[i].predictedHead == arcs[i].head) {
                correctArcs++;
            }
        }
        return correctArcs;
    }

    public int correctLabels() {
        int correctLabels = 0;
        for (int i = 0; i < arcs.length; i++) {
            if (arcs[i].predictedLabel != null && arcs[i].predictedLabel.equals(arcs[i].label)) {
                correctLabels++;
            }
        }
        return correctLabels;
    }

    public DependencyGraph clear() {
        for (int i = 0; i < arcs.length; i++) {
            arcs[i].predictedHead = -1;
            arcs[i].predictedLabel = null;
            arcs[i].predictedPos = null;
        }
        shiftReduceClassifications = 0;
        correctShiftReduceClassifications = 0;
        reduceDirectionClassifications = 0;
        correctReduceDirectionClassifications = 0;
        return this;
    }

    @Override
    public DependencyGraph clone() {
        final DependencyGraph clone = new DependencyGraph(size() - 1);
        for (int i = 0; i < arcs.length; i++) {
            clone.arcs[i] = (arcs[i].clone());
        }
        return clone;
    }

    public String tokenizedSentence() {
        // Reconstruct the tokenized sentence
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arcs.length - 1; i++) {
            sb.append(arcs[i].token);
            if (i < arcs.length - 2) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    /**
     * Output in CoNLL format
     */
    public String toConllString() {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < arcs.length; i++) {
            final Arc a = arcs[i];
            if (a != ROOT) {
                sb.append(String.format("%d\t%s\t_\t%s\t%s\t_\t%d\t%s\t_\t_\n", i + 1, a.token, a.coarsePos, a.pos,
                        a.head, a.label));
            }
        }
        return sb.toString();
    }

    /**
     * Augment CoNLL format with predicted POS and heads, and (if available) head scores
     */
    public String toEnhancedConllString() {
        final StringBuilder sb = new StringBuilder();
        // Output with heads
        for (int i = 0; i < arcs.length; i++) {
            final Arc a = arcs[i];
            if (a != ROOT) {
                if (a.score != 0) {
                    sb.append(String.format("%d\t%s\t_\t%s\t%s\t_\t%d\t%s\t_\t_\t%s\t%d\t%.5f\n", i + 1, a.token,
                            a.coarsePos, a.pos, a.head, a.label, a.score, a.predictedPos, a.predictedHead));
                } else {
                    sb.append(String.format("%d\t%s\t_\t%s\t%s\t_\t%d\t%s\t_\t_\t%s\5%d\n", i + 1, a.token,
                            a.coarsePos, a.pos, a.head, a.label, a.predictedPos, a.predictedHead));
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toConllString();
    }

    public static enum DerivationAction {
        SHIFT, REDUCE_LEFT, REDUCE_RIGHT;

        final static Int2ObjectOpenHashMap<DerivationAction> ordinalValueMap = new Int2ObjectOpenHashMap<DependencyGraph.DerivationAction>();
        static {
            for (final DerivationAction a : EnumSet.allOf(DerivationAction.class)) {
                ordinalValueMap.put(a.ordinal(), a);
            }
        }

        public static DerivationAction forInt(final int ordinalValue) {
            return ordinalValueMap.get(ordinalValue);
        }
    }

    public static class Arc implements Cloneable {
        public final int index;
        public final String token;
        public final String coarsePos;
        public final String pos;
        public int head;
        public String label;
        public String predictedPos = "_";
        public int predictedHead = 0;
        public String predictedLabel;

        private int incomingBackwardArcs;
        public float score;

        public Arc(final String token, final String coarsePos, final String pos, final int index, final int head,
                final String label) {
            this.token = token;
            this.coarsePos = coarsePos;
            this.pos = pos;
            this.index = index;
            this.head = head;
            this.label = label;
        }

        @Override
        public Arc clone() {
            if (this == ROOT) {
                return this;
            }

            try {
                return (Arc) super.clone();
            } catch (final CloneNotSupportedException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String toString() {
            return String.format("%-10s\t%-4s\t%d\t%d\t%-6s\t%d\t%-6s  (%d,%.5f)", token, pos, index, head, label,
                    predictedHead, predictedLabel, incomingBackwardArcs, score);
        }
    }
}
