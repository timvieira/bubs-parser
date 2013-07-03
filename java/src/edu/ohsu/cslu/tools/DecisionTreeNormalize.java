package edu.ohsu.cslu.tools;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;

/**
 * Applies decision-tree text normalizations to a treebank (using {@link DecisionTreeTokenClassifier}).
 * 
 * @author Aaron Dunlop
 */
public class DecisionTreeNormalize extends BaseTextNormalizationTool {

    @Override
    protected void run() throws Exception {

        // Read the entire corpus and count token occurrences
        final BufferedReader br = inputAsBufferedReader();
        // Allow re-reading up to 50 MB
        br.mark(50 * 1024 * 1024);

        final Object2IntOpenHashMap<String> lexicon = countTokenOccurrences(br);

        // Reset the reader and reread the corpus, this time applying appropriate normalizations and outputting each
        // tree
        br.reset();

        for (final String line : inputLines(br)) {
            final NaryTree<String> tree = NaryTree.read(line, String.class);

            for (final NaryTree<String> node : tree.inOrderTraversal()) {

                if (node.isLeaf() && thresholdMap.containsKey(node.parentLabel())
                        && lexicon.getInt(key(node)) <= thresholdMap.getInt(node.parentLabel())) {
                    node.setLabel(DecisionTreeTokenClassifier.berkeleyGetSignature(node.label(), false, null));
                }
            }

            System.out.println(tree.toString());
        }
    }

    public static void main(final String[] args) {
        run(args);
    }
}
