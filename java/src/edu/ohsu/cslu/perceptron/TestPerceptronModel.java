package edu.ohsu.cslu.perceptron;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;

/**
 * Unit tests for {@link PerceptronModel}
 * 
 * @author Aaron Dunlop
 * @since Oct 12, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestPerceptronModel {

    @Test
    public void testUpdate() {
        final PerceptronModel p = new PerceptronModel(1, 10f);
        // 10.1, 10.1
        p.update(new SparseBitVector(new int[] { 0 }, false), .1f, 1);
        assertEquals(10.1f, p.rawFeatureWeight(0), .001f);
        assertEquals(10.1f, p.averagedFeatureWeight(0, 2), .001f);

        // 10.2
        p.update(new SparseBitVector(new int[] { 0 }, false), .1f, 3);
        assertEquals(10.2f, p.rawFeatureWeight(0), .001f);
        assertEquals(10.1333f, p.averagedFeatureWeight(0, 3), .001f);
    }

    @Test
    public void testUpdateAveragedModel() {
        final PerceptronModel p = new PerceptronModel(1, 10f);
        // 10, 10, 10
        // p.update(new SparseBitVector(new int[] { 0 }, false), 10, 0);
        assertEquals(10f, p.averagedFeatureWeight(0, 1), .001f);
        assertEquals(10f, p.averagedFeatureWeight(0, 2), .001f);
        assertEquals(10f, p.averagedFeatureWeight(0, 3), .001f);

        // 9, 9, 9
        p.update(new SparseBitVector(new int[] { 0 }, false), -1f, 4);
        assertEquals(9.75f, p.averagedFeatureWeight(0, 4), .001f);
        assertEquals(9.6f, p.averagedFeatureWeight(0, 5), .001f);
        assertEquals(9.5f, p.averagedFeatureWeight(0, 6), .001f);

        // 8, 8
        p.update(new SparseBitVector(new int[] { 0 }, false), -1f, 7);
        assertEquals(9.2857f, p.averagedFeatureWeight(0, 7), .001f);
        assertEquals(9.125f, p.averagedFeatureWeight(0, 8), .001f);

        // 7
        p.update(new SparseBitVector(new int[] { 0 }, false), -1f, 9);
        assertEquals(8.8889f, p.averagedFeatureWeight(0, 9), .001f);

        // Another 7
        p.updateAveragedModel(10);
        assertEquals(8.7f, p.averagedPerceptron().getFloat(0), .001f);

        // Another 2 7's
        p.updateAveragedModel(12);
        assertEquals(8.41666f, p.averagedPerceptron().getFloat(0), .001f);

        // And 2 6's
        p.update(new SparseBitVector(new int[] { 0 }, false), -1f, 13);
        p.updateAveragedModel(14);
        assertEquals(8.0714, p.averagedPerceptron().getFloat(0), .001f);
    }
}
