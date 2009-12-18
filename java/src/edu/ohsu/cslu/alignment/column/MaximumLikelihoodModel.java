package edu.ohsu.cslu.alignment.column;

import java.io.BufferedReader;
import java.io.IOException;

import edu.ohsu.cslu.alignment.CharVocabulary;
import edu.ohsu.cslu.datastructs.matrices.FloatMatrix;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;

/**
 * A simple unsmoothed maximum-likelihood model
 * 
 * TODO: Refactor to share more code with LaplaceModel
 * 
 * @author Aaron Dunlop
 * @since Jan 21, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class MaximumLikelihoodModel extends MatrixColumnAlignmentModel {

    public MaximumLikelihoodModel(java.io.Reader trainingData, CharVocabulary vocabulary,
            boolean ignoreLabelLines) throws IOException {
        super(vocabulary);

        BufferedReader br = new BufferedReader(trainingData);

        String line = br.readLine();
        if (ignoreLabelLines) {
            // Discard label line
            line = br.readLine();
        }

        final int rows = vocabulary.size();
        final int columns = line.length();

        matrices[0] = new FloatMatrix(rows, columns, false);
        IntMatrix counts = new IntMatrix(vocabulary.size(), columns);
        countLine(vocabulary, line, columns, counts);
        // We already counted one line
        int totalCount = 1;

        for (line = br.readLine(); line != null; line = br.readLine()) {
            if (ignoreLabelLines) {
                // Discard label line
                line = br.readLine();
            }
            countLine(vocabulary, line, columns, counts);
            totalCount++;
        }
        trainingData.close();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                matrices[0].set(i, j, (float) -Math.log(counts.getFloat(i, j) / totalCount));
            }
        }
    }

    private void countLine(CharVocabulary vocabulary, String sequence, final int columns, IntMatrix counts) {
        for (int j = 0; j < columns; j++) {
            counts.increment(vocabulary.mapCharacter(sequence.charAt(j)), j);
        }
    }
}
