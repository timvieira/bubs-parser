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

    @Option(name = "-t", required = true, metaVar = "tag", separator = ",", usage = "Tag(s) to normalize (replace with their unknown-word class)")
    private OpenClassTags[] tags;

    @Option(name = "-th", required = true, metaVar = "count", separator = ",", usage = "Normalization threshold(s) - observation count in the corpus")
    private int[] thresholds;

    private Object2IntOpenHashMap<String> thresholdMap = new Object2IntOpenHashMap<String>();

    @Override
    protected void setup() throws Exception {
        if (tags.length != thresholds.length) {
            throw new IllegalArgumentException("The number of specified thresholds (" + thresholds.length
                    + ") does not match the number of specified tags (" + tags.length + ")");
        }
        for (int i = 0; i < tags.length; i++) {
            thresholdMap.put(tags[i].name(), thresholds[i]);
        }
    }

    @Override
    protected void run() throws Exception {

        // Read the entire corpus and count token occurrences
        final BufferedReader br = inputAsBufferedReader();
        // Allow re-reading up to 50 MB
        br.mark(50 * 1024 * 1024);

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

                if (node.isLeaf() && thresholdMap.containsKey(node.parentLabel())
                        && lexicon.getInt(key(node)) <= thresholdMap.getInt(node.parentLabel())) {
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

    public static enum OpenClassTags {
        CD, JJ, NN, NNP, NNPS, NNS, RB, VB, VBD, VBG, VBN, VBP, VBZ;
    }
}
