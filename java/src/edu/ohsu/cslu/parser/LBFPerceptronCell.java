package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Vector;

import edu.ohsu.cslu.classifier.Perceptron;
import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.grammar.Grammar.Production;
import edu.ohsu.cslu.parser.cellselector.CSLUTBlockedCells;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;

public class LBFPerceptronCell extends LocalBestFirstChartParser {

    private CSLUTBlockedCells cslutScores;
    private Vector<Float> cslutStartScore, cslutEndScore;
    Perceptron perceptron = null;

    public LBFPerceptronCell(final LeftHashGrammar grammar, final EdgeSelector edgeSelector, final CellSelector cellSelector, final CSLUTBlockedCells cslutScores) {
        super(grammar, edgeSelector, cellSelector);
        this.cslutScores = cslutScores;
    }

    public void train(final BufferedReader inStream) throws Exception {
        LinkedList<ChartEdge> goldEdgeList;
        String line, sentence;
        ParseTree tree;
        ChartCell cell;

        // save input data so we can iterate over it multiple times
        final List<String> inputData = new LinkedList<String>();
        while ((line = inStream.readLine()) != null) {
            inputData.add(line.trim());
        }

        // read in gold tree
        for (int ittr = 0; ittr < ParserDriver.param2; ittr++) {
            System.out.println(" == ittr " + ittr + " ==");
            for (final String bracketString : inputData) {
                tree = ParseTree.readBracketFormat(bracketString);
                if (tree.isBinaryTree() == false) {
                    Log.info(0, "ERROR: Training trees must be binarized exactly as used in decoding");
                    System.exit(1);
                }

                sentence = ParserUtil.join(tree.getLeafNodesContent(), " ");
                System.out.println("SENT: " + sentence);
                this.currentSentence = sentence;

                assert cslutScores.allStartScore.containsKey(sentence);
                cslutStartScore = cslutScores.allStartScore.get(sentence);
                cslutEndScore = cslutScores.allEndScore.get(sentence);

                tree.tokenizeLeaves(grammar);
                final Chart goldChart = tree.convertToChart(grammar);

                if (goldChart != null) {

                    final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);
                    initParser(sent.length);
                    addLexicalProductions(sent);
                    addUnaryExtensionsToLexProds();
                    cellSelector.init(this);

                    if (perceptron == null) {
                        // run this to figure out how many features the current implementation is using,
                        // which will also create a new Perceptron model
                        extractFeatures(chart.getCell(0, 1), new PriorityQueue<ChartEdge>());
                    }

                    while (cellSelector.hasNext() && !hasCompleteParse()) {
                        cell = cellSelector.next();
                        goldEdgeList = new LinkedList<ChartEdge>();
                        for (final ChartEdge goldEdge : goldChart.getCell(cell.start(), cell.end()).getEdges()) {
                            if (goldEdge.prod.isLexProd() == false) {
                                goldEdgeList.add(goldEdge);
                            }
                        }

                        if (cell.width() > 1) {
                            trainVisitCell(cell, goldEdgeList);
                        }
                    }
                }
            }
            perceptron.decreaseLearningRate();
            System.out.println(perceptron.toString());
        }
    }

    public void addUnaryExtensionsToLexProds() {
        for (int i = 0; i < chart.size(); i++) {
            final ChartCell cell = chart.getCell(i, i + 1);
            for (final int pos : cell.getPosEntries()) {
                for (final Production unaryProd : grammar.getUnaryProductionsWithChild(pos)) {
                    cell.addEdge(unaryProd, cell, null, cell.getBestEdge(pos).inside + unaryProd.prob);
                }
            }
        }
    }

    private void trainVisitCell(final ChartCell cell, final LinkedList<ChartEdge> goldEdges) {
        final ChartCell ChartCell = cell;
        final int start = ChartCell.start();
        final int end = ChartCell.end();
        final int spanLength = end - start;
        Collection<Production> possibleProds;
        ChartEdge edge;
        final ChartEdge[] bestEdges = new ChartEdge[grammar.numNonTerms()];

        // System.out.println(" visitCell: " + cell);
        // for (final ChartEdge goldEdge : goldEdges) {
        // System.out.println("   goldEdge: " + goldEdge);
        // }

        if (spanLength == 1) {
            for (final int pos : ChartCell.getPosEntries()) {
                for (final Production p : grammar.getUnaryProductionsWithChild(pos)) {
                    final float prob = p.prob + ChartCell.getBestEdge(pos).inside;
                    edge = new ChartEdge(p, ChartCell, prob, edgeSelector);
                    addEdgeToArray(edge, bestEdges);
                }
            }
        } else {
            // add binary edges
            for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
                final ChartCell leftCell = chart.getCell(start, mid);
                final ChartCell rightCell = chart.getCell(mid, end);
                for (final ChartEdge leftEdge : leftCell.getBestLeftEdges()) {
                    for (final ChartEdge rightEdge : rightCell.getBestRightEdges()) {
                        possibleProds = grammar.getBinaryProductionsWithChildren(leftEdge.prod.parent, rightEdge.prod.parent);
                        if (possibleProds != null) {
                            for (final Production p : possibleProds) {
                                final float prob = p.prob + leftEdge.inside + rightEdge.inside;
                                edge = new ChartEdge(p, leftCell, rightCell, prob, edgeSelector);
                                addEdgeToArray(edge, bestEdges);
                            }
                        }
                    }
                }
            }
        }

        addBestEdgesToChart(ChartCell, bestEdges, goldEdges);
    }

    private void addEdgeToArray(final ChartEdge edge, final ChartEdge[] bestEdges) {
        final int parent = edge.prod.parent;
        if (bestEdges[parent] == null || edge.fom > bestEdges[parent].fom) {
            bestEdges[parent] = edge;
        }
    }

    private void addEdgeToAgenda(final ChartEdge edge, final PriorityQueue<ChartEdge> agenda) {
        agenda.add(edge);
        nAgendaPush++;
    }

    private void addBestEdgesToChart(final ChartCell cell, final ChartEdge[] bestEdges, final LinkedList<ChartEdge> goldEdges) {
        ChartEdge edge, unaryEdge;
        int numEdgesAdded = 0, maxEdgesAdded;
        boolean addedEdge, addedGoldEdges = false;

        final PriorityQueue<ChartEdge> agenda = new PriorityQueue<ChartEdge>();
        for (int i = 0; i < bestEdges.length; i++) {
            if (bestEdges[i] != null) {
                addEdgeToAgenda(bestEdges[i], agenda);
            }
        }

        final float[] spanFeatures = extractFeatures(cell, agenda);
        final int guessNumPops = (int) perceptron.score(spanFeatures);
        // guessNumPops = 10;
        int popsToGold = 0;
        if (goldEdges.size() == 0) {
            maxEdgesAdded = (int) ParserDriver.param1;
        } else {
            maxEdgesAdded = 99999;
        }

        // while (goldEdges.size() > 0) {
        // while (!agenda.isEmpty() && numEdgesAdded < maxEdgesAdded && !addedGoldEdges) {
        while (!agenda.isEmpty()) {
            edge = agenda.poll();
            ChartEdge found = null;
            for (final ChartEdge goldEdge : goldEdges) {
                if (goldEdge.prod.parent == edge.prod.parent) {
                    found = goldEdge;
                }
            }
            if (found != null) {
                goldEdges.remove(found);
                addedGoldEdges = (goldEdges.size() == 0);
                popsToGold = numEdgesAdded + 1;
            }
            // if (goldEdges.contains(edge)) {
            // goldEdges.remove(edge);
            // }
            addedEdge = cell.addEdge(edge);
            // goldNumPops++;
            if (addedEdge) {
                numEdgesAdded++;
                // Add unary productions to agenda so they can compete with binary productions
                for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
                    unaryEdge = new ChartEdge(p, cell, p.prob + edge.inside, edgeSelector);
                    addEdgeToAgenda(unaryEdge, agenda);
                }
            }
        }

        correctModel(spanFeatures, popsToGold, guessNumPops);
    }

    private void correctModel(final float[] spanFeatures, final int goldNumPops, final int guessNumPops) {

        // if (goldNumPops == 0) {
        // return;
        // }

        System.out.println(" #gold=" + goldNumPops + " \t#guess=" + guessNumPops);
        System.out.println(" train: " + goldNumPops + " " + floatArrayToString(spanFeatures));
        // System.out.println("   feats:" + floatArrayToString(spanFeatures));

        if (goldNumPops == guessNumPops) {
            return;
        }

        // L1 loss function in one direction plus and strong penalty in the other
        // final float loss = java.lang.Math.abs(guessNumPops - goldNumPops);
        // // if (guessNumPops < goldNumPops) {
        // // under shot it. Really bad because gold edge isn't added
        // // loss = loss + 10;
        // // }
        //
        // final float[] gradient = new float[spanFeatures.length];
        // for (int i = 0; i < spanFeatures.length; i++) {
        // gradient[i] = (float) (spanFeatures[i] * 0.001);
        // }
        // perceptron.learnOnline(gradient);

        // System.out.println(" weights: " + perceptron.toString());

    }

    public String floatArrayToString(final float[] x) {
        String s = "";
        for (final float f : x) {
            s += f + " ";
        }
        return s;
    }

    private float[] extractFeatures(final ChartCell span, final PriorityQueue<ChartEdge> agenda) {
        // final LinkedList<Boolean> featList = new LinkedList<Boolean>();

        // // bias
        // featList.add(true);
        //
        // // is base cell
        // if (span.width() == 1) {
        // featList.add(true);
        // } else {
        // featList.add(false);
        // }
        //
        // // top cell
        // featList.add(span == this.rootChartCell);
        //
        // // span width
        // featList.addAll(ParserUtil.binValue(span.width(), 1, 25, 25));
        //
        // // pct span width
        // final float pctSpanOfWidth = span.width() / (float) this.chartSize;
        // featList.addAll(ParserUtil.binValue(pctSpanOfWidth, 0, 1, 21));
        //
        // // // count possible midpoints exist which should predict how fruitful the cell is
        // // int numMidpts = 0;
        // // for (int i = span.start() + 1; i < span.end(); i++) {
        // // if (hasBeenPopped[span.start()][i] && hasBeenPopped[i][span.end()]) {
        // // numMidpts++;
        // // }
        // // }
        // // featList.addAll(binValue(numMidpts, 0, 25, 26));
        // //
        // // // pct midpoints
        // // final float pctMidptsOfSpan = numMidpts / (float) (span.width() - 1);
        // // featList.addAll(binValue(pctMidptsOfSpan, 0, 1, 21));
        //
        // // CSLUT scores
        // final float startScore = cslutStartScore.get(span.start());
        // featList.addAll(ParserUtil.binValue(startScore, -500, 500, 101));
        //
        // final float endScore = cslutEndScore.get(span.end() - 1); // -1 because it's s word index, not a span
        // featList.addAll(ParserUtil.binValue(endScore, -500, 500, 101));
        //
        // if (perceptron == null) {
        // perceptron = new Perceptron(featList.size());
        // } else if (featList.size() != perceptron.numFeatures()) {
        // Log.info(0, "ERROR: len(featureList)=" + featList.size() + " but number features in model files is numFeats=" + numFeats);
        // System.exit(1);
        // }

        // System.out.println(" numFeats=" + numFeats + " featList=" + boolListToString(featList));

        final LinkedList<Float> featList = new LinkedList<Float>();

        // bias
        featList.add((float) 1);

        if (span.width() == 1) {
            featList.add((float) 1);
        } else {
            featList.add((float) 0);
        }

        featList.add((float) span.width());
        featList.add((float) chart.size());
        featList.add(span.width() / (float) chart.size());
        featList.add((float) agenda.size());
        // PriorityQueue<ChartEdge> agendaClone = agenda.clone();
        // LinkedList<ChartEdge> tmpEdges = new LinkedList<ChartEdge>();
        // int numEdges=0;
        // float bestFOM = Float.NEGATIVE_INFINITY;
        // while (numEdges < 10 && !agenda.isEmpty()) {
        // ChartEdge edge = agenda.poll();
        // tmpEdges.add(edge);
        // if (numEdges==0) {
        // bestFOM = edge.figureOfMerit;
        // } else {
        // featList.add(bestFOM - edge.figureOfMerit);
        // }
        // numEdges++;
        // }
        // for (ChartEdge edge : tmpEdges) {
        // agenda.add(edge);
        // }
        // for (int i=0; i<numEdges; i++) {
        // featList.add((float)0);
        // }

        final float startScore = cslutStartScore.get(span.start());
        final float endScore = cslutEndScore.get(span.end() - 1);
        featList.add(startScore);
        featList.add(endScore);
        featList.add(startScore + endScore);

        if (perceptron == null) {
            perceptron = new Perceptron(featList.size());
        } else if (featList.size() != perceptron.numFeatures()) {
            Log.info(0, "ERROR: len(featureList)=" + featList.size() + " but number features in model files is numFeats=" + perceptron.numFeatures());
            System.exit(1);
        }

        final float[] feats = new float[perceptron.numFeatures()];
        int i = 0;
        for (final float value : featList) {
            feats[i] = value;
            i++;
        }
        return feats;
    }
}
