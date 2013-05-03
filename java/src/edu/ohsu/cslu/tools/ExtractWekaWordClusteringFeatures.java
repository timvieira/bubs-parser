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
import java.util.ListIterator;
import java.util.NoSuchElementException;

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

    private final static String NULL = "<null>";

    @Override
    protected void run() throws Exception {

        //
        // Set up attributes
        //
        final FastVector attributes = new FastVector();

        // The token itself
        attributes.addElement(new Attribute("Token", (FastVector) null));

        // Previous, current, and next POS tags
        attributes.addElement(new Attribute("Pos-1", (FastVector) null));
        attributes.addElement(new Attribute("Pos", (FastVector) null));
        attributes.addElement(new Attribute("Pos+1", (FastVector) null));

        // Grandparent and great-grandparent non-terminal labels
        attributes.addElement(new Attribute("GrandparentLabel", (FastVector) null));
        attributes.addElement(new Attribute("GreatGrandparentLabel", (FastVector) null));

        // Grandparent span
        final FastVector grandparentSpan = new FastVector();
        grandparentSpan.addElement("1");
        grandparentSpan.addElement("2");
        grandparentSpan.addElement("3");
        grandparentSpan.addElement("45");
        grandparentSpan.addElement("6+");
        attributes.addElement(new Attribute("GrandparentSpan", grandparentSpan));
        final int grandparentSpan1 = grandparentSpan.indexOf("1");
        final int grandparentSpan2 = grandparentSpan.indexOf("2");
        final int grandparentSpan3 = grandparentSpan.indexOf("3");
        final int grandparentSpan45 = grandparentSpan.indexOf("45");
        final int grandparentSpan6 = grandparentSpan.indexOf("6+");

        // Grandparent span
        final FastVector greatGrandparentSpan = new FastVector();
        greatGrandparentSpan.addElement("1");
        greatGrandparentSpan.addElement("2");
        greatGrandparentSpan.addElement("3");
        greatGrandparentSpan.addElement("45");
        greatGrandparentSpan.addElement("6+");
        attributes.addElement(new Attribute("GreatGrandparentSpan", greatGrandparentSpan));
        final int greatGrandparentSpan1 = greatGrandparentSpan.indexOf("1");
        final int greatGrandparentSpan2 = greatGrandparentSpan.indexOf("2");
        final int greatGrandparentSpan3 = greatGrandparentSpan.indexOf("3");
        final int greatGrandparentSpan45 = greatGrandparentSpan.indexOf("45");
        final int greatGrandparentSpan6 = greatGrandparentSpan.indexOf("6+");

        // Unigram and bigram suffixes are string attributes
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

        // Contains punctation
        final FastVector containsPunctuationValues = new FastVector();
        containsPunctuationValues.addElement("+punct");
        containsPunctuationValues.addElement("-punct");
        attributes.addElement(new Attribute("ContainsPunctuation", containsPunctuationValues));
        final int containsPunctuationPositive = containsPunctuationValues.indexOf("+punct");
        final int containsPunctuationNegative = containsPunctuationValues.indexOf("-punct");

        // Punctuation percentage
        final FastVector punctuationPercentage = new FastVector();
        punctuationPercentage.addElement("0");
        punctuationPercentage.addElement("20+");
        punctuationPercentage.addElement("40+");
        punctuationPercentage.addElement("60+");
        punctuationPercentage.addElement("80+");
        punctuationPercentage.addElement("100");
        attributes.addElement(new Attribute("PunctuationPercentage", punctuationPercentage));
        final int punctuationPercentage0 = punctuationPercentage.indexOf("0");
        final int punctuationPercentage20 = punctuationPercentage.indexOf("20+");
        final int punctuationPercentage40 = punctuationPercentage.indexOf("40+");
        final int punctuationPercentage60 = punctuationPercentage.indexOf("60+");
        final int punctuationPercentage80 = punctuationPercentage.indexOf("80+");
        final int punctuationPercentage100 = punctuationPercentage.indexOf("100");

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

            for (final ListIterator<NaryTree<String>> iter = tree.leafList().listIterator(); iter.hasNext();) {
                final NaryTree<String> leaf = iter.next();

                final String token = leaf.label();
                if (!thresholdMap.containsKey(leaf.parentLabel())
                        || tokenCounts.getInt(key(leaf)) > thresholdMap.getInt(leaf.parentLabel())) {
                    continue;
                }

                final DoubleList values = new DoubleArrayList();

                // Token
                values.add(instances.attribute(values.size()).addStringValue(token));

                // Previous POS label
                try {
                    iter.previous();
                    values.add(instances.attribute(values.size()).addStringValue(iter.previous().parentLabel()));
                    iter.next();
                } catch (final NoSuchElementException e) {
                    values.add(instances.attribute(values.size()).addStringValue(NULL));
                }

                // Current POS label
                values.add(instances.attribute(values.size()).addStringValue(iter.next().parentLabel()));

                // Next POS label
                try {
                    values.add(instances.attribute(values.size()).addStringValue(iter.next().parentLabel()));
                    iter.previous();
                } catch (final NoSuchElementException e) {
                    values.add(instances.attribute(values.size()).addStringValue(NULL));
                }

                // Grandparent label (the parent of the POS)
                final NaryTree<String> grandparent = leaf.parent().parent();
                NaryTree<String> greatGrandparent = null;
                if (grandparent != null) {
                    values.add(instances.attribute(values.size()).addStringValue(grandparent.label()));

                    // Great-grandparent label
                    greatGrandparent = grandparent.parent();
                    if (greatGrandparent != null) {
                        values.add(instances.attribute(values.size()).addStringValue(greatGrandparent.label()));
                    } else {
                        values.add(instances.attribute(values.size()).addStringValue(NULL));
                    }
                } else {
                    values.add(instances.attribute(values.size()).addStringValue(NULL));
                    values.add(instances.attribute(values.size()).addStringValue(NULL));
                }

                // Grandparent span
                if (grandparent != null) {
                    switch (grandparent.leaves()) {
                    case 1:
                        values.add(grandparentSpan1);
                        break;
                    case 2:
                        values.add(grandparentSpan2);
                        break;
                    case 3:
                        values.add(grandparentSpan3);
                        break;
                    case 4:
                    case 5:
                        values.add(grandparentSpan45);
                        break;
                    default:
                        values.add(grandparentSpan6);
                        break;
                    }
                } else {
                    // Treat a _very_ shallow tree as span-1
                    values.add(grandparentSpan1);
                }

                // Great-grandparent span
                if (greatGrandparent != null) {
                    switch (greatGrandparent.leaves()) {
                    case 1:
                        values.add(greatGrandparentSpan1);
                        break;
                    case 2:
                        values.add(greatGrandparentSpan2);
                        break;
                    case 3:
                        values.add(greatGrandparentSpan3);
                        break;
                    case 4:
                    case 5:
                        values.add(greatGrandparentSpan45);
                        break;
                    default:
                        values.add(greatGrandparentSpan6);
                        break;
                    }
                } else {
                    // Treat a _very_ shallow tree as span-1
                    values.add(greatGrandparentSpan1);
                }

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
                    } else {
                        values.add(numeralPercentage0);
                    }
                } else {
                    values.add(containsNumeralNegative);
                    values.add(numeralPercentage0);
                }

                // Contains-numeral and numeral percentage
                final float pp = punctuationPercentage(token);
                if (pp > 0) {
                    values.add(containsPunctuationPositive);
                    if (pp == 1f) {
                        values.add(punctuationPercentage100);
                    } else if (pp >= .8f) {
                        values.add(punctuationPercentage80);
                    } else if (pp >= .6f) {
                        values.add(punctuationPercentage60);
                    } else if (pp >= .4f) {
                        values.add(punctuationPercentage40);
                    } else if (pp >= .2f) {
                        values.add(punctuationPercentage20);
                    } else {
                        values.add(punctuationPercentage0);
                    }
                } else {
                    values.add(containsPunctuationNegative);
                    values.add(punctuationPercentage0);
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

    /**
     * A simple set of punctuation characters. This might need expansion for alternate languages or domains.
     * 
     * @param c
     * @return True if the supplied character is a punctuation (according to a very loosely-defined set)
     */
    public static boolean isPunctuation(final char c) {
        // TODO Handle hyphen, comma, and period separately? Hyphen isn't included at all yet
        switch (c) {
        case ',':
        case '.':
        case '!':
        case '?':
        case ':':
        case ';':
        case '/':
        case '\\':
        case '#':
        case '$':
        case '%':
            return true;
        default:
            return false;
        }
    }

    private float punctuationPercentage(final String token) {
        float numerals = 0;
        for (int i = 0; i < token.length(); i++) {
            if (isPunctuation(token.charAt(i))) {
                numerals++;
            }
        }
        return numerals / token.length();
    }

    public static void main(final String[] args) {
        run(args);
    }

}
