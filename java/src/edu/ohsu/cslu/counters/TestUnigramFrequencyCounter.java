package edu.ohsu.cslu.counters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.tests.FilteredRunner;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests for {@link UnigramFrequencyCounter}
 * 
 * @author Aaron Dunlop
 * @since Jun 29, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestUnigramFrequencyCounter {

    private final String document1 = "In an Oct. 19 review of The Misanthrope at Chicago's Goodman Theatre,"
            + " (Revitalized Classics Take the Stage in Windy City, Leisure & Arts), the role of Celimene,"
            + " played by Kim Cattral, was mistakenly attributed to Christina Haag.";
    private final String document2 = "Ms. Haag plays Elianti while Ms. Cattral plays Celimene.";
    private final String document3 = "Ms. Cattral made her debut in 1996.";

    @Test
    public void testTfIdf() throws Exception {
        checkCounter(countDocuments());
    }

    private UnigramFrequencyCounter countDocuments() {
        UnigramFrequencyCounter counter = new UnigramFrequencyCounter();
        counter.addDocument(document1);
        counter.addDocument(document2);
        counter.addDocument(document3);
        return counter;
    }

    private void checkCounter(UnigramFrequencyCounter counter) {
        assertEquals(-.2876f, counter.logIdf("Cattral"), .01f);
        assertEquals(0f, counter.logIdf("Haag"), .01f);
        assertEquals(.4055f, counter.logIdf("1996"), .01f);
    }

    @Test
    public void testSerialize() throws Exception {
        // Serialize a counter
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(countDocuments());

        // Read in the counter and verify it.
        UnigramFrequencyCounter counter = (UnigramFrequencyCounter) new ObjectInputStream(
            new ByteArrayInputStream(bos.toByteArray())).readObject();
        checkCounter(counter);
    }
}
