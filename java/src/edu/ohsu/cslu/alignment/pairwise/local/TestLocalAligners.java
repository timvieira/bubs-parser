package edu.ohsu.cslu.alignment.pairwise.local;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class TestLocalAligners
{
    @Test
    public void testDynamicAligner()
    {
        LocalAligner aligner = new FullDynamicLocalAligner();
        exactAlignmentTest(aligner);
        approximateAlignmentTest(aligner);
        localAlignmentTest(aligner);

        String text, pattern;

        text = "abcabc";
        pattern = "abcabc";
        java.util.Set<edu.ohsu.cslu.alignment.pairwise.local.TextAlignment> alignments = aligner.alignments(pattern, text, 3, 0);
        assertEquals("Wrong number of alignments found for 'abcabc'", 3, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 3, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 6, 0, 6)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(3, 6, 0, 3)));
    }

    @Test
    public void testSelfAlignment()
    {
        FullDynamicLocalAligner aligner = new FullDynamicLocalAligner();
        exactAlignmentTest(aligner);
        approximateAlignmentTest(aligner);
        localAlignmentTest(aligner);

        String text = "abcabc";
        java.util.Set<edu.ohsu.cslu.alignment.pairwise.local.TextAlignment> alignments = aligner.selfAlignments(text, 3, 0);
        assertEquals("Wrong number of alignments found for 'abcabc'", 2, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 3, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(3, 6, 0, 3)));

        text = "abcabcabc";
        alignments = aligner.selfAlignments(text, 6, 0);
        assertEquals("Wrong number of alignments found for 'abcabc'", 2, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 6, 0, 6)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(3, 9, 0, 6)));
    }

    private void exactAlignmentTest(LocalAligner aligner)
    {
        String text, pattern;

        text = "tpyxtpzxtpyxtpr";
        pattern = "tpr";
        java.util.Set<edu.ohsu.cslu.alignment.pairwise.local.TextAlignment> alignments = aligner.alignments(pattern, text, 0);
        assertEquals("Wrong number of alignments found for 'tpr'", 1, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(12, 15, 0, 3)));

        text = "cbababcababc";
        pattern = "abab";
        alignments = aligner.alignments(pattern, text, 0);
        assertEquals("Wrong number of alignments found for 'abab'", 2, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(2, 6, 0, 4)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(7, 11, 0, 4)));

        text = "aaaaa";
        pattern = "aaa";
        alignments = aligner.alignments(pattern, text, 0);
        assertEquals("Wrong number of alignments found for 'aaa'", 3, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 3, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(1, 4, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(2, 5, 0, 3)));
    }

    private void approximateAlignmentTest(LocalAligner aligner)
    {
        String text, pattern;

        text = "tpyxtpzxtpyxtpr";
        pattern = "tpr";
        java.util.Set<edu.ohsu.cslu.alignment.pairwise.local.TextAlignment> alignments = aligner.alignments(pattern, text, 1);
        assertEquals("Wrong number of alignments found for 'tpr'", 4, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 3, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(4, 7, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(8, 11, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(12, 15, 0, 3)));

        text = "cbababcababc";
        pattern = "abab";
        alignments = aligner.alignments(pattern, text, 1);
        assertEquals("Wrong number of alignments found for 'abab'", 4, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 4, 0, 4)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(2, 6, 0, 4)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(4, 9, 0, 4)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(7, 11, 0, 4)));
    }

    private void localAlignmentTest(LocalAligner aligner)
    {
        String text, pattern;

        text = "abcdabcd";
        pattern = "abcd";
        java.util.Set<edu.ohsu.cslu.alignment.pairwise.local.TextAlignment> alignments = aligner.alignments(pattern, text, 4, 0);
        assertEquals("Wrong number of alignments found for 'abcd'", 2, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 4, 0, 4)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(4, 8, 0, 4)));

        text = "abcabcabc";
        pattern = "abcd";
        alignments = aligner.alignments(pattern, text, 3, 0);
        assertEquals("Wrong number of alignments found for 'abc'", 3, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 3, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(3, 6, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(6, 9, 0, 3)));

        text = "tpyxtpzxtpyxtpr";
        pattern = "tpr";
        alignments = aligner.alignments(pattern, text, 1);
        assertEquals("Wrong number of alignments found for 'tpr'", 4, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 3, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(4, 7, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(8, 11, 0, 3)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(12, 15, 0, 3)));

        text = "cbababcababc";
        pattern = "abab";
        alignments = aligner.alignments(pattern, text, 1);
        assertEquals("Wrong number of alignments found for 'abab'", 4, alignments.size());
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(0, 4, 0, 4)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(2, 6, 0, 4)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(5, 9, 0, 4)));
        assertTrue("Wrong alignment", alignments.contains(new TextAlignment(7, 11, 0, 4)));
    }
}
