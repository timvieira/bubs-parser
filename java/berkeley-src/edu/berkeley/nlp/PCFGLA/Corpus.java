/**
 * 
 */
package edu.berkeley.nlp.PCFGLA;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
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

    ArrayList<Tree<String>> trainTrees = new ArrayList<Tree<String>>();
    ArrayList<Tree<String>> validationTrees = new ArrayList<Tree<String>>();
    ArrayList<Tree<String>> devTestTrees = new ArrayList<Tree<String>>();
    ArrayList<Tree<String>> finalTestTrees = new ArrayList<Tree<String>>();

    /**
     * Load the WSJ, Brown, and Chinese corpora from the given locations. If either is null, don't load it. If both are
     * null, use the dummy sentence. Then, throw away all but *fraction* of the data. To train on only the part of the
     * Chinese Treebank that Levy and Manning do, use fraction=0.22225.
     */
    public Corpus(final String path, final boolean onlyTest) {
        this(path, onlyTest, -1, false);
    }

    /**
     * Load the WSJ, Brown, and Chinese corpora from the given locations. If any is null, don't load it. If all are
     * null, use the dummy sentence. Don't load the English corpora if we load the Chinese one.
     */
    public Corpus(final String path, final boolean onlyTest, final int skipSection, final boolean skipBilingual) {
        final boolean dummy = path == null;
        if (dummy) {
            System.out.println("Loading one dummy sentence into training set only.");
            Trees.PennTreeReader reader;
            Tree<String> tree;
            final int exampleNumber = 8;
            final List<String> sentences = new ArrayList<String>();
            switch (exampleNumber) {
            case 0:
                // Joshua Goodman's example
                sentences.add("((S (A x) (C x)))");
                // sentences.add("((S (A x) (C x)))");
                // sentences.add("((S (A y) (C x)))");
                sentences.add("((S (E x) (B x)))");
                // sentences.add("((S (F x) (B x)))");
                // sentences.add("((S (F x) (B x)))");
                // sentences.add("((T (E x) (C x)))");
                // sentences.add("((T (E x) (C x)))");
                break;
            case 1:
                // A single sentence
                // sentences.add("((S (UN1 (UN2 (NP (DT the) (JJ quick) (JJ brown) (NN fox)))) (VP (VBD jumped) (PP (IN over) (NP (DT the) (JJ lazy) (NN dog)))) (. .)))");
                // sentences.add("((S (NP (DT Some) (NNS traders)) (VP (VBD said) (SBAR (IN that) (S (NP (NP (DT the) (ADJP (RB closely) (VBN watched)) (NNP Majo) (NNP Market) (NNP Index)) (, ,) (SBAR (WHNP (WP$ whose) (NP (CD 20) (NNS stocks))) (S (VP (VBP mimic) (NP (DT the) (NNP Dow) (NNS industrials))))) (, ,)) (VP (VBD did) (RB n't) (VP (VB lead) (NP (NP (NN yesterday) (POS 's)) (JJ big) (NN rally))))))) (. .)))");
                // sentences.add("((S (NP (NP (JJ Influential) (NNS members)) (PP (IN of) (NP (DT the) (NNP House) (NNP Ways) (CC and) (NNP Means) (NNP Committee)))) (VP (VBD introduced) (NP (NP (NN legislation)) (SBAR (WHNP (WDT that)) (S (VP (MD would) (VP (VB restrict) (SBAR (WHADVP (WRB how)) (S (NP (DT the) (JJ new) (NN savings-and-loan) (NN bailout) (NN agency)) (VP (MD can) (VP (VB raise) (NP (NN capital)))))) (, ,) (S (VP (VBG creating) (NP (NP (DT another) (JJ potential) (NN obstacle)) (PP (TO to) (NP (NP (NP (DT the) (NN government) (POS 's)) (NN sale)) (PP (IN of)(NP (JJ sick) (NNS thrifts)))))))))))))) (. .)))");
                sentences
                        .add("((S (NP (NP (DT The) (JJ complicated) (NN language)) (PP (IN in) (NP (DT the) (JJ huge) (JJ new) (NN law)))) (VP (VBZ has) (VP (VBD muddied) (NP (DT the) (NN fight)))) (. .)))");
                // sentences.add("((S (NP (NP (DT No) (NN fiddling)) (PP (IN with) (NP (NNS systems) (CC and) (NNS procedures)))) (VP (MD will) (ADVP (RB ever)) (VP (VB prevent) (NP (NNS markets)) (PP (IN from) (S (VP (VBG suffering) (NP (NP (DT a) (NN panic) (NN wave)) (PP (IN of) (NP (NN selling))))))))) (. .)))");
                break;
            case 2:
                // On this example, Max-rule should return
                // (ROOT (S (Z4 (Z5 x) (Z6 x)) (U (A (C x) (D x)) (B (G x) (H
                // x)))))
                // While Viterbi should return
                // (ROOT (S (Z4 (Z5 x) (Z6 x)) (U (B (G x) (H x)) (B (G x) (H
                // x)))))
                sentences.add("((S (Z1 (Z2 x) (NNPS x)) (U3 (Uu (A1 (NNP x1) (NNPS x2))))))");// (B
                                                                                              // (G
                                                                                              // x)
                                                                                              // (H
                                                                                              // x))))");
                sentences.add("((S (K (U2 (Z1 (Z2 x) (NNP x)))) (U7 (NNS x))))");//
                sentences.add("((S (Z1 (NNPS x) (NN x)) (F (CC y) (ZZ z))))");//
                // sentences.add("((S (Z4 (Z5 x) (Z6 x)) (U (B (G x) (H x)) (B (G x) (H x)))))");
                // sentences.add("((S (Z7 (Z8 x) (Z9 x)) (U (B (G x) (H x)) (B (G x) (H x)))))");
                // sentences.add("((S (V (Z10 (Z11 x) (Z12 x)) (A (C x) (D x))) (Z13 (Z14 x) (Z15 x))))");
                // sentences.add("((S (V (Z16 (Z17 x) (Z18 x)) (A (C x) (D x))) (Z19 (Z20 x) (Z21 x))))");
                break;
            case 3:
                // On this example, Max-rule should return
                // (ROOT (S (A x) (B x))) until the threshold is too large
                sentences.add("((X (C (B b) (B b)) (F (E (D d)))))");
                sentences.add("((Y (C (B a) (B a)) (E (D d))))");
                sentences.add("((X (C (B b) (B b)) (E (D d))))");
                // sentences.add("((T (X t) (X t)))");
                // sentences.add("((S (X s) (X s)))");
                // sentences.add("((S (A x) (B x)))");
                break;
            case 4:
                // sentences.add("((S (NP (DT The) (NN house)) (VP (VBZ is) (ADJP (JJ green))) (. .)))");
                sentences
                        .add("( (S (SBAR (IN In) (NN order) (S (VP (TO to) (VP (VB strengthen) (NP (NP (JJ cultural) (NN exchange) (CC and) (NN contact)) (PP (IN between) (NP (NP (NP (DT the) (NNS descendents)) (PP (IN of) (NP (DT the) (NNPS Emperors)))) (UCP (PP (IN at) (NP (NN home))) (CC and) (ADVP (RB abroad)))))))))) (, ,) (NP (NNP China)) (VP (MD will) (VP (VB hold) (NP (DT the) (JJ \") (NNP China) (NNP Art) (NNP Festival) (NN \")) (PP (IN in) (NP (NP (NNP Beijing)) (CC and) (NNP Shenzhen))) (ADVP (RB simultaneously)) (PP (IN from) (NP (DT the) (NN 8th))) (PP (TO to) (NP (NP (DT the) (JJ 18th)) (PP (IN of) (NP (NNP December))))) (NP (DT this) (NN year)))) (. .)) )");
                sentences
                        .add("( (S (PP (IN In) (NP (NP (NN order) (S (VP (TO to) (VP (VB strengthen) (NP (NP (JJ cultural) (NN exchange) (CC and) (NN contact)) (PP (IN between) (NP (NP (DT the) (NNS descendents)) (PP (IN of) (NP (DT the) (NNPS Emperors))) (PP (IN at) (NP (NN home)))))))))) (CC and) (ADVP (RB abroad)))) (, ,) (NP (NNP China)) (VP (MD will) (VP (VB hold) (NP (DT the) (JJ \") (NNP China) (NNP Art) (NNP Festival) (NN \")) (PP (IN in) (NP (NP (NNP Beijing)) (CC and) (NNP Shenzhen))) (ADVP (RB simultaneously)) (PP (IN from) (NP (DT the) (NN 8th))) (PP (TO to) (NP (NP (DT the) (JJ 18th)) (PP (IN of) (NP (NNP December))))) (NP (DT this) (NN year)))) (. .)) )");
                sentences
                        .add("( (S (PP (IN In) (NP (NN order) (S (VP (TO to) (VP (VB strengthen) (NP (NP (JJ cultural) (NN exchange) (CC and) (NN contact)) (PP (IN between) (NP (NP (DT the) (NNS descendents)) (PP (IN of) (NP (DT the) (NNPS Emperors)))))) (UCP (PP (IN at) (ADVP (RB home))) (CC and) (ADVP (RB abroad)))))))) (, ,) (NP (NNP China)) (VP (MD will) (VP (VB hold) (NP (DT the) (`` \") (NNP China) (NNP Art) (NNP Festival) (NN \")) (PP (IN in) (NP (NNP Beijing) (CC and) (NNP Shenzhen))) (ADVP (RB simultaneously)) (PP (PP (IN from) (NP (DT the) (NN 8th))) (PP (IN to) (NP (DT the) (NN 18th))) (PP (IN of) (NP (NNP December)))) (NP (DT this) (NN year)))) (. .)) )");

                break;
            case 5:
                sentences.add("((X (C (B a) (B a)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (E (D d) (D d))))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (E (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                break;
            case 6:
                sentences.add("((Y (C (B @) (B b)) (E (D d) (D e))))");
                sentences.add("((Y (C (B b) (D b)) (D d)))");
                // sentences.add("((Y (C (K (B b)) (D b)) (Z (D d))))");
                // sentences.add("((Y (C (K (N n)) (B b)) (Z (D d))))");
                // sentences.add("((X (V (C (B a) (B a))) (D d)))");
                // sentences.add("((X (C (B a) (B a)) (D d)))");

                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (U (C (B b) (B b))) (D d)))");
                // sentences.add("((Y (E (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                // sentences.add("((Y (C (B b) (B b)) (Z (D d))))");
                // sentences.add("((Y (C (K (B b)) (B b)) (Z (D d))))");

                break;
            case 7:
                sentences.add("((X (S (NP (X (PRP I))) (VP like))))");
                sentences.add("((X (C (U (V (W (B a) (B a))))) (D d)))");
                sentences.add("((X (Y (Z (V (C (B a) (B a))) (D d)))))");
                sentences.add("((X (C (B a) (B a)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (E (D d) (D d))))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (U (C (B b) (B b))) (D d)))");
                sentences.add("((Y (E (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
                sentences.add("((Y (C (B b) (B b)) (D d)))");
            case 8:
                sentences
                        .add("((S-SBJ (NP (PRP We)) (VP (VBP 're) (RB about) (VP (TO to) (VP (VB see) (SBAR (IN if) (S (NP (NN advertising)) (VP (VBZ works))))))) (. .)))");
                break;
            default:

            }
            for (final String sentence : sentences) {
                reader = new Trees.PennTreeReader(new StringReader(sentence));
                tree = reader.next();
                final Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
                final Tree<String> normalizedTree = treeTransformer.transformTree(tree);
                tree = normalizedTree;
                trainTrees.add(tree);
                devTestTrees.add(tree);
                validationTrees.add(tree);
            }
        }
        // load from at least one corpus
        else {
            try {
                System.out.println("Loading data from single file!");
                loadSingleFile(path);
            } catch (final Exception e) {
                System.out.println("Error loading trees!");
                System.out.println(e.getStackTrace().toString());
                throw new Error(e.getMessage(), e);
            }
        }
    }

    private void loadSingleFile(final String path) throws Exception {
        System.out.print("Loading trees from single file...");
        final InputStreamReader inputData = new InputStreamReader(new FileInputStream(path), "UTF-8");
        final PennTreeReader treeReader = new PennTreeReader(inputData);

        while (treeReader.hasNext()) {
            trainTrees.add(treeReader.next());
        }

        final Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
        final ArrayList<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
        for (final Tree<String> tree : trainTrees) {
            final Tree<String> normalizedTree = treeTransformer.transformTree(tree);
            normalizedTreeList.add(normalizedTree);
        }
        if (normalizedTreeList.size() == 0) {
            throw new Exception("failed to load any trees at " + path);
        }
        trainTrees = normalizedTreeList;

        devTestTrees = trainTrees;
        System.out.println("done");

        // trainTrees.addAll(readTrees(path,-1,
        // Integer.MAX_VALUE,Charset.defaultCharset()));
    }

    public static List<Tree<String>> binarizeAndFilterTrees(final List<Tree<String>> trees,
            final int verticalAnnotations, final int horizontalAnnotations, final int sentenceMaxLength,
            final Binarization binarization, final boolean manualAnnotation, final boolean VERBOSE) {
        return binarizeAndFilterTrees(trees, verticalAnnotations, horizontalAnnotations, sentenceMaxLength,
                binarization, manualAnnotation, VERBOSE, false);
    }

    public static List<Tree<String>> binarizeAndFilterTrees(final List<Tree<String>> trees,
            final int verticalAnnotations, final int horizontalAnnotations, final int sentenceMaxLength,
            final Binarization binarization, final boolean manualAnnotation, final boolean VERBOSE,
            final boolean markUnaryParents) {
        final List<Tree<String>> binarizedTrees = new ArrayList<Tree<String>>();
        System.out.print("Binarizing and annotating trees...");

        if (VERBOSE)
            System.out.println("annotation levels: vertical=" + verticalAnnotations + " horizontal="
                    + horizontalAnnotations);

        for (final Tree<String> tree : trees) {
            final List<String> testSentence = tree.leafLabels();
            if (testSentence.size() > sentenceMaxLength) {
                continue;
            }
            binarizedTrees.add(TreeAnnotations.processTree(tree, verticalAnnotations, horizontalAnnotations,
                    binarization, manualAnnotation, markUnaryParents, true));
        }
        System.out.print("done.\n");
        return binarizedTrees;
    }

    /**
     * @return training trees
     */
    public List<Tree<String>> getTrainTrees() {
        return trainTrees;
    }

    /**
     * @return validation-set trees
     */
    public List<Tree<String>> getValidationTrees() {
        return validationTrees;
    }

    /**
     * @return development-set trees
     */
    public List<Tree<String>> getDevTestingTrees() {
        return devTestTrees;
    }

    /**
     * @return final test-set trees
     */
    public List<Tree<String>> getFinalTestingTrees() {
        return finalTestTrees;
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
