package edu.ohsu.cslu.dep;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;

/**
 * Represents a sentence with dependency arcs
 */
public class DependencyGraph implements Cloneable {

    final static Arc ROOT = new Arc("ROOT", "_", "_", 0, 0, "_");

    final Arc[] arcs;

    private Action[] derivation = null;

    public DependencyGraph(final int sentenceLength) {
        arcs = new Arc[sentenceLength + 1];
        arcs[sentenceLength] = ROOT;
    }

    /**
     * Reads a CoNLL-format dependency graph structure
     * 
     * @param reader Source of the CoNLL-format data
     * @return {@link DependencyGraph}
     * @throws IOException if the reader fails
     * @throws IllegalArgumentException if the input is malformed
     */
    public static DependencyGraph readConll(final BufferedReader reader) throws IOException {
        final ArrayList<String> lines = new ArrayList<String>();
        for (String line = reader.readLine(); line != null && !line.isEmpty(); line = reader.readLine()) {
            lines.add(line);
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
        final Action[] d = derivation();

        final LinkedList<Arc> stack = new LinkedList<Arc>();
        int step = 0;

        for (int i = 0; i < arcs.length || (stack.size() != 1 || stack.peek() != ROOT);) {
            if (stack.size() < 2) {
                stack.addFirst(arcs[i++]);
                d[step++] = Action.SHIFT;
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
    public Action[] derivation() {

        if (derivation != null) {
            return derivation;
        }

        final Action[] tmpDerivation = new Action[arcs.length * 2 - 1];
        final LinkedList<Arc> stack = new LinkedList<Arc>();
        int step = 0;

        for (int i = 0; i < arcs.length || (stack.size() != 1 || stack.peek() != ROOT);) {
            if (stack.size() < 2) {
                stack.addFirst(arcs[i++]);
                tmpDerivation[step++] = Action.SHIFT;
            } else {
                final Arc last = stack.get(0);
                final Arc previous = stack.get(1);

                // Can we reduce the top two on the stack?
                if (last.head == previous.index && last.incomingBackwardArcs == 0) {
                    // Reduce left
                    tmpDerivation[step++] = Action.REDUCE_LEFT;
                    previous.incomingBackwardArcs--;
                    stack.removeFirst();
                } else if (previous.head == last.index && previous.incomingBackwardArcs == 0) {
                    // Reduce right
                    tmpDerivation[step++] = Action.REDUCE_RIGHT;
                    final Arc tmp = stack.removeFirst();
                    stack.removeFirst();
                    stack.addFirst(tmp);
                } else {
                    if (i >= arcs.length) {
                        throw new IllegalArgumentException("Unable to derive shift-reduce derivation");
                    }
                    stack.addFirst(arcs[i++]);
                    tmpDerivation[step++] = Action.SHIFT;
                }
            }
        }

        this.derivation = tmpDerivation;
        return derivation;
    }

    public int size() {
        return arcs.length;
    }

    @Override
    public DependencyGraph clone() {
        final DependencyGraph clone = new DependencyGraph(size() - 1);
        for (int i = 0; i < arcs.length; i++) {
            clone.arcs[i] = (arcs[i].clone());
        }
        return clone;
    }

    /**
     * Outputs in CoNLL format, with or without heads
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        if (arcs[0].head >= 0) {
            // Output with heads
            for (int i = 0; i < arcs.length; i++) {
                final Arc a = arcs[i];
                if (a != ROOT) {
                    sb.append(String.format("%d\t%s\t_\t%s\t%s\t_\t%d\t%s\t_\t_\n", i + 1, a.token, a.coarsePos, a.pos,
                            a.head, a.label));
                }
            }
        } else {
            // Output without heads
            for (int i = 0; i < arcs.length; i++) {
                final Arc a = arcs[i];
                if (a != ROOT) {
                    sb.append(String.format("%d\t%s\t_\t%s\t%s\t_\t%d\n", i + 1, a.token, a.coarsePos, a.coarsePos));
                }
            }
        }
        return sb.toString();
    }

    public static enum Action {
        SHIFT, REDUCE_LEFT, REDUCE_RIGHT;

        final static Int2ObjectOpenHashMap<Action> ordinalValueMap = new Int2ObjectOpenHashMap<DependencyGraph.Action>();
        static {
            for (final Action a : EnumSet.allOf(Action.class)) {
                ordinalValueMap.put(a.ordinal(), a);
            }
        }

        public static Action forInt(final int ordinalValue) {
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
        private int incomingBackwardArcs;

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
            try {
                return (Arc) super.clone();
            } catch (final CloneNotSupportedException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public String toString() {
            return token + "(" + coarsePos + ") : " + index + " : " + head + " (" + incomingBackwardArcs + ")";
        }
    }
}