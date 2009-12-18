package edu.ohsu.cslu.common;

public interface MappedSequence extends Sequence {

    /**
     * @return the vocabularies used to map this sequence
     */
    public Vocabulary[] vocabularies();

    // Type-strengthen return type
    @Override
    public MappedSequence retainFeatures(int... features);

    // Type-strengthen return type
    @Override
    public Sequence subSequence(int beginIndex, int endIndex);

    // Type-strengthen return type
    @Override
    public MappedSequence[] splitIntoSentences();

    // Type-strengthen return type
    @Override
    public MappedSequence insertGaps(int[] gapIndices);

    // Type-strengthen return type
    @Override
    public MappedSequence removeAllGaps();

    /**
     * Formats this sequence as a String, using the supplied formatting Strings
     * 
     * @param formats
     * @return String visualization of the sequence
     */
    public String toColumnString(String[] formats);

    /**
     * @param index
     * @return the length of the longest string label in the specified position
     */
    public int maxLabelLength(int index);
}
