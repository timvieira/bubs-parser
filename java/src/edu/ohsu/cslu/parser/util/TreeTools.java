package edu.ohsu.cslu.parser.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import edu.ohsu.cslu.parser.ParserOptions;

public class TreeTools {

    private static BufferedReader inputStream = new BufferedReader(new InputStreamReader(System.in));
    private static BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(System.out));

    public static void main(final String[] args) throws Exception {
        for (String sentence = inputStream.readLine(); sentence != null; sentence = inputStream.readLine()) {
            outputStream.write(sentence);
            final ParseTree tree = ParseTree.readBracketFormat(sentence);
            ParseTree.unbinarizeTree(tree, ParserOptions.GrammarFormatType.CSLU);
            outputStream.write(tree.toString());
        }
    }

}
