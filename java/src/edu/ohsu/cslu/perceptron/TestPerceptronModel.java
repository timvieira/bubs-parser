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

/*

    It seems that listing a bunch of assertEquals statments is a time 
    consuming process and takes a lot of manual work to update/change 
    if code changes.  Instead, what if we create toy problems and 
    embed the data files in the test file (ie. a string) and then run
    a number of tests and compare the string output (at different
    levels).  If code is updated, we can just run the program again
    and copy the output into the "correct" output field for later tests.
    
    Learning the NOR problem
  
    Input:
        gold/bias/x1/x2
        1 1 0 0
        1 1 0 1
        1 1 1 0
        0 1 1 1

    Output:
        raw=0.100 0.000 0.000    avg=0.100 0.000 0.000
        raw=0.100 0.000 0.000    avg=0.100 0.000 0.000
        raw=0.100 0.000 0.000    avg=0.100 0.000 0.000
        raw=0.000 -0.100 -0.100  avg=0.075 -0.025 -0.025
        ittr=0   trainAcc=0.25
        
        raw=0.100 -0.100 -0.100  avg=0.080 -0.025 -0.025
        raw=0.200 -0.100 0.000   avg=0.100 -0.025 -0.033
        raw=0.200 -0.100 0.000   avg=0.100 -0.025 -0.033
        raw=0.100 -0.200 -0.100  avg=0.112 -0.075 -0.038
        ittr=1   trainAcc=0.50 

    
*/
        
    
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
