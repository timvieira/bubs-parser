package edu.ohsu.cslu.parser;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

import edu.ohsu.cslu.grammar.Grammar;

public class Chart<T extends ChartCell> {

    private T chart[][];
    private int size;
    public Grammar grammar;

    // TODO: this class shouldn't have to know anything about the grammar. We need
    // to separate the two
    @SuppressWarnings("unchecked")
    public Chart(final int size, final Class<T> chartCellClass, final Grammar grammar) {
        this.size = size;
        this.grammar = grammar;

        try {
            // final Class<T> chartCellClass = (Class<T>) ((ParameterizedType) getClass()).getActualTypeArguments()[0];
            final Constructor<T> chartCellConstructor = chartCellClass.getConstructor(int.class, int.class, Chart.class);

            chart = (T[][]) Array.newInstance(chartCellClass, size, size + 1);// new T[size][size + 1];

            // The chart is (chartSize+1)*chartSize/2
            for (int start = 0; start < size; start++) {
                for (int end = start + 1; end < size + 1; end++) {
                    chart[start][end] = chartCellConstructor.newInstance(start, end, this);
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

    }

    // private T getRuntimeInstance() {
    //        
    //
    // // if (runtimeType instanceof ArrayChartCell) {
    // // chart[start][end] = new ArrayChartCell(start, end, this);
    // // } else if (runtimeType instanceof )
    //        
    // final Class<T> chartCellClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    // Constructor<T> c;
    // try {
    // c = chartCellClass.getConstructor(int.class, int.class, Chart.class);
    // chart[start][end] = c.newInstance(start, end, this);
    // } catch (final Exception e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }

    public T getCell(final int start, final int end) {
        return chart[start][end];
    }

    public int size() {
        return size;
    }

    public T getRootCell() {
        return chart[0][size];
    }
}
