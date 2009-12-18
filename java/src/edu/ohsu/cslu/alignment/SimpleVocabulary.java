package edu.ohsu.cslu.alignment;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import edu.ohsu.cslu.common.FeatureClass;
import edu.ohsu.cslu.util.Strings;

/**
 * Simple String vocabulary implementation.
 * 
 * @author Aaron Dunlop
 * @since Oct 9, 2008
 * 
 *        $Id$
 */
public class SimpleVocabulary implements AlignmentVocabulary, Serializable {

    public final static int GAP_SYMBOL = 0;
    public final static String STRING_GAP_SYMBOL = FeatureClass.Gap.toString();
    public final static int UNKNOWN_SYMBOL = 1;
    public final static String STRING_UNKNOWN_SYMBOL = FeatureClass.Unknown.toString();
    protected final static String[] STATIC_SYMBOLS = new String[] { STRING_GAP_SYMBOL, STRING_UNKNOWN_SYMBOL };
    private final Object2IntOpenHashMap<String> token2IndexMap;
    private final String[] tokens;
    private final HashSet<String> rareTokens;
    private final IntSet rareIndices = new IntOpenHashSet();

    protected SimpleVocabulary(String[] tokens) {
        this(tokens, new HashSet<String>());
    }

    protected SimpleVocabulary(String[] tokens, HashSet<String> rareTokens) {
        this.tokens = tokens;
        this.rareTokens = rareTokens;

        this.token2IndexMap = new Object2IntOpenHashMap<String>();
        token2IndexMap.defaultReturnValue(Integer.MIN_VALUE);

        for (int i = 0; i < tokens.length; i++) {
            token2IndexMap.put(tokens[i], i);
            if (rareTokens.contains(tokens[i])) {
                rareIndices.add(i);
            }
        }
    }

    public static SimpleVocabulary induce(final String s) {
        try {
            return induce(new BufferedReader(new StringReader(s)));
        } catch (IOException e) {
            // We shouldn't ever IOException in a StringReader
            return null;
        }
    }

    public static SimpleVocabulary induce(final String s, final int rareTokenCutoff) {
        try {
            return induce(new BufferedReader(new StringReader(s)), rareTokenCutoff);
        } catch (IOException e) {
            // We shouldn't ever IOException in a StringReader
            return null;
        }
    }

    public static SimpleVocabulary induce(final BufferedReader reader) throws IOException {
        return induce(reader, 0);
    }

    public static SimpleVocabulary induce(final BufferedReader reader, int rareTokenCutoff)
            throws IOException {
        LinkedHashMap<String, Integer> tokenMap = new LinkedHashMap<String, Integer>(128);

        for (String s = reader.readLine(); s != null; s = reader.readLine()) {
            final String[] tokens = s.split("[( )\r\n]");

            for (String token : tokens) {
                if (token.length() > 0) {
                    Integer count = tokenMap.get(token);
                    if (count == null) {
                        tokenMap.put(token, 1);
                    } else {
                        tokenMap.put(token, count.intValue() + 1);
                    }
                }
            }
        }

        final HashSet<String> rareTokens = new HashSet<String>();

        String[] tokenArray = new String[STATIC_SYMBOLS.length + tokenMap.size()];
        int i;
        for (i = 0; i < STATIC_SYMBOLS.length; i++) {
            tokenArray[i] = STATIC_SYMBOLS[i];
        }
        for (String token : tokenMap.keySet()) {
            tokenArray[i++] = token;
            if (tokenMap.get(token) <= rareTokenCutoff) {
                rareTokens.add(token);
            }
        }

        return new SimpleVocabulary(tokenArray, rareTokens);
    }

