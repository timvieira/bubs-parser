/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */

package edu.ohsu.cslu.tools;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.util.Arff;
import edu.ohsu.cslu.util.Strings;

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

        final StringBuilder header = new StringBuilder();
        header.append("@relation RareWords\n");
        header.append("\n");
        header.append("@attribute Token string\n");
        header.append("@attribute SentenceIndex numeric\n");
        header.append("@attribute WordIndex numeric\n");
        header.append("@attribute OccurrenceCount numeric\n");
        header.append("@attribute Pos-1 string\n");
        header.append("@attribute Pos string\n");
        header.append("@attribute Pos+1 string\n");
        header.append("@attribute GrandparentLabel string\n");
        header.append("@attribute GreatGrandparentLabel string\n");
        header.append("@attribute GrandparentSpan {1,2,3,45,6+}\n");
        header.append("@attribute GreatGrandparentSpan {1,2,3,45,6+}\n");
        header.append("@attribute UnigramSuffix string\n");
        header.append("@attribute BigramSuffix string\n");
        header.append("@attribute ContainsNumeral {+num,-num}\n");
        header.append("@attribute NumeralPercentage {0,20+,40+,60+,80+,100}\n");
        header.append("@attribute ContainsPunctuation {+punct,-punct}\n");
        header.append("@attribute PunctuationPercentage {0,20+,40+,60+,80+,100}\n");
        header.append("@attribute Contains@ {+@,-@}\n");
        header.append("@attribute StartsWith# {+#-start,-#-start}\n");
        header.append("@attribute StartsWithHttp {+http-start,-http-start}\n");
        System.out.print(header.toString());
        System.out.println();
        System.out.println("@data");

        // Read the entire corpus and count token occurrences
        final BufferedReader br = inputAsBufferedReader();
        // Allow re-reading up to 50 MB
        br.mark(50 * 1024 * 1024);

        // Maps a string key (POS|token) to the number of times that combination was observed in the training corpus
        final Object2IntOpenHashMap<String> observationCounts = countTokenOccurrences(br);

        // Reset the reader and reread the corpus, this time applying appropriate normalizations and outputting each
        // tree
        br.reset();

        int sentenceIndex = 0;
        for (final String line : inputLines(br)) {
            final NaryTree<String> tree = NaryTree.read(line, String.class);
            sentenceIndex++;

            int wordIndex = 0;
            for (final ListIterator<NaryTree<String>> iter = tree.leafList().listIterator(); iter.hasNext();) {

                final NaryTree<String> leaf = iter.next();
                final String token = leaf.label();
                wordIndex++;

                // Include only tokens observed in the training corpus <= threshold times with the current POS
                final int observationCount = observationCounts.getInt(key(leaf));
                if (!thresholdMap.containsKey(leaf.parentLabel())
                        || observationCount > thresholdMap.getInt(leaf.parentLabel())) {
                    continue;
                }

                final StringBuilder sb = new StringBuilder();
                sb.append(Arff.escape(token));
                sb.append(',');

                // Sentence and word index
                sb.append(Integer.toString(sentenceIndex));
                sb.append(',');
                sb.append(Integer.toString(wordIndex));
                sb.append(',');

                // Occurrence count
                sb.append(Integer.toString(observationCount));
                sb.append(',');

                // Previous POS label
                try {
                    iter.previous();
                    sb.append(Arff.escape(iter.previous().parentLabel()));
                    iter.next();
                } catch (final NoSuchElementException e) {
                    sb.append(NULL);
                }
                sb.append(',');

                // Current POS label
                sb.append(Arff.escape(iter.next().parentLabel()));
                sb.append(',');

                // Next POS label
                try {
                    sb.append(Arff.escape(iter.next().parentLabel()));
                    iter.previous();
                } catch (final NoSuchElementException e) {
                    sb.append(NULL);
                }
                sb.append(',');

                // Grandparent label (the parent of the POS)
                final NaryTree<String> grandparent = leaf.parent().parent();
                NaryTree<String> greatGrandparent = null;
                if (grandparent != null) {
                    sb.append(Arff.escape(grandparent.label()));
                    sb.append(',');

                    // Great-grandparent label
                    greatGrandparent = grandparent.parent();
                    if (greatGrandparent != null) {
                        sb.append(Arff.escape(greatGrandparent.label()));
                    } else {
                        sb.append(NULL);
                    }
                    sb.append(',');
                } else {
                    sb.append(NULL);
                    sb.append(',');
                    sb.append(NULL);
                    sb.append(',');
                }

                // Grandparent span
                if (grandparent != null) {
                    switch (grandparent.leaves()) {
                    case 1:
                    case 2:
                    case 3:
                        sb.append(Integer.toString(grandparent.leaves()));
                        break;
                    case 4:
                    case 5:
                        sb.append("45");
                        break;
                    default:
                        sb.append("6+");
                        break;
                    }
                } else {
                    // Treat a _very_ shallow tree as span-1
                    sb.append("1");
                }
                sb.append(',');

                // Great-grandparent span
                if (greatGrandparent != null) {
                    switch (greatGrandparent.leaves()) {
                    case 1:
                    case 2:
                    case 3:
                        sb.append(Integer.toString(greatGrandparent.leaves()));
                        break;
                    case 4:
                    case 5:
                        sb.append("45");
                        break;
                    default:
                        sb.append("6+");
                        break;
                    }
                } else {
                    // Treat a _very_ shallow tree as span-1
                    sb.append("1");
                }
                sb.append(',');

                // Unigram suffix
                sb.append(Arff.escape(token.substring(token.length() - 1)));
                sb.append(',');

                // Bigram suffix
                if (token.length() >= 2) {
                    sb.append(Arff.escape(token.substring(token.length() - 2)));
                }
                sb.append(',');

                // Contains-numeral and numeral percentage
                final float np = Strings.numeralPercentage(token);
                if (np > 0) {
                    sb.append("+num");
                    sb.append(',');

                    if (np == 1f) {
                        sb.append("100");
                    } else if (np >= .8f) {
                        sb.append("80+");
                    } else if (np >= .6f) {
                        sb.append("60+");
                    } else if (np >= .4f) {
                        sb.append("40+");
                    } else if (np >= .2f) {
                        sb.append("20+");
                    } else {
                        sb.append("0");
                    }
                    sb.append(',');

                } else {
                    sb.append("-num");
                    sb.append(',');
                    sb.append("0");
                    sb.append(',');
                }

                // Contains-numeral and numeral percentage
                final float pp = Strings.punctuationPercentage(token);
                if (pp > 0) {
                    sb.append("+punct");
                    sb.append(',');
                    if (pp == 1f) {
                        sb.append("100");
                    } else if (pp >= .8f) {
                        sb.append("80+");
                    } else if (pp >= .6f) {
                        sb.append("60+");
                    } else if (pp >= .4f) {
                        sb.append("40+");
                    } else if (pp >= .2f) {
                        sb.append("20+");
                    } else {
                        sb.append("0");
                    }
                    sb.append(',');

                } else {
                    sb.append("-punct");
                    sb.append(',');
                    sb.append("0");
                    sb.append(',');
                }

                sb.append(token.indexOf('@') >= 0 ? "+@," : "-@,");
                sb.append(token.indexOf('#') == 0 ? "+#-start," : "-#-start,");
                sb.append(token.startsWith("http") ? "+http-start" : "-http-start");

                System.out.println(sb.toString());
            }
        }
    }

    public static void main(final String[] args) {
        run(args);
    }

}
