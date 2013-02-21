/**
 * 
 */
package edu.berkeley.nlp.PCFGLA;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.berkeley.nlp.io.PennTreebankReader;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.syntax.Trees;
import edu.berkeley.nlp.syntax.Trees.PennTreeReader;
import edu.berkeley.nlp.util.Counter;

/**
 * Class Corpus will give easy access to loading the training, validation, development testing, and testing sets from
 * both the WSJ and Brown corpora.
 * 
 * @author leon
 * 
 */
public class Corpus {

    public static boolean keepFunctionLabels;

    ArrayList<Tree<String>> trainTrees = new ArrayList<Tree<String>>();
    ArrayList<Tree<String>> validationTrees = new ArrayList<Tree<String>>();
    ArrayList<Tree<String>> devTestTrees = new ArrayList<Tree<String>>();
    ArrayList<Tree<String>> finalTestTrees = new ArrayList<Tree<String>>();

    /**
     * Load the WSJ, Brown, and Chinese corpora from the given locations. If either is null, don't load it. If both are
     * null, use the dummy sentence. Then, throw away all but *fraction* of the data. To train on only the part of the
     * Chinese Treebank that Levy and Manning do, use fraction=0.22225.
     * 
     * @param fraction The fraction of training data to use. In the range [0,1].
     */
    public Corpus(final String path, final double fraction, final boolean onlyTest) {
        this(path, fraction, onlyTest, -1, false, false);
    }

    public Corpus(final String path, final double fraction, final boolean onlyTest, final int skipSection,
            final boolean skipBilingual, final boolean keepFunctionLabels) {
        this(path, onlyTest, skipSection, skipBilingual, keepFunctionLabels);
        final int beforeSize = trainTrees.size();
        if (fraction < 0) {
            final int startIndex = (int) Math.ceil(beforeSize * -1.0 * fraction);
            trainTrees = new ArrayList<Tree<String>>(trainTrees.subList(startIndex, trainTrees.size()));
        } else if (fraction < 1) {
            final int endIndex = (int) Math.ceil(beforeSize * fraction);
            trainTrees = new ArrayList<Tree<String>>(trainTrees.subList(0, endIndex));
        }
        int nTrainingWords = 0;
        for (final Tree<String> tree : trainTrees) {
            nTrainingWords += tree.getYield().size();
        }
        System.out.println("In training set we have # of words: " + nTrainingWords);
        final int afterSize = trainTrees.size();
        System.out.println("reducing number of training trees from " + beforeSize + " to " + afterSize);
    }

