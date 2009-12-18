package edu.ohsu.cslu.alignment.column;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import edu.ohsu.cslu.alignment.CharVocabulary;
import edu.ohsu.cslu.alignment.multiple.MultipleSequenceAlignment;
import edu.ohsu.cslu.datastructs.matrices.FloatMatrix;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;

/**
 * Models the training data, adding in the specified number of 'pseudo' counts. Specifying 0 yields a Maximum
 * Likelihood model.
 * 
 * @author Aaron Dunlop
 * @since Jun 30, 2008
 * 
 *        $Id$
 */
public final class LaplaceModel extends MatrixColumnAlignmentModel {

    public LaplaceModel(Reader trainingData, CharVocabulary vocabulary, int pseudoCounts,
            boolean ignoreLabelLines) throws IOException {
        super(vocabulary);

        BufferedReader br = new BufferedReader(trainingData);

        String line = br.readLine();
        if (ignoreLabelLines) {
            line = br.readLine();
        }

        final int rows = vocabulary.size();
        final int columns = line.length();

        matrices[0] = new FloatMatrix(rows, columns, false);
        IntMatrix counts = new IntMatrix(vocabulary.size(), columns, false);

        countLine(vocabulary, line, columns, counts);
        // We already counted one line
        int totalCount = 1 + pseudoCounts;

        for (line = br.readLine(); line != null; line = br.readLine()) {
            if (ignoreLabelLines) {
                // Discard label line
                line = br.readLine();
            }
            countLine(vocabulary, line, columns, counts);
            totalCount++;
        }
        trainingData.close();

        float pseudoCountsPerChar = ((float) pseudoCounts) / vocabulary.size();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrices[0].set(i, j, (float) -Math.log((counts.getFloat(i, j) + pseudoCountsPerChar)
                        / totalCount));
            }
        }
    }

    public LaplaceModel(MultipleSequenceAlignment trainingData, CharVocabulary vocabulary, int pseudoCounts) {
        super(null);
        throw new UnsupportedOperationException("LaplaceModel does not currently support Sequences");
    }

    private void countLine(CharVocabulary vocabulary, String sequence, final int columns, IntMatrix counts) {
        for (int j = 0; j < columns; j++) {
            counts.increment(vocabulary.mapCharacter(sequence.charAt(j)), j);
        }
    }
}
