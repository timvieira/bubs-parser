/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */ 
package edu.ohsu.cslu.parser.beam;


//public class BSCPBeamConf extends BSCPPruneViterbi {

//    protected AveragedPerceptron beamConfModel;
//    protected int[] beamClassCounts;
//    // protected int maxBeamWidth;
//    protected float scoreThresh;
//    private String cellStats;
//    private boolean inferFactoredCells = false, classifyBaseCells = false;
//    private int beamWidthValues[][];
//
//    public BSCPBeamConf(final ParserDriver opts, final LeftHashGrammar grammar, final AveragedPerceptron beamWidthModel) {
//        super(opts, grammar);
//        this.beamConfModel = beamWidthModel;
//        this.beamClassCounts = new int[beamWidthModel.numClasses()];
//
//        if (ParserDriver.param2 != -1) {
//            inferFactoredCells = true;
//        }
//
//        if (ParserDriver.param3 != -1) {
//            classifyBaseCells = true;
//        }
//
//        if (inferFactoredCells == false && classifyBaseCells == true) {
//            throw new IllegalArgumentException("ERROR: got that wrong -- no models -fact +base");
//        }
//
//        logger.finer("INFO: beamconf: inferFactoredCells=" + ParserUtil.bool2int(inferFactoredCells)
//                + " classifyBaseCells=" + ParserUtil.bool2int(classifyBaseCells));
//    }
//
//    @Override
//    protected void initSentence(final int[] tokens) {
//        super.initSentence(tokens);
//        computeBeamWidthValues();
//    }
//
//    @Override
//    protected void initCell(final short start, final short end) {
//        super.initCell(start, end);
//        if (numReparses == 0) {
//            beamWidth = beamWidthValues[start][end];
//        } else {
//            // back off to not using any constraints if we're reparsing
//            beamWidth = origBeamWidth * (int) Math.pow(2, numReparses);
//        }
//
//        // if (classifyBaseCells || end - start > 1) {
//        // final SparseBitVector feats = getCellFeatures(start, end, beamConfModel.featureTemplate());
//        // final int guessClass = beamConfModel.classify(feats);
//        //
//        // beamClassCounts[guessClass]++;
//        // beamWidth = (int) beamConfModel.class2value(guessClass);
//        //
//        // final int maxBeamWidth = origBeamWidth * (int) Math.pow(2, numReparses);
//        // if (beamWidth > maxBeamWidth) {
//        // beamWidth = maxBeamWidth;
//        // }
//        // }
//    }
//
//    @Override
//    public String getStats() {
//        logger.finer("INFO: beamconf: " + cellStats);
//        String s = "";
//        for (int i = 0; i < beamConfModel.numClasses(); i++) {
//            s += String.format(" class%d:%d", i, beamClassCounts[i]);
//        }
//        return super.getStats() + s;
//    }
//
//    // TODO: Should move this into a CellSelector class....
//    private void computeBeamWidthValues() {
//        SparseBitVector feats;
//        int guessBeamWidth;
//        int guessClass;
//        final int n = this.currentInput.sentenceLength;
//        beamWidthValues = new int[n][n + 1];
//        cellStats = "";
//
//        Arrays.fill(beamClassCounts, 0);
//
//        // traverse in a top-down order so we can remember when we first see a non-empty cell
//        // only works for right factored (berkeley) grammars right now.
//        // for (int end = 1; end < n + 1; end++) {
//        for (int start = 0; start < n; start++) {
//            boolean foundOpenCell = false;
//            // for (int start = 0; start < end; start++) {
//            for (int end = n; end > start; end--) {
//                if (end - start == 1 && classifyBaseCells == false) {
//                    beamWidthValues[start][end] = origBeamWidth;
//                    cellStats += String.format("%d,%d=%d ", start, end, origBeamWidth);
//                } else {
//                    feats = getCellFeatures(start, end, beamConfModel.featureTemplate());
//                    guessClass = beamConfModel.classify(feats);
//                    beamClassCounts[guessClass]++;
//                    guessBeamWidth = (int) Math.min(beamConfModel.class2value(guessClass), origBeamWidth);
//
//                    // need to allow factored productions for classifiers that don't predict these cells
//                    if (inferFactoredCells == true && guessBeamWidth == 0 && foundOpenCell) {
//                        guessBeamWidth = factoredBeamWidth;
//                        cellStats += String.format("%d,%d=2 ", start, end);
//                    } else if (guessBeamWidth > 0) {
//                        foundOpenCell = true;
//                        // cellStats += String.format("%d,%d=%d ", start, end, guessBeamWidth > 0 ? 4 : 0);
//                        cellStats += String.format("%d,%d=%d ", start, end, guessBeamWidth);
//                    }
//
//                    beamWidthValues[start][end] = guessBeamWidth;
//                }
//            }
//        }
//
//    }
// }
