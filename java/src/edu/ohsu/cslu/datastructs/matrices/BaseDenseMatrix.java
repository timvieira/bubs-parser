package edu.ohsu.cslu.datastructs.matrices;

import java.io.IOException;
import java.io.Writer;

public abstract class BaseDenseMatrix extends BaseMatrix implements DenseMatrix {

    BaseDenseMatrix(final int m, final int n, final boolean symmetric) {
        super(m, n, symmetric);
    }

    @Override
    public float[] getRow(final int i) {
        final float[] row = new float[n];
        for (int j = 0; j < n; j++) {
            row[j] = getFloat(i, j);
        }
        return row;
    }

    @Override
    public int[] getIntRow(final int i) {
        final int[] row = new int[n];
        for (int j = 0; j < n; j++) {
            row[j] = getInt(i, j);
        }
        return row;
    }

    @Override
    public float[] getColumn(final int j) {
        final float[] column = new float[m];
        for (int i = 0; i < m; i++) {
            column[i] = getFloat(i, j);
        }
        return column;
    }

    @Override
    public int[] getIntColumn(final int j) {
        final int[] column = new int[m];
        for (int i = 0; i < m; i++) {
            column[i] = getInt(i, j);
        }
        return column;
    }

    @Override
    public void setRow(final int i, final float[] newRow) {
        for (int j = 0; j < n; j++) {
            set(i, j, newRow[j]);
        }
    }

    @Override
    public void setRow(final int i, final float value) {
        for (int j = 0; j < n; j++) {
            set(i, j, value);
        }
    }

    @Override
    public void setRow(final int i, final int[] newRow) {
        for (int j = 0; j < n; j++) {
            set(i, j, newRow[j]);
        }
    }

    @Override
    public void setRow(final int i, final int value) {
        for (int j = 0; j < n; j++) {
            set(i, j, value);
        }
    }

    @Override
    public void setColumn(final int j, final float[] newColumn) {
        for (int i = 0; i < m; i++) {
            set(i, j, newColumn[i]);
        }
    }

    @Override
    public void setColumn(final int j, final float value) {
        for (int i = 0; i < m; i++) {
            set(i, j, value);
        }
    }

    @Override
    public void setColumn(final int j, final int[] newColumn) {
        for (int i = 0; i < m; i++) {
            set(i, j, newColumn[i]);
        }
    }

    @Override
    public void setColumn(final int j, final int value) {
        for (int i = 0; i < m; i++) {
            set(i, j, value);
        }
    }

    protected void write(final Writer writer, final String format) throws IOException {
        // Write Matrix contents
        for (int i = 0; i < m; i++) {
            final int maxJ = symmetric ? i : n - 1;
            for (int j = 0; j < maxJ; j++) {
                writer.write(String.format(format, getFloat(i, j)));
                writer.write(' ');
            }

            // Handle the end-of-line string separately
            String eolString = String.format(format, getFloat(i, maxJ));

            // Trim spaces from the end of the string
            while (eolString.charAt(eolString.length() - 1) == ' ') {
                eolString = eolString.substring(0, eolString.length() - 1);
            }

            writer.write(eolString);
            writer.write('\n');
        }
        writer.flush();
    }
}
