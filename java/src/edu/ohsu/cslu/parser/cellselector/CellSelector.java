package edu.ohsu.cslu.parser.cellselector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Iterator;

import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.util.Log;

public abstract class CellSelector implements Iterator<short[]> {

    public CellSelectorType type;

    static public enum CellSelectorType {
        LeftRightBottomTop, LeftCorner, CSLUT, Perceptron
    }

    public abstract boolean hasNext();

    public abstract short[] next();

    public void init(final ChartParser<?, ?> parser) {
        // default is to do nothing
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public static CellSelector create(final CellSelectorType type) throws Exception {
        return create(type, null, null);
    }

    public static CellSelector create(final CellSelectorType type, final BufferedReader modelStream,
            final BufferedReader cslutScoresStream) {
        CellSelector spanSelection;
        switch (type) {
        case LeftRightBottomTop:
            spanSelection = new LeftRightBottomTopTraversal();
            break;
        case LeftCorner:
            spanSelection = new LeftCornerTraversal();
            break;
        case CSLUT:
            spanSelection = new CSLUTBlockedCells(modelStream);
            break;
        case Perceptron:
            spanSelection = new PerceptronCellSelector(modelStream, cslutScoresStream);
            break;
        default:
            Log.info(0, "ERROR: CellSelectorType " + type + " not supported.");
            System.exit(1);
            return null;
        }
        spanSelection.type = type;
        return spanSelection;
    }

    public void train(final BufferedReader inStream) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void readModel(final BufferedReader inStream) throws Exception {
        throw new UnsupportedOperationException();
    }

    public void writeModel(final BufferedWriter outStream) throws Exception {
        throw new UnsupportedOperationException();
    }
}
