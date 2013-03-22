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

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;

/**
 * Extracts rare-word attributes (including syntactic features) from a training corpus. Outputs in Weka ARFF file
 * format, for use in k-means++ or other clustering systems.
 * 
 * @author Aaron Dunlop
 * @since Mar 21, 2013
 */
public class ExtractWekaWordClusteringFeatures extends BaseTextNormalizationTool {

    @Override
    protected void run() throws Exception {

        //
        // Set up attributes
        //
        final FastVector attributes = new FastVector();

        // Unigram and bigram suffixes are string attributes
        attributes.addElement(new Attribute("Token", (FastVector) null));
        attributes.addElement(new Attribute("UnigramSuffix", (FastVector) null));
        attributes.addElement(new Attribute("BigramSuffix", (FastVector) null));

        // Contains a numeral
        final FastVector containsNumeralValues = new FastVector();
        containsNumeralValues.addElement("+num");
        containsNumeralValues.addElement("-num");
        attributes.addElement(new Attribute("ContainsNumeral", containsNumeralValues));
        final int containsNumeralPositive = containsNumeralValues.indexOf("+num");
        final int containsNumeralNegative = containsNumeralValues.indexOf("-num");

        // Numeral percentage
        final FastVector numeralPercentage = new FastVector();
        numeralPercentage.addElement("0");
        numeralPercentage.addElement("20+");
        numeralPercentage.addElement("40+");
        numeralPercentage.addElement("60+");
        numeralPercentage.addElement("80+");
        numeralPercentage.addElement("100");
        attributes.addElement(new Attribute("NumeralPercentage", numeralPercentage));
        final int numeralPercentage0 = numeralPercentage.indexOf("0");
        final int numeralPercentage20 = numeralPercentage.indexOf("20+");
        final int numeralPercentage40 = numeralPercentage.indexOf("40+");
        final int numeralPercentage60 = numeralPercentage.indexOf("60+");
        final int numeralPercentage80 = numeralPercentage.indexOf("80+");
        final int numeralPercentage100 = numeralPercentage.indexOf("100");

        // Contains '@'
        final FastVector containsAtValues = new FastVector();
        containsAtValues.addElement("+@");
        containsAtValues.addElement("-@");
        attributes.addElement(new Attribute("Contains@", containsAtValues));
        final int containsAtPositive = containsAtValues.indexOf("+@");
        final int containsAtNegative = containsAtValues.indexOf("-@");

        // Starts-with '#'
        final FastVector startsWithHashValues = new FastVector();
        startsWithHashValues.addElement("+#-start");
        startsWithHashValues.addElement("-#-start");
        attributes.addElement(new Attribute("StartsWith#", startsWithHashValues));
        final int startsWithHashPositive = startsWithHashValues.indexOf("+#-start");
        final int startsWithHashNegative = startsWithHashValues.indexOf("-#-start");

        // Starts-with 'http'
        final FastVector startsWithHttpValues = new FastVector();
        startsWithHttpValues.addElement("+http-start");
        startsWithHttpValues.addElement("-http-start");
        attributes.addElement(new Attribute("StartsWithHttp", startsWithHttpValues));
        final int startsWithHttpPositive = startsWithHttpValues.indexOf("+http-start");
        final int startsWithHttpNegative = startsWithHttpValues.indexOf("-http-start");

        //
        // Populate instances
        //
        final Instances instances = new Instances("RareWords", attributes, 0);

        // Read the entire corpus and count token occurrences
        final BufferedReader br = inputAsBufferedReader();
        // Allow re-reading up to 50 MB
        br.mark(50 * 1024 * 1024);

        final Object2IntOpenHashMap<String> tokenCounts = countTokenOccurrences(br);

        // Reset the reader and reread the corpus, this time applying appropriate normalizations and outputting each
        // tree
        br.reset();

        for (final String line : inputLines(br)) {
            final NaryTree<String> tree = NaryTree.read(line, String.class);

            for (final NaryTree<String> leaf : tree.leafTraversal()) {

                final String token = leaf.label();
                if (!thresholdMap.containsKey(leaf.parentLabel())
                        || tokenCounts.getInt(key(leaf)) > thresholdMap.getInt(leaf.parentLabel())) {
                    continue;
                }

                final DoubleList values = new DoubleArrayList();

                // Token
                values.add(instances.attribute(values.size()).addStringValue(token));

                // Unigram suffix
                values.add(instances.attribute(values.size()).addStringValue(token.substring(token.length() - 1)));

                // Bigram suffix
                if (token.length() >= 2) {
                    values.add(instances.attribute(values.size()).addStringValue(token.substring(token.length() - 2)));
                } else {
                    values.add(instances.attribute(values.size()).addStringValue(""));
                }

                // Contains-numeral and numeral percentage
                final float np = numeralPercentage(token);
                if (np > 0) {
                    values.add(containsNumeralPositive);
                    if (np == 1f) {
                        values.add(numeralPercentage100);
                    } else if (np >= .8f) {
                        values.add(numeralPercentage80);
                    } else if (np >= .6f) {
                        values.add(numeralPercentage60);
                    } else if (np >= .4f) {
                        values.add(numeralPercentage40);
                    } else if (np >= .2f) {
                        values.add(numeralPercentage20);
                    }
                } else {
                    values.add(containsNumeralNegative);
                    values.add(numeralPercentage0);
                }

                values.add(token.indexOf('@') >= 0 ? containsAtPositive : containsAtNegative);
                values.add(token.indexOf('#') == 0 ? startsWithHashPositive : startsWithHashNegative);
                values.add(token.startsWith("http") ? startsWithHttpPositive : startsWithHttpNegative);

                instances.add(new Instance(1.0, values.toDoubleArray()));
            }
        }

        // Write the features out in ARFF format
        System.out.println(instances);
    }

    private float numeralPercentage(final String token) {
        float numerals = 0;
        for (int i = 0; i < token.length(); i++) {
            if (Character.isDigit(token.charAt(i))) {
                numerals++;
            }
        }
        return numerals / token.length();
    }

    public static void main(final String[] args) {
        run(args);
    }

}
