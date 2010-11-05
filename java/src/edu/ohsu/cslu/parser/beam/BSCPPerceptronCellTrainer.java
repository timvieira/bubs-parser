package edu.ohsu.cslu.parser.beam;

import edu.ohsu.cslu.grammar.LeftHashGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.PerceptronCellSelector;
import edu.ohsu.cslu.parser.chart.EdgeCellChart;

public class BSCPPerceptronCellTrainer extends BeamSearchChartParser<LeftHashGrammar, EdgeCellChart> {

    PerceptronCellSelector perceptronCellSelector;

    public BSCPPerceptronCellTrainer(final ParserDriver opts, final LeftHashGrammar grammar) {
        super(opts, grammar);
        perceptronCellSelector = (PerceptronCellSelector) cellSelector;
    }

    // public void addUnaryExtensionsToLexProds(final EdgeCellChart goldChart) {
    // ChartCell cell;
    //
    // for (int i = 0; i < chart.size(); i++) {
    // cell = (ChartCell) chart.getCell(i, i + 1);
    // for (final int pos : cell.getPosNTs()) {
    // for (final Production unaryProd : grammar.getUnaryProductionsWithChild(pos)) {
    // cell.addEdge(unaryProd, cell, null, cell.getBestEdge(pos).inside + unaryProd.prob);
    // }
    // }
    //
    // for (final ChartEdge goldEdge : goldChart.getCell(i, i + 1).getEdges()) {
    // cell.addEdgeForceOverwrite(goldEdge);
    // }
    // }
    // }
    //
    // public void train(final BufferedReader inStream) throws Exception {
    // String line, sentence;
    // ParseTree tree;
    // ChartCell guessSpan;
    // ChartCell goldSpan;
    // LinkedList<ChartEdge> goldEdgeList;
    //
    // // perceptronCellSelector.initWeights();
    //
    // final List<String> inputData = new LinkedList<String>();
    // while ((line = inStream.readLine()) != null) {
    // inputData.add(line.trim());
    // }
    //
    // // read in gold tree
    // for (int ittr = 0; ittr < ParserOptions.param2; ittr++) {
    // int totalCells = 0, correctCells = 0;
    // System.out.println(" == ittr " + ittr + " ==");
    // for (final String bracketString : inputData) {
    // tree = ParseTree.readBracketFormat(bracketString);
    // if (tree.isBinaryTree() == false) {
    // Log.info(0, "ERROR: Training trees must be binarized exactly as used in decoding");
    // System.exit(1);
    // }
    //
    // sentence = ParserUtil.join(tree.getLeafNodesContent(), " ");
    // System.out.println("SENT: " + sentence);
    // this.currentSentence = sentence;
    // tree.tokenizeLeaves(grammar);
    // final EdgeCellChart goldChart = (EdgeCellChart) tree.convertToChart(grammar);
    //
    // if (goldChart != null) {
    //
    // final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);
    // initParser(sent.length);
    // addLexicalProductions(sent);
    // addUnaryExtensionsToLexProds(goldChart);
    // perceptronCellSelector.init(this);
    //
    // // goldEdgeList = getGoldEdgeList(goldChart);
    // goldEdgeList = getGoldBinaryEdgeList(goldChart);
    // while (goldEdgeList.size() > 0) {
    //
    // guessSpan = perceptronCellSelector.nextWithGoldCorrection(goldEdgeList);
    //
    // // collect gold edges in guess span (if any)
    // // final Collection<ChartEdge> goldEdgeListForGuessSpan = goldChart.getChartCell(guessSpan.start(),
    // guessSpan.end()).getEdges();
    //
    // // LinkedList<ChartEdge> goldEdgeListForGuessSpan = new LinkedList<ChartEdge>();
    //
    // // is guess span in gold span frontier?
    // boolean guessSpanInGoldSpanFrontier = false;
    // for (final ChartEdge goldEdge : goldEdgeList) {
    // goldSpan = (ChartCell) chart.getCell(goldEdge.start(), goldEdge.end());
    // if (goldSpan == guessSpan) {
    // guessSpanInGoldSpanFrontier = true;
    // // for (final ChartEdge cellGoldEdge : goldChart.getChartCell(goldEdge.start(), goldEdge.end()).getEdges()) {
    // // if (cellGoldEdge.p.isLexProd() == false) {
    // // goldEdgeListForGuessSpan.add(cellGoldEdge);
    // // System.out.println("goldEdgeForSpan: " + cellGoldEdge);
    // // }
    // // }
    // }
    // }
    //
    // LinkedList<ChartEdge> goldEdgeListForGuessSpan = (LinkedList<ChartEdge>) goldChart.getCell(guessSpan.start(),
    // guessSpan.end()).getEdges();
    //
    // boolean guessSpanAlreadyHasGoldEdges = false;
    // if (goldEdgeListForGuessSpan.size() > 0) {
    // guessSpanAlreadyHasGoldEdges = true;
    // for (final ChartEdge goldEdge : goldEdgeListForGuessSpan) {
    // guessSpanAlreadyHasGoldEdges = guessSpanAlreadyHasGoldEdges & chart.getCell(guessSpan.start(),
    // guessSpan.end()).hasEdge(goldEdge);
    // // System.out.println("hasGold? " + goldEdge + " chart.hasEdge=" +
    // chart[guessSpan.start()][guessSpan.end()].hasEdge(goldEdge)
    // // + " alreadyHasGoldEdges=" + alreadyHasGoldEdges);
    // }
    // }
    //
    // if (!guessSpanInGoldSpanFrontier && !guessSpanAlreadyHasGoldEdges) {
    // goldEdgeListForGuessSpan = new LinkedList<ChartEdge>();
    // }
    //
    // // boolean alreadyHasGoldEdges = false;
    // // for (final ChartEdge goldEdge : goldChart.getChartCell(guessSpan.start(), guessSpan.end()).getEdges()) {
    // // if (chart[guessSpan.start()][guessSpan.end()].hasEdge(goldEdge)) {
    // // alreadyHasGoldEdges = true;
    // // }
    // // }
    //
    // // if (alreadyHasGoldEdges == false) {
    // visitCell(guessSpan, goldEdgeListForGuessSpan);
    // // }
    // // goldEdgeList = getGoldEdgeList(goldChart);
    // goldEdgeList = getGoldBinaryEdgeList(goldChart);
    // }
    //
    // totalCells += perceptronCellSelector.numTotal;
    // correctCells += perceptronCellSelector.numCorrect;
    //
    // System.out.println(" sentLen=" + sent.length + " nGuessSpans=" + perceptronCellSelector.numTotal +
    // " nCorrectSpans=" + perceptronCellSelector.numCorrect
    // + " pct=" + ((float) perceptronCellSelector.numCorrect / perceptronCellSelector.numTotal));
    // }
    // }
    // perceptronCellSelector.decreaseLearningRate();
    // System.out.println("TOTAL: total=" + totalCells + " correct=" + correctCells + " pct=" + ((float) correctCells /
    // totalCells));
    // // perceptronCellSelector.writeModel(new BufferedWriter(new OutputStreamWriter(System.out)));
    // System.out.println(perceptronCellSelector.toString());
    // }
    // }
    //
    // public void train_02_06(final BufferedReader inStream) throws Exception {
    // String line, sentence;
    // ParseTree tree;
    // ChartCell guessSpan;
    // ChartCell goldSpan;
    // LinkedList<ChartEdge> goldEdgeList;
    //
    // perceptronCellSelector.initWeights();
    //
    // final List<String> inputData = new LinkedList<String>();
    // while ((line = inStream.readLine()) != null) {
    // inputData.add(line.trim());
    // }
    //
    // // read in gold tree
    // // while ((line = inStream.readLine()) != null) {
    // for (int ittr = 0; ittr < ParserOptions.param2; ittr++) {
    // int totalCells = 0, correctCells = 0;
    // System.out.println(" == ittr " + ittr + " ==");
    // for (final String bracketString : inputData) {
    // // line = inStream.readLine();
    // // for (int x = 0; x < 100; x++) {
    // // tree = ParseTree.readBracketFormat(line);
    // tree = ParseTree.readBracketFormat(bracketString);
    // if (tree.isBinaryTree() == false) {
    // Log.info(0, "ERROR: Training trees must be binarized exactly as used in decoding");
    // System.exit(1);
    // }
    //
    // sentence = ParserUtil.join(tree.getLeafNodesContent(), " ");
    // // System.out.println(sentence);
    // this.currentSentence = sentence;
    // final boolean canBuildGoldEdge = true;
    // tree.tokenizeLeaves(grammar);
    // final EdgeCellChart goldChart = tree.convertToChart(grammar);
    //
    // if (goldChart != null) {
    //
    // final int sent[] = grammar.tokenizer.tokenizeToIndex(sentence);
    // initParser(sent.length);
    // addLexicalProductions(sent);
    // // addUnaryExtensionsToLexProds();
    // perceptronCellSelector.init(this);
    //
    // // while (hasCompleteParse() == false && perceptronCellSelector.hasNext() && canBuildGoldEdge == true) {
    // goldEdgeList = getGoldEdgeList(goldChart);
    // while (goldEdgeList.size() > 0) {
    // // goldEdge = getGoldEdge(goldChart);
    // // goldEdgeList = getGoldEdgeList(goldChart);
    // // goldEdgeList = getBuildableGoldEdgeList(goldChart);
    //
    // // if (goldEdge == null) {
    // // if (goldEdgeList.size() == 0) {
    // // Log.info(0, "WARNING: no gold edge found");
    // // canBuildGoldEdge = false;
    // // } else {
    // // goldSpan = chart[goldEdge.start()][goldEdge.end()];
    // // guessSpan = perceptronCellSelector.nextWithGoldCorrection((ChartCell) goldSpan);
    //
    // // goldEdge = perceptronCellSelector.nextWithGoldCorrection(goldEdgeList);
    // // goldSpan = chart[goldEdge.start()][goldEdge.end()];
    //
    // guessSpan = perceptronCellSelector.nextWithGoldCorrection(goldEdgeList);
    //
    // // visitCell(goldSpan, goldEdge);
    // // visitCell(goldSpan, goldEdgeListInSpan);
    // // final Collection<ChartEdge> goldEdgeListInSpan = goldChart.getChartCell(goldEdge.start(),
    // goldEdge.end()).getEdges();
    //
    // // final Collection<ChartEdge> allGoldEdgesInGuessSpan = goldChart.getChartCell(guessSpan.start(),
    // guessSpan.end()).getEdges();
    // // final Collection<ChartEdge> goldEdgesInGuessSpan = new LinkedList<ChartEdge>();
    // // we have already put all the lexical edges in the chart, so these don't count
    // // for (final ChartEdge edge : allGoldEdgesInGuessSpan) {
    // // if (edge.p.isLexProd() == false) {
    // // goldEdgesInGuessSpan.add(edge);
    // // }
    // // }
    //
    // // we can't just take the gold edges from the guess span because they might not
    // // be buildable yet according to the gold parse
    // // boolean guessSpanInGoldBuildableSpans = false;
    //
    // // boolean builtGoldEdgeList = false;
    // final LinkedList<ChartEdge> goldEdgeListForGuessSpan = new LinkedList<ChartEdge>();
    // for (final ChartEdge goldEdge : goldEdgeList) {
    // goldSpan = chart.getCell(goldEdge.start(), goldEdge.end());
    // if (goldSpan == guessSpan) {
    // // goldEdgeListForGuessSpan.add(goldEdge);
    // for (final ChartEdge cellGoldEdge : goldChart.getCell(goldEdge.start(), goldEdge.end()).getEdges()) {
    // if (cellGoldEdge.prod.isLexProd() == false) {
    // goldEdgeListForGuessSpan.add(cellGoldEdge);
    // }
    // }
    // }
    // }
    //
    // // final boolean isBaseCell = ((goldEdge.end() - goldEdge.start()) == 1);
    // // should only be triggered once for each cell because there can only be one
    // // binary gold prod for each cell. wait. what about multiple unary chains in a basecell!
    // // if (goldSpan == guessSpan && builtGoldEdgeList == false && (isBaseCell || goldEdge.p.isBinaryProd())) {
    // // use a flag since if there are two gold spans, we don't want to double up the edges
    // // guessSpanInGoldBuildableSpans = true;
    // // builtGoldEdgeList = true;
    // // for (final ChartEdge possibleBuildableEdge : goldChart.getChartCell(guessSpan.start(),
    // guessSpan.end()).getEdges()) {
    // // if (possibleBuildableEdge.p.isLexProd() == false) {
    // // buildableGoldEdgesInGuessSpan.add(possibleBuildableEdge);
    // // }
    // // }
    // //
    // // }
    // // }
    //
    // // if (guessSpanInGoldBuildableSpans) {
    // // for (final ChartEdge possibleBuildableEdge : goldChart.getChartCell(guessSpan.start(),
    // guessSpan.end()).getEdges()) {
    // // if (possibleBuildableEdge.p.isLexProd() == false) {
    // // buildableGoldEdgesInGuessSpan.add(possibleBuildableEdge);
    // // }
    // // }
    // // }
    //
    // // final LinkedList<ChartEdge> goldEdgesCopy = (LinkedList<ChartEdge>) buildableGoldEdgesInGuessSpan.clone();
    // // visitCell(guessSpan, buildableGoldEdgesInGuessSpan);
    // visitCell(guessSpan, goldEdgeListForGuessSpan);
    //
    // // force us to build the entire gold tree. If we built a different TOP production
    // // than remove it and keep parsing.
    // // if (hasCompleteParse() && goldEdgesCopy.size() == 0) {
    // // if (hasCompleteParse() && builtGoldEdgeList == false) {
    // // ((ArrayChartCell) (this.rootChartCell)).bestEdge[grammar.startSymbol] = null;
    // // }
    // // }
    // goldEdgeList = getGoldEdgeList(goldChart);
    // }
    //
    // totalCells += perceptronCellSelector.numTotal;
    // correctCells += perceptronCellSelector.numCorrect;
    //
    // System.out.println(" sentLen=" + sent.length + " nGuessSpans=" + perceptronCellSelector.numTotal +
    // " nCorrectSpans=" + perceptronCellSelector.numCorrect
    // + " pct=" + ((float) perceptronCellSelector.numCorrect / perceptronCellSelector.numTotal));
    // }
    // }
    // perceptronCellSelector.decreaseLearningRate();
    // System.out.println("TOTAL: total=" + totalCells + " correct=" + correctCells + " pct=" + ((float) correctCells /
    // totalCells));
    // }
    // }
    //
    // // TODO: there must be a better way to extract the buildable gold edges. Something
    // // at least in O(n^2) if not O(numTreeNodes)
    // private LinkedList<ChartEdge> getGoldBinaryEdgeList(final EdgeCellChart goldChart) throws Exception {
    // ChartCell leftChartCell, rightChartCell;
    // ChartEdge leftGoldEdge, rightGoldEdge;
    // final LinkedList<ChartEdge> edgeCollection = new LinkedList<ChartEdge>();
    //
    // for (int spanLength = 1; spanLength <= chart.size(); spanLength++) {
    // for (int start = 0; start < chart.size() - spanLength + 1; start++) { // beginning
    // final int end = start + spanLength;
    // final ChartCell curSpan = chart.getCell(start, end);
    // for (final ChartEdge goldEdge : goldChart.getCell(start, end).getEdges()) {
    // // if (curSpan.hasEdge(goldEdge) == false && curSpan.canBuild(goldEdge, chart)) {
    // if (curSpan.hasEdge(goldEdge) == false) {
    // if (goldEdge.prod.isBinaryProd()) {
    // leftChartCell = chart.getCell(start, goldEdge.midpt());
    // rightChartCell = chart.getCell(goldEdge.midpt(), end);
    // leftGoldEdge = goldChart.getCell(start, goldEdge.midpt()).getBestEdge(goldEdge.prod.leftChild);
    // rightGoldEdge = goldChart.getCell(goldEdge.midpt(), end).getBestEdge(goldEdge.prod.rightChild);
    // if (leftChartCell.hasEdge(leftGoldEdge) && rightChartCell.hasEdge(rightGoldEdge)) {
    // edgeCollection.add(goldEdge);
    // }
    // }
    // }
    // }
    // }
    // }
    // return edgeCollection;
    // }
    //
    // // TODO: there must be a better way to extract the buildable gold edges. Something
    // // at least in O(n^2) if not O(numTreeNodes)
    // private LinkedList<ChartEdge> getGoldEdgeList(final EdgeCellChart goldChart) throws Exception {
    // ChartCell leftChartCell, rightChartCell;
    // ChartEdge leftGoldEdge, rightGoldEdge;
    // final LinkedList<ChartEdge> edgeCollection = new LinkedList<ChartEdge>();
    //
    // for (int spanLength = 1; spanLength <= chart.size(); spanLength++) {
    // for (int start = 0; start < chart.size() - spanLength + 1; start++) { // beginning
    // final int end = start + spanLength;
    // final ChartCell curSpan = chart.getCell(start, end);
    // for (final ChartEdge goldEdge : goldChart.getCell(start, end).getEdges()) {
    // // if (curSpan.hasEdge(goldEdge) == false && curSpan.canBuild(goldEdge, chart)) {
    // if (curSpan.hasEdge(goldEdge) == false) {
    // if (goldEdge.prod.isBinaryProd()) {
    // leftChartCell = chart.getCell(start, goldEdge.midpt());
    // rightChartCell = chart.getCell(goldEdge.midpt(), end);
    // leftGoldEdge = goldChart.getCell(start, goldEdge.midpt()).getBestEdge(goldEdge.prod.leftChild);
    // rightGoldEdge = goldChart.getCell(goldEdge.midpt(), end).getBestEdge(goldEdge.prod.rightChild);
    // if (leftChartCell.hasEdge(leftGoldEdge) && rightChartCell.hasEdge(rightGoldEdge)) {
    // edgeCollection.add(goldEdge);
    // }
    // } else {
    // leftChartCell = chart.getCell(start, end);
    // leftGoldEdge = goldChart.getCell(start, end).getBestEdge(goldEdge.prod.leftChild);
    // if (leftChartCell.hasEdge(leftGoldEdge)) {
    // edgeCollection.add(goldEdge);
    // }
    // }
    //
    // }
    // }
    // }
    // }
    // return edgeCollection;
    // }
    //
    // // private ChartCell getGoldSpan(final Chart goldChart) throws Exception {
    // private ChartEdge getGoldEdge(final EdgeCellChart goldChart) throws Exception {
    // // Pick largest span where a gold edge can be (and has not yet been) built.
    // // Other ideas:
    // // * Of qualifying spans, pick span we could be "most sure" of
    // // * Prefer to re-choose a span if the gold edge was not popped?
    //
    // // for (int spanLength = chartSize; spanLength >= 1; spanLength--) {
    // for (int spanLength = 1; spanLength <= chart.size(); spanLength++) {
    // for (int start = 0; start < chart.size() - spanLength + 1; start++) { // beginning
    // final int end = start + spanLength;
    // final ChartCell curSpan = chart.getCell(start, end);
    // for (final ChartEdge goldEdge : goldChart.getCell(start, end).getEdges()) {
    // if (curSpan.hasEdge(goldEdge) == false && curSpan.canBuild(goldEdge)) {
    // // System.out.println(goldEdge);
    // // return curSpan;
    // return goldEdge;
    // }
    // }
    // }
    // }
    //
    // Log.info(0, "WARNING: no gold edge found");
    // return null;
    // }
    //
    // // protected int visitCell(final ChartCell cell, final ChartEdge goldEdge) {
    // protected int visitCell(final ChartCell cell, final LinkedList<ChartEdge> goldEdges) {
    // final int start = cell.start(), end = cell.end();
    // Collection<Production> possibleProds;
    // ChartEdge edge;
    // final ChartEdge[] bestEdges = new ChartEdge[grammar.numNonTerms()];
    //
    // cell.numSpanVisits++;
    //
    // // first add unary edges. If we have previously visited this cell, there may be edges
    // // already placed in the chart which we can build unary edges from. They won't be built
    // // during the addBestEdgesToChart because edgeAdded will equal false since the edge is already
    // // in the chart. We could also solve this problem by clearing all entres from this cell
    // // before processing.
    // for (final ChartEdge possUnaryChild : chart.getCell(start, end).getEdges()) {
    // for (final Production p : grammar.getUnaryProductionsWithChild(possUnaryChild.prod.parent)) {
    // final float prob = p.prob + possUnaryChild.inside;
    // edge = chart.new ChartEdge(p, cell, prob, edgeSelector);
    // addEdgeToArray(edge, bestEdges);
    // // addEdgeToAgenda(edge, agenda);
    // // addEdgeToAgenda(edge, agenda, goldEdges);
    // }
    // }
    //
    // // add binary edges
    // for (int mid = start + 1; mid <= end - 1; mid++) { // mid point
    // final ChartCell leftCell = chart.getCell(start, mid);
    // final ChartCell rightCell = chart.getCell(mid, end);
    // for (final int leftNT : leftCell.getLeftChildNTs()) {
    // for (final int rightNT : rightCell.getRightChildNTs()) {
    // possibleProds = grammar.getBinaryProductionsWithChildren(leftNT, rightNT);
    // if (possibleProds != null) {
    // for (final Production p : possibleProds) {
    // final float prob = p.prob + leftCell.getInside(leftNT) + rightCell.getInside(rightNT);
    // edge = chart.new ChartEdge(p, leftCell, rightCell, prob, edgeSelector);
    // addEdgeToArray(edge, bestEdges);
    // // addEdgeToAgenda(edge, agenda);
    // // addEdgeToAgenda(edge, agenda, goldEdges);
    // }
    // }
    // }
    // }
    // }
    //
    // // if (spanLength == 1) {
    // // for (final int pos : arrayChartCell.getPosEntries()) {
    // // for (final Production p : grammar.getUnaryProdsByChild(pos)) {
    // // final float prob = p.prob + arrayChartCell.getBestEdge(pos).insideProb;
    // // edge = new ChartEdge(p, arrayChartCell, prob, edgeFOM);
    // // System.out.println("  unary:" + edge);
    // // // addEdgeToArray(edge, bestEdges);
    // // addEdgeToAgenda(edge, agenda);
    // // }
    // // }
    // // }
    //
    // return addBestEdgesToChart(cell, bestEdges, goldEdges);
    // // return addBestEdgesToChart(arrayChartCell, agenda, goldEdge);
    // // return addBestEdgesToChart(arrayChartCell, agenda, goldEdges);
    // }
    //
    // private void addEdgeToArray(final ChartEdge edge, final ChartEdge[] bestEdges) {
    // final int parent = edge.prod.parent;
    // if (bestEdges[parent] == null || edge.fom > bestEdges[parent].fom) {
    // bestEdges[parent] = edge;
    // }
    // }
    //
    // private void addEdgeToAgenda(final ChartEdge edge, final PriorityQueue<ChartEdge> agenda) {
    // agenda.add(edge);
    // nAgendaPush++;
    // }
    //
    // // private void addEdgeToAgenda(final ChartEdge edge, final PriorityQueue<ChartEdge> agenda, final
    // Collection<ChartEdge> goldEdges) {
    // // force the gold parse to be built by only allowing TOP edges from the gold parse
    // // i dont think this would work ... we can still build the gold TOP edge without the rest of the gold tree
    // // }
    //
    // private int addBestEdgesToChart(final ChartCell cell, final ChartEdge[] bestEdges, final LinkedList<ChartEdge>
    // goldEdges) {
    // ChartEdge edge, unaryEdge;
    // boolean addedEdge;
    // int numAdded = 0;
    //
    // agenda = new PriorityQueue<ChartEdge>();
    // for (int i = 0; i < bestEdges.length; i++) {
    // if (bestEdges[i] != null) {
    // addEdgeToAgenda(bestEdges[i], agenda);
    // }
    // }
    //
    // // System.out.println(" agenda size = " + agenda.size());
    //
    // while (agenda.isEmpty() == false && numAdded <= maxEdgesToAdd) {
    // // while (addedGoldEdge == false) {
    // edge = agenda.poll();
    // addedEdge = cell.addEdge(edge, edge.inside);
    // if (addedEdge) {
    // numAdded++;
    // // Add unary productions to agenda so they can compete with binary productions
    // for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
    // unaryEdge = chart.new ChartEdge(p, cell, p.prob + edge.inside, edgeSelector);
    // addEdgeToAgenda(unaryEdge, agenda);
    // }
    // }
    // }
    //
    // // force add gold edges
    // for (final ChartEdge goldEdge : goldEdges) {
    // cell.addEdgeForceOverwrite(goldEdge);
    // }
    //
    // return numAdded;
    // }
    //
    // // private int addBestEdgesToChart(final ArrayChartCell cell, final PriorityQueue<ChartEdge> agenda, final
    // ChartEdge goldEdge) {
    // // private int addBestEdgesToChart(final ArrayChartCell cell, final PriorityQueue<ChartEdge> agenda, final
    // LinkedList<ChartEdge> goldEdges) {
    // // ChartEdge edge, unaryEdge;
    // // boolean addedEdge;
    // // final boolean addedGoldEdge = false;
    // // int numAdded = 0;
    // //
    // // // make copy so we can force add them at the end
    // // final LinkedList<ChartEdge> goldEdgesCopy = (LinkedList<ChartEdge>) goldEdges.clone();
    // // for (final ChartEdge e : goldEdgesCopy) {
    // // System.out.println("  gold edge:" + e);
    // // }
    // //
    // // int maxEdgesToAdd = 15;
    // // if (goldEdges.size() > 0) {
    // // maxEdgesToAdd = Integer.MAX_VALUE;
    // // }
    // //
    // // System.out.println(" agendaSize = " + agenda.size() + " goldSize=" + goldEdges.size() + " maxEdges=" +
    // maxEdgesToAdd);
    // //
    // // // We can sometimes not be able to build the gold edge because
    // // // (1) the viterbi max edge is not the gold edge and the gold edge gets pruned (this temp works by adding all
    // edges to the agenda)
    // // // (2) the gold edge contains a production not in our grammar
    // // // while (addedGoldEdge == false && agenda.isEmpty() == false) {
    // // while (goldEdges.size() > 0 || (numAdded < maxEdgesToAdd && !agenda.isEmpty())) {
    // // edge = agenda.poll();
    // // // if (goldEdge.equals(edge)) {
    // // if (goldEdges.contains(edge)) {
    // // // addedGoldEdge = true;
    // // goldEdges.remove(edge);
    // // }
    // // // addedEdge = cell.addEdge(edge);
    // // // we must keep adding edges until we reach the gold edge. We can't keep just the viterbi best
    // // // for a particular NT because we can miss the gold edge.
    // // // addedEdge = cell.addEdgeForceOverwrite(edge);
    // // addedEdge = cell.addEdge(edge);
    // // if (addedEdge) {
    // // numAdded++;
    // // // Add unary productions to agenda so they can compete with binary productions
    // // for (final Production p : grammar.getUnaryProdsByChild(edge.p.parent)) {
    // // unaryEdge = new ChartEdge(p, cell, p.prob + edge.insideProb, edgeFOM);
    // // addEdgeToAgenda(unaryEdge, agenda);
    // // // addEdgeToAgenda(edge, agenda, goldEdges);
    // // }
    // // }
    // // }
    // //
    // // for (final ChartEdge goldEdge : goldEdgesCopy) {
    // // cell.addEdgeForceOverwrite(goldEdge);
    // // }
    // //
    // // return numAdded;
    // // }
    //
    // private int addBestEdgesToChart(final ChartCell cell, final PriorityQueue<ChartEdge> agenda, final
    // LinkedList<ChartEdge> goldEdges) {
    // ChartEdge edge, unaryEdge;
    // boolean addedEdge;
    // int numAdded = 0;
    //
    // final int maxEdgesToAdd = 15;
    // System.out.println(" agendaSize = " + agenda.size() + " goldSize=" + goldEdges.size() + " maxEdges=" +
    // maxEdgesToAdd);
    //
    // // We can sometimes not be able to build the gold edge because
    // // (1) the viterbi max edge is not the gold edge and the gold edge gets pruned (this temp works by adding all
    // edges to the agenda)
    // // (2) the gold edge contains a production not in our grammar
    // while (numAdded < maxEdgesToAdd && !agenda.isEmpty()) {
    // edge = agenda.poll();
    // addedEdge = cell.addEdge(edge, edge.inside);
    // if (addedEdge) {
    // numAdded++;
    // // Add unary productions to agenda so they can compete with binary productions
    // for (final Production p : grammar.getUnaryProductionsWithChild(edge.prod.parent)) {
    // unaryEdge = chart.new ChartEdge(p, cell, p.prob + edge.inside, edgeSelector);
    // addEdgeToAgenda(unaryEdge, agenda);
    // }
    // }
    // }
    //
    // // force add gold edges
    // for (final ChartEdge goldEdge : goldEdges) {
    // cell.addEdgeForceOverwrite(goldEdge);
    // }
    //
    // return numAdded;
    // }

}
