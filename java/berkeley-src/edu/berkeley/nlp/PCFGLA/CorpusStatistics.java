/**
 * 
 */
package edu.berkeley.nlp.PCFGLA;

import java.util.Collection;

import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Counter;
import edu.berkeley.nlp.util.Numberer;

/**
 * CorpusStatistics calculates symbol counts for a corpus.
 * 
 * @author leon
 * 
 *         TODO Collapse into GrammarTrainer
 */
public class CorpusStatistics {

    int[] counts;
    Collection<Tree<StateSet>> trees;
    Counter<UnaryRule> unaryRuleCounter;
    Counter<BinaryRule> binaryRuleCounter;

    /**
     * Count statistics for a collection of StateSet trees.
     */
    public CorpusStatistics(final Numberer tagNumberer, final Collection<Tree<StateSet>> trees) {
        counts = new int[tagNumberer.objects().size()];
        this.trees = trees;
        unaryRuleCounter = new Counter<UnaryRule>();
        binaryRuleCounter = new Counter<BinaryRule>();
    }

    private void countSymbols() {
        for (final Tree<StateSet> tree : trees) {
            addCount(tree);
        }
    }

    private void addCount(final Tree<StateSet> tree) {
        counts[tree.getLabel().getState()] += 1.0;
        if (!tree.isPreTerminal()) {
            for (final Tree<StateSet> child : tree.getChildren()) {
                addCount(child);
            }
        }
    }

    /**
     * Get the number of times each state appeared.
     * 
     * @return
     */
    public int[] getSymbolCounts() {
        countSymbols();
        return counts;
    }
}
