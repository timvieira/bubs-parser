/**
 * 
 */
package edu.berkeley.nlp.PCFGLA;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.syntax.Trees;
import edu.berkeley.nlp.syntax.Trees.PennTreeReader;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;

/**
 * Class Corpus will give easy access to loading the training, validation, development testing, and testing sets from
 * both the WSJ and Brown corpora.
 * 
 * @author leon
 * 
 */
public class Corpus {

    private final ArrayList<Tree<String>> trees = new ArrayList<Tree<String>>();

    /**
     * Load the a parsed corpus from the specified <code>inputStream</code>. Assumes UTF-8 encoding
     * 
     * @param inputStream
     * @throws IllegalArgumentException if the training corpus is empty
     */
    public Corpus(final InputStream inputStream) {

        try {
            final PennTreeReader treeReader = new PennTreeReader(new InputStreamReader(inputStream, "UTF-8"));

            final Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
            while (treeReader.hasNext()) {
                trees.add(treeTransformer.transformTree(treeReader.next()));
            }

            if (trees.size() == 0) {
                throw new IllegalArgumentException("Empty training corpus");
            }

        } catch (final UnsupportedEncodingException e) {
            // We always use UTF-8, so this should never happen
            throw new IllegalArgumentException(e);
        }
    }

    public static List<Tree<String>> binarizeAndFilterTrees(final List<Tree<String>> trees,
            final int horizontalAnnotations, final int sentenceMaxLength, final Binarization binarization) {
        final List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();

        for (final Tree<String> tree : trees) {
            final List<String> testSentence = tree.leafLabels();
            if (testSentence.size() > sentenceMaxLength) {
                continue;
            }
            binarizedTrees.add(TreeAnnotations.forgetLabels(TreeAnnotations.binarizeTree(tree, binarization),
                    horizontalAnnotations));
        }
        return binarizedTrees;
    }

    /**
     * @return trees
     */
    public List<Tree<String>> trees() {
        return trees;
    }

    public static void lowercaseWords(final List<Tree<String>> trainTrees) {
        for (final Tree<String> tree : trainTrees) {
            final List<Tree<String>> words = tree.leafList();
            for (final Tree<String> word : words) {
                final String lWord = word.label().toLowerCase();
                word.setLabel(lWord);
            }
        }
    }

}
