package edu.ohsu.cslu.tools;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.Tokenizer;

/**
 * Applies text normalizations to a treebank
 * 
 * @author Aaron Dunlop
 * @since Dec 4, 2012
 */
public class Normalize extends BaseCommandlineTool {

    @Option(name = "-cd", metaVar = "count", usage = "Normalize cardinal numbers (children of CD) which occur n or fewer times")
    private int cdThreshold;

    @Option(name = "-nnp", metaVar = "count", usage = "Normalize children of NNP nodes which occur n or fewer times")
    private int nnpThreshold;

    @Override
    protected void run() throws Exception {

        // Read the entire corpus and count token occurrences
        final BufferedReader br = inputAsBufferedReader();
        // Allow re-reading up to 25 MB
        br.mark(25 * 1024 * 10240);

        final Object2IntOpenHashMap<String> lexicon = new Object2IntOpenHashMap<String>();
        for (final String line : inputLines(br)) {
            final NaryTree<String> tree = NaryTree.read(line, String.class);
            for (final NaryTree<String> leafNode : tree.leafTraversal()) {
                lexicon.add(key(leafNode), 1);
            }
        }

        // Reset the reader and reread the corpus, this time applying appropriate normalizations and outputting each
        // tree
        br.reset();

        for (final String line : inputLines(br)) {
            final NaryTree<String> tree = NaryTree.read(line, String.class);

            for (final NaryTree<String> node : tree.inOrderTraversal()) {

                // Normalize cardinal numbers
                if (node.isLeaf() && node.parent().label().equals("CD") && lexicon.getInt(key(node)) <= cdThreshold) {
                    node.setLabel(Tokenizer.berkeleyGetSignature(node.label(), false, null));
                }

                // Normalize proper nouns (NNP)
                if (node.isLeaf() && node.parent().label().equals("NNP") && lexicon.getInt(key(node)) <= nnpThreshold) {
                    node.setLabel(Tokenizer.berkeleyGetSignature(node.label(), false, null));
                }
            }

            System.out.println(tree.toString());
        }
    }

    private static String key(final NaryTree<String> leafNode) {
        return leafNode.parentLabel() + "|" + leafNode.label();
    }

    public static void main(final String[] args) {
        run(args);
    }
}
