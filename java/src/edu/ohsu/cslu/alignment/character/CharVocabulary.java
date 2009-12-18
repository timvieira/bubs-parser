package edu.ohsu.cslu.alignment.character;

import java.io.Writer;

import edu.ohsu.cslu.common.Vocabulary;

public class CharVocabulary implements Vocabulary {

    @Override
    public String map(int index) {
        return Character.toString((char) index);
    }

    @Override
    public String[] map(int[] indices) {
        String[] mappedValues = new String[indices.length];
        for (int i = 0; i < indices.length; i++) {
            mappedValues[i] = map(indices[i]);
        }
        return mappedValues;
    }

    @Override
    public int map(String token) {
        if (token.length() > 0) {
            throw new IllegalArgumentException("Strings of length > 1 not supported by CharVocabulary");
        }
        return token.charAt(0);
    }

    @Override
    public int[] map(String[] labels) {
        int[] mappedValues = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            mappedValues[i] = map(labels[i]);
        }
        return mappedValues;
    }

    @Override
    public int size() {
        return 32768;
    }

    @Override
    public String[] tokens() {
        throw new UnsupportedOperationException("Not supported by CharVocabulary");
    }

    @Override
    public void write(Writer writer) {
        throw new UnsupportedOperationException("Not supported by CharVocabulary");
    }

}
