package edu.ohsu.cslu.alignment;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;

import edu.ohsu.cslu.util.Strings;

/**
 * Simple String vocabulary implementation.
 * 
 * @author Aaron Dunlop
 * @since Oct 9, 2008
 * 
 *        $Id$
 */
public class SimpleVocabulary implements AlignmentVocabulary, Serializable
{
    protected final static int GAP_SYMBOL = 0;
    protected final static String STRING_GAP_SYMBOL = "_-";
    private final static int FIRST_SYMBOL = 1;
    private final Object2IntOpenHashMap<String> token2IndexMap;
    private final String[] tokens;

    protected SimpleVocabulary(String[] tokens)
    {
        this.tokens = tokens;

        this.token2IndexMap = new Object2IntOpenHashMap<String>();
        token2IndexMap.defaultReturnValue(Integer.MIN_VALUE);

        for (int i = 0; i < tokens.length; i++)
        {
            token2IndexMap.put(tokens[i], i);
        }
    }

    public static SimpleVocabulary induce(final String s)
    {
        try
        {
            return induce(new BufferedReader(new StringReader(s)));
        }
        catch (IOException e)
        {
            // We shouldn't ever IOException in a StringReader
            return null;
        }
    }

    public static SimpleVocabulary induce(final BufferedReader reader) throws IOException
    {
        LinkedHashSet<String> tokenList = new LinkedHashSet<String>(128);

        for (String s = reader.readLine(); s != null; s = reader.readLine())
        {
            final String[] tokens = s.split("[( )\r\n]");

            for (String token : tokens)
            {
                if (token.length() > 0 && !tokenList.contains(token))
                {
                    tokenList.add(token);
                }
            }

        }

        String[] tokenArray = new String[FIRST_SYMBOL + tokenList.size()];
        for (int i = 0; i < FIRST_SYMBOL; i++)
        {
            tokenArray[i] = STRING_GAP_SYMBOL;
        }
        int i = FIRST_SYMBOL;
        for (String token : tokenList)
        {
            tokenArray[i++] = token;
        }

        return new SimpleVocabulary(tokenArray);
    }

    public static SimpleVocabulary[] induceVocabularies(final String s)
    {
        try
        {
            return induceVocabularies(new BufferedReader(new StringReader(s)));
        }
        catch (IOException e)
        {
            // We shouldn't ever IOException in a StringReader
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static SimpleVocabulary[] induceVocabularies(final BufferedReader reader) throws IOException
    {
        String line = reader.readLine();
        final int featureCount = line.substring(0, line.indexOf(')')).split(" +").length;

        LinkedHashSet<String>[] featureLists = new LinkedHashSet[featureCount];

        for (int i = 0; i < featureCount; i++)
        {
            featureLists[i] = new LinkedHashSet<String>(128);
            for (int j = 0; j < FIRST_SYMBOL; j++)
            {
                featureLists[i].add(STRING_GAP_SYMBOL);
            }
        }

        int[] indices = new int[featureCount];
        Arrays.fill(indices, FIRST_SYMBOL);

        while (line != null)
        {
            String[] elements = line.split("\\)+ *");
            for (String element : elements)
            {
                String[] features = element.substring(1).split(" +");
                if (features.length != featureCount)
                {
                    throw new IllegalArgumentException("Feature count mismatch on element " + element);
                }

                for (int i = 0; i < features.length; i++)
                {
                    final String feature = features[i];
                    if (feature.length() > 0 && !featureLists[i].contains(feature))
                    {
                        featureLists[i].add(feature);
                    }
                }
            }

            line = reader.readLine();
        }

        SimpleVocabulary[] vocabularies = new SimpleVocabulary[featureCount];
        for (int i = 0; i < featureCount; i++)
        {
            vocabularies[i] = new SimpleVocabulary(featureLists[i].toArray(new String[featureLists[i].size()]));
        }
        return vocabularies;
    }

    @Override
    public int gapSymbol()
    {
        return GAP_SYMBOL;
    }

    @Override
    public String map(int index)
    {
        return tokens[index];
    }

    @Override
    public String[] map(int[] indices)
    {
        if (indices == null)
        {
            return null;
        }

        String[] labels = new String[indices.length];
        for (int i = 0; i < labels.length; i++)
        {
            labels[i] = tokens[indices[i]];
        }
        return labels;
    }

    @Override
    public int map(String token)
    {
        return token2IndexMap.getInt(token);
    }

    @Override
    public int[] map(String[] labels)
    {
        if (labels == null)
        {
            return null;
        }

        int[] indices = new int[labels.length];
        for (int i = 0; i < indices.length; i++)
        {
            indices[i] = token2IndexMap.getInt(labels[i]);
        }
        return indices;
    }

    public static SimpleVocabulary read(final Reader reader) throws IOException
    {
        final BufferedReader br = new BufferedReader(reader);
        Map<String, String> attributes = Strings.headerAttributes(br.readLine());

        int size = Integer.parseInt(attributes.get("size"));
        String[] tokens = new String[size];

        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            if (line.length() > 0)
            {
                String[] split = line.split(" +: +");
                int index = Integer.parseInt(split[0]);
                tokens[index] = split[1];
            }
        }
        return new SimpleVocabulary(tokens);
    }

    protected void writeHeader(Writer writer) throws IOException
    {
        writer.write("vocabulary size=");
        writer.write(Integer.toString(size()));
        writer.write('\n');
    }

    @Override
    public void write(Writer writer) throws IOException
    {
        writeHeader(writer);

        for (int i = 0; i < tokens.length; i++)
        {
            writer.write(Integer.toString(i));
            writer.write(" : ");
            writer.write(tokens[i]);
            writer.write('\n');
        }
    }

    @Override
    public int size()
    {
        return tokens.length;
    }

    @Override
    public String[] tokens()
    {
        return tokens;
    }

    @Override
    public String toString()
    {
        try
        {
            StringWriter writer = new StringWriter(size() * 20);
            write(writer);
            return writer.toString();
        }
        catch (IOException e)
        {
            // Should never IOException writing to a StringWriter
            return null;
        }
    }
}
