/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

package edu.ohsu.cslu.tools;

import java.io.BufferedReader;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.GrammarFormatType;

/**
 * Formats a standalone LaTeX CYK chart, using cykchart.sty
 * 
 * Reads input in bracketed tree format from a file or STDIN
 * 
 * @author Aaron Dunlop
 */
public class LatexCykChart extends BaseCommandlineTool {

    @Option(name = "-b", metaVar = "direction", usage = "Binarization direction")
    private Binarization binarization = Binarization.LEFT;

    @Override
    protected void run() throws Exception {

        final BufferedReader br = inputAsBufferedReader();
        br.mark(8096);

        // If the input is longer than a single line, use tabular to format multiple charts
        boolean tabular = false;
        br.readLine();
        if (br.readLine() != null) {
            tabular = true;
        }
        br.reset();

        System.out.println("\\documentclass[class=minimal,border=10pt]{standalone}");
        System.out.println("\\usepackage{tikz}");
        System.out.println("\\usepackage{cykchart}");

        System.out.println("\\begin{document}");

        if (tabular) {
            System.out.println("\\begin{tabular}{@{}c@{}}\n");

            for (final String line : inputLines(br)) {
                formatChart(NaryTree.read(line, String.class));
                System.out.println("\n\\\\\n\\\\\n");
            }

            System.out.println("\\end{tabular}");

        } else {
            formatChart(NaryTree.read(br.readLine(), String.class));
        }

        System.out.println("\\end{document}");
    }

    private void formatChart(final NaryTree<String> tree) {
        final int sentenceLength = tree.leaves();

        System.out.println("  \\begin{tikzpicture}[node distance=0 cm,outer sep = 0pt]%");
        System.out.println("    % Setup the chart options");
        System.out.println("    \\defcykcell{1.5cm}{3.5cm}{13}{7}%");
        System.out.println("    \\definecolor{lightgray}{rgb}{0.8,0.8,0.8}%");
        System.out.println("    \\definecolor{mediumgray}{rgb}{0.5,0.5,0.5}%");
        System.out.println("    \\definecolor{nobegin}{rgb}{0.35,0.35,0.35}%");
        System.out.println("    \\definecolor{noend}{rgb}{0.35,0.35,0.35}%");
        System.out.println("    \\definecolor{closed}{rgb}{0.35,0.35,0.35}%");
        System.out.println();
        System.out.println("    % Format chart cells");

        final BinaryTree<String> binaryTree = tree.binarize(GrammarFormatType.Berkeley, binarization);
        final String[][] populatedCells = new String[sentenceLength][sentenceLength + 1];
        traverse(populatedCells, binaryTree, 0, sentenceLength, "");

        for (int span = 1; span <= sentenceLength; span++) {
            for (int start = 0; start < sentenceLength - span + 1; start++) {
                final int end = start + span;
                if (populatedCells[start][end] != null) {
                    System.out.println(populatedCells[start][end]);
                } else {
                    System.out.format("    \\cykcell[white]{%d}{%d}{}\n\n", start, end);
                }
            }
        }
        System.out.println("  \\end{tikzpicture}");
    }

    private void traverse(final String[][] populatedCells, final BinaryTree<String> binaryTree, final int start,
            final int end, final String unaries) {

        if (binaryTree.isPreterminal()) {
            populatedCells[start][end] = String.format(
                    "    \\cykcell[white]{%d}{%d}{\n%s      \\unaryedge{%s}{%s}{ } \\\\\n    }\n", start, end, unaries,
                    latexEscape(binaryTree.label()), latexEscape(binaryTree.leftChild().label()));
            // Stop when we reach a preterminal and its leaf
            return;

        } else if (binaryTree.rightChild() == null) {
            // Unary edge - add edge to the chart
            final String newUnaries = unaries
                    + String.format("      \\unaryedge{%s}{%s}{ } \\\\\n", latexEscape(binaryTree.label()),
                            latexEscape(binaryTree.leftChild().label()));
            // Recurse on the child
            traverse(populatedCells, binaryTree.leftChild(), start, end, newUnaries);

        } else {
            // Binary edge
            populatedCells[start][end] = String.format(
                    "    \\cykcell[white]{%d}{%d}{\n%s      \\binaryedge{%s}{%s}{%s}{ }{ } \\\\\n    }\n", start, end,
                    unaries, latexEscape(binaryTree.label()), latexEscape(binaryTree.leftChild().label()),
                    latexEscape(binaryTree.rightChild().label()));

            // Recurse on both children
            final int midpoint = start + binaryTree.leftChild().leaves();
            traverse(populatedCells, binaryTree.leftChild(), start, midpoint, "");
            traverse(populatedCells, binaryTree.rightChild(), midpoint, end, "");
        }
    }

    private String latexEscape(String s) {

        // Special characters
        s = s.replaceAll("\\#", "\\\\#");
        s = s.replaceAll("\\$", "\\\\\\$");
        s = s.replaceAll("%", "\\\\%");
        s = s.replaceAll("\\&", "\\\\&");
        s = s.replaceAll("~", "\\\\~{}");
        s = s.replaceAll("_", "\\\\_");
        s = s.replaceAll("\\^", "\\\\^{}");
        s = s.replaceAll("\\{", "\\\\{");
        s = s.replaceAll("\\}", "\\\\}");

        // Convert quotes
        s = s.replaceAll("^'", "`");
        s = s.replaceAll("^\"", "``");
        s = s.replaceAll(" '", " `");
        s = s.replaceAll(" \"", " ``");

        return s;
    }

    public static void main(final String[] args) {
        run(args);
    }

}