    /**
     * Load the WSJ, Brown, and Chinese corpora from the given locations. If any is null, don't load it. If all are
     * null, use the dummy sentence. Don't load the English corpora if we load the Chinese one.
     */
    private Corpus(final String path, final boolean onlyTest, final int skipSection, final boolean skipBilingual,
            final boolean keepFunctionLabel) {
        final boolean dummy = path == null;
        keepFunctionLabels = keepFunctionLabel;
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
                final Trees.TreeTransformer<String> treeTransformer = (keepFunctionLabels) ? new Trees.FunctionLabelRetainingTreeNormalizer()
                        : new Trees.StandardTreeNormalizer();
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

        final Trees.TreeTransformer<String> treeTransformer = (keepFunctionLabels) ? new Trees.FunctionLabelRetainingTreeNormalizer()
                : new Trees.StandardTreeNormalizer();
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

    private void loadCONLL(final String path, final boolean useLatinEncoding) throws Exception {
        final Charset charSet = (useLatinEncoding) ? Charset.forName("ISO8859_1") : Charset.forName("UTF-8");

        System.out.print("Loading CoNLL trees...");
        trainTrees = readAndPreprocessTrees(path, 1, 1, charSet);
        validationTrees = readAndPreprocessTrees(path, 2, 2, charSet);
        devTestTrees = readAndPreprocessTrees(path, 2, 2, charSet);
        finalTestTrees = readAndPreprocessTrees(path, 3, 3, charSet);
        for (final Tree t : trainTrees) {
            if (t.getChildren().size() != 1)
                System.out.println("Malformed v: " + t);
        }
        for (final Tree t : devTestTrees) {
            if (t.getChildren().size() != 1)
                System.out.println("Malformed v: " + t);
        }
        for (final Tree t : finalTestTrees) {
            if (t.getChildren().size() != 1)
                System.out.println("Malformed t: " + t);
        }
        System.out.println("done");
    }

    /**
     * @param path
     * @param i
     * @param j
     * @param charSet
     * @return
     * @throws Exception
     */
    private ArrayList<Tree<String>> readAndPreprocessTrees(final String path, final int i, final int j,
            final Charset charSet) throws Exception {
        final List<Tree<String>> tmp = new ArrayList<Tree<String>>();
        final ArrayList<Tree<String>> tmp2 = new ArrayList<Tree<String>>();
        tmp.addAll(readTrees(path, i, j, charSet));
        for (Tree t : tmp) {
            if (!t.getLabel().equals("ROOT")) {
                final List<Tree<String>> childrenList = new ArrayList<Tree<String>>(1);
                childrenList.add(t);
                final Tree<String> rootedTree = new Tree<String>("ROOT", childrenList);
                t = rootedTree;
            }
            tmp2.add(t);
        }
        return tmp2;
    }

    public static List<Tree<String>> readTrees(final String basePath, final int low, final int high,
            final Charset charset) throws Exception {
        final Collection<Tree<String>> trees = PennTreebankReader.readTrees(basePath, low, high, charset);
        // System.out.println("in readTrees");
        // normalize trees
        final Trees.TreeTransformer<String> treeTransformer = (keepFunctionLabels) ? new Trees.FunctionLabelRetainingTreeNormalizer()
                : new Trees.StandardTreeNormalizer();
        final List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
        for (final Tree<String> tree : trees) {
            final Tree<String> normalizedTree = treeTransformer.transformTree(tree);
            normalizedTreeList.add(normalizedTree);
        }
        if (normalizedTreeList.size() == 0) {
            throw new Exception("failed to load any trees at " + basePath + " from " + low + " to " + high);
        }
        return normalizedTreeList;
    }

    /**
     * Split a set of trees into 7/10 training, 1/10 validation, 1/10 dev test, 1/10 final test sets. Every set of 10
     * sentences is split exactly into these sets deterministically.
     * 
     * @param sectionTrees
     * @param trainTrees
     * @param validationTrees
     * @param sectionTestTrees
     */
    public static void splitTrainValidTest(final List<Tree<String>> sectionTrees, final List<Tree<String>> trainTrees,
            final List<Tree<String>> validationTrees, final List<Tree<String>> devTestTrees,
            final List<Tree<String>> finalTestTrees) {
        final int CYCLE_SIZE = 10;
        for (int i = 0; i < sectionTrees.size(); i++) {
            if (i % CYCLE_SIZE < 7) {
                trainTrees.add(sectionTrees.get(i));
            } else if (i % CYCLE_SIZE == 7) {
                validationTrees.add(sectionTrees.get(i));
            } else if (i % CYCLE_SIZE == 8) {
                devTestTrees.add(sectionTrees.get(i));
            } else if (i % CYCLE_SIZE == 9) {
                finalTestTrees.add(sectionTrees.get(i));
            }
        }
    }

    public static List<Tree<String>> filterTreesForConditional(final List<Tree<String>> trees,
            final boolean filterAllUnaries, final boolean filterStupidFrickinWHNP, final boolean justCollapseUnaryChains) {
        final List<Tree<String>> filteredTrees = new ArrayList<Tree<String>>(trees.size());
        OUTER: for (final Tree<String> tree : trees) {
            if (tree.getYield().size() == 1)
                continue;
            if (tree.hasUnaryChain()) {
                if (justCollapseUnaryChains) {
                    // System.out.println(tree);
                    tree.removeUnaryChains();
                    // System.out.println(tree);
                } else
                    continue;
            }
            if (filterStupidFrickinWHNP) {
                for (final Tree<String> n : tree.getNonTerminals()) {
                    // if (n.getLabel().equals("@WHNP^g") ||
                    // (n.getLabel().equals("WHNP") && n.getChildren().size() >
                    // 1))
                    if (n.getLabel().contains("WHNP"))
                        continue OUTER;
                }
            }
            if (filterAllUnaries && tree.hasUnariesOtherThanRoot())
                continue;
            filteredTrees.add(tree);
        }
        return filteredTrees;
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

        int i = 0;
        for (final Tree<String> tree : trees) {
            final List<String> testSentence = tree.getYield();
            i++;
            if (testSentence.size() > sentenceMaxLength)
                continue;
            // if (noUnaries && tree.hasUnaryChain()) continue;
            if (true) {
                binarizedTrees.add(TreeAnnotations.processTree(tree, verticalAnnotations, horizontalAnnotations,
                        binarization, manualAnnotation, markUnaryParents, true));
            } else {
                binarizedTrees.add(TreeAnnotations.binarizeTree(tree, binarization));
            }
        }
        System.out.print("done.\n");
        return binarizedTrees;
    }

    /**
     * Get the training trees.
     * 
     * @return
     */
    public List<Tree<String>> getTrainTrees() {
        return trainTrees;
    }

    /**
     * Get the validation trees.
     * 
     * @return
     */
    public List<Tree<String>> getValidationTrees() {
        return validationTrees;
    }

    /**
     * Get the trees we test on during development.
     * 
     * @return
     */
    public List<Tree<String>> getDevTestingTrees() {
        return devTestTrees;
    }

    /**
     * Get the trees we test on for our final results.
     * 
     * @return
     */
    public List<Tree<String>> getFinalTestingTrees() {
        return finalTestTrees;
    }

    public static List<Tree<String>> makePosTrees(final List<Tree<String>> trees) {
        System.out.print("Making POS-trees...");
        final List<Tree<String>> posTrees = new ArrayList<Tree<String>>();
        for (final Tree<String> tree : trees) {
            posTrees.add(makePosTree(tree));
        }
        System.out.print(" done.\n");
        return posTrees;
    }

    public static Tree<String> makePosTree(final Tree<String> tree) {
        final List<Tree<String>> terminals = tree.getTerminals();
        final List<String> preTerminals = tree.getPreTerminalYield();

        final int n = preTerminals.size();
        String label = "STOP"; // preTerminals.get(n-1);

        List<Tree<String>> tmpChildList = new ArrayList<Tree<String>>();
        tmpChildList.add(new Tree<String>(label));// terminals.get(n-1));
        Tree<String> tmpTree = new Tree<String>(label, tmpChildList);

        // tmpChildList = new ArrayList<Tree<String>>();
        // tmpChildList.add(tmpTree);
        Tree<String> posTree = tmpTree; // new Tree<String>(label,tmpChildList);

        for (int i = n - 1; i >= 0; i--) {
            label = preTerminals.get(i);
            tmpChildList = new ArrayList<Tree<String>>();
            tmpChildList.add(terminals.get(i));
            tmpTree = new Tree<String>(label, tmpChildList);

            tmpChildList = new ArrayList<Tree<String>>();
            tmpChildList.add(tmpTree);
            tmpChildList.add(posTree);
            posTree = new Tree<String>(label, tmpChildList);// "X"
        }
        tmpChildList = new ArrayList<Tree<String>>();
        tmpChildList.add(posTree);
        posTree = new Tree<String>(tree.getLabel(), tmpChildList);
        return posTree;
    }

    public static void replaceRareWords(final StateSetTreeList trainTrees, final SimpleLexicon lexicon,
            final int threshold) {
        final Counter<String> wordCounts = new Counter<String>();
        for (final Tree<StateSet> tree : trainTrees) {
            final List<StateSet> words = tree.getYield();
            for (final StateSet word : words) {
                final String wordString = word.getWord();
                wordCounts.incrementCount(wordString, 1.0);
                lexicon.wordIndexer.add(wordString);
            }
        }
        // replace the rare words and also add the others to the appropriate
        // numberers
        for (final Tree<StateSet> tree : trainTrees) {
            final List<StateSet> words = tree.getYield();
            int ind = 0;
            for (final StateSet word : words) {
                String sig = word.getWord();
                if (wordCounts.getCount(sig) <= threshold) {
                    sig = lexicon.getSignature(word.getWord(), ind);
                    word.setWord(sig);
                }
                ind++;
            }
        }
    }

    public static void replaceRareWords(final List<Tree<String>> trainTrees, final SimpleLexicon lexicon,
            final int threshold) {
        final Counter<String> wordCounts = new Counter<String>();
        for (final Tree<String> tree : trainTrees) {
            final List<String> words = tree.getYield();
            for (final String word : words) {
                wordCounts.incrementCount(word, 1.0);
                lexicon.wordIndexer.add(word);
            }
        }
        // replace the rare words and also add the others to the appropriate
        // numberers
        for (final Tree<String> tree : trainTrees) {
            final List<Tree<String>> words = tree.getTerminals();
            int ind = 0;
            for (final Tree<String> word : words) {
                String sig = word.getLabel();
                if (wordCounts.getCount(sig) <= threshold) {
                    sig = lexicon.getSignature(word.getLabel(), ind);
                    word.setLabel(sig);
                }
                ind++;
            }
        }
    }

    public static void lowercaseWords(final List<Tree<String>> trainTrees) {
        for (final Tree<String> tree : trainTrees) {
            final List<Tree<String>> words = tree.getTerminals();
            for (final Tree<String> word : words) {
                final String lWord = word.getLabel().toLowerCase();
                word.setLabel(lWord);
            }
        }
    }

}