    public static SimpleVocabulary[] induceVocabularies(final String s) {
        try {
            return induceVocabularies(new BufferedReader(new StringReader(s)));
        } catch (IOException e) {
            // We shouldn't ever IOException in a StringReader
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static SimpleVocabulary[] induceVocabularies(final BufferedReader reader) throws IOException {
        String line = reader.readLine();
        final int featureCount = line.substring(0, line.indexOf(')')).split(" +").length;

        LinkedHashSet<String>[] featureLists = new LinkedHashSet[featureCount];

        for (int i = 0; i < featureCount; i++) {
            featureLists[i] = new LinkedHashSet<String>(128);
            for (int j = 0; j < STATIC_SYMBOLS.length; j++) {
                featureLists[i].add(STATIC_SYMBOLS[j]);
            }
        }

        int[] indices = new int[featureCount];
        Arrays.fill(indices, STATIC_SYMBOLS.length);

        while (line != null) {
            String[] elements = line.split("\\)+ *");
            for (String element : elements) {
                String[] features = element.substring(1).split(" +");
                if (features.length != featureCount) {
                    throw new IllegalArgumentException("Feature count mismatch on element " + element);
                }

                for (int i = 0; i < features.length; i++) {
                    final String feature = features[i];
                    if (feature.length() > 0 && !featureLists[i].contains(feature)) {
                        featureLists[i].add(feature);
                    }
                }
            }

            line = reader.readLine();
        }

        SimpleVocabulary[] vocabularies = new SimpleVocabulary[featureCount];
        for (int i = 0; i < featureCount; i++) {
            vocabularies[i] = new SimpleVocabulary(featureLists[i]
                .toArray(new String[featureLists[i].size()]));
        }
        return vocabularies;
    }

    @Override
    public int gapSymbol() {
        return GAP_SYMBOL;
    }

    @Override
    public String map(int index) {
        return tokens[index];
    }

    @Override
    public boolean isRareToken(int index) {
        return rareIndices.contains(index);
    }

    @Override
    public String[] map(int[] indices) {
        if (indices == null) {
            return null;
        }

        String[] labels = new String[indices.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = tokens[indices[i]];
        }
        return labels;
    }

    @Override
    public int map(String token) {
        return token2IndexMap.getInt(token);
    }

    @Override
    public boolean isRareToken(String token) {
        return rareTokens.contains(token);
    }

    @Override
    public int[] map(String[] labels) {
        if (labels == null) {
            return null;
        }

        int[] indices = new int[labels.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = token2IndexMap.getInt(labels[i]);
        }
        return indices;
    }

    public static SimpleVocabulary read(final Reader reader) throws IOException {
        final BufferedReader br = new BufferedReader(reader);
        final Map<String, String> attributes = Strings.headerAttributes(br.readLine());
        final HashSet<String> rareTokens = new HashSet<String>();

        int size = Integer.parseInt(attributes.get("size"));
        String[] tokens = new String[size];

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.length() > 0) {
                String[] split = line.split(" +: +");
                int index = Integer.parseInt(split[0]);
                tokens[index] = split[1];
                if (split[2].equals("true")) {
                    rareTokens.add(split[1]);
                }
            }
        }
        return new SimpleVocabulary(tokens, rareTokens);
    }

    protected void writeHeader(Writer writer) throws IOException {
        writer.write("vocabulary size=");
        writer.write(Integer.toString(size()));
        writer.write('\n');
    }

    @Override
    public void write(Writer writer) throws IOException {
        writeHeader(writer);

        for (int i = 0; i < tokens.length; i++) {
            writer.write(Integer.toString(i));
            writer.write(" : ");
            writer.write(tokens[i]);
            writer.write(" : ");
            writer.write(rareIndices.contains(i) ? "true" : "false");
            writer.write('\n');
        }
    }

    @Override
    public int size() {
        return tokens.length;
    }

    @Override
    public String[] tokens() {
        return tokens;
    }

    @Override
    public String toString() {
        try {
            StringWriter writer = new StringWriter(size() * 20);
            write(writer);
            return writer.toString();
        } catch (IOException e) {
            // Should never IOException writing to a StringWriter
            return null;
        }
    }
}
