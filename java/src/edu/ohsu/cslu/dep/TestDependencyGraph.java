package edu.ohsu.cslu.dep;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import cltool4j.BaseCommandlineTool;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.dep.DependencyGraph.DerivationAction;

/**
 * Unit tests for {@link DependencyGraph}
 * 
 * @author Aaron Dunlop
 */
public class TestDependencyGraph extends BaseCommandlineTool {

    private DependencyGraph conllExample;

    @Before
    public void setUp() throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("1	The	_	DT	DT	_	4	NMOD	_	_\n");
        sb.append("2	luxury	_	NN	NN	_	4	NMOD	_	_\n");
        sb.append("3	auto	_	NN	NN	_	4	NMOD	_	_\n");
        sb.append("4	maker	_	NN	NN	_	7	SBJ	_	_\n");
        sb.append("5	last	_	JJ	JJ	_	6	NMOD	_	_\n");
        sb.append("6	year	_	NN	NN	_	7	VMOD	_	_\n");
        sb.append("7	sold	_	VB	VBD	_	0	ROOT	_	_\n");
        sb.append("8	1,214	_	CD	CD	_	9	NMOD	_	_\n");
        sb.append("9	cars	_	NN	NNS	_	7	OBJ	_	_\n");
        sb.append("10	in	_	IN	IN	_	7	ADV	_	_\n");
        sb.append("11	the	_	DT	DT	_	12	NMOD	_	_\n");
        sb.append("12	U.S.	_	NN	NNP	_	10	PMOD	_	_\n");

        conllExample = DependencyGraph.readConll(sb.toString());
    }

    @Test
    public void testTaggedSequenceConstructor() {
        final StringBuilder sb = new StringBuilder();
        sb.append("1	The	_	_	DT	_	0	_	_	_\n");
        sb.append("2	aged	_	_	NN	_	0	_	_	_\n");
        sb.append("3	bottle	_	_	NN	_	0	_	_	_\n");
        sb.append("4	flies	_	_	NN	_	0	_	_	_\n");
        sb.append("5	fast	_	_	NN	_	0	_	_	_\n");

        final DependencyGraph g = new DependencyGraph("(DT The) (NN aged) (NN bottle) (NN flies) (NN fast)");
        assertEquals(sb.toString(), g.toConllString());
    }

    @Test
    public void testDerivation() throws IOException {

        final StringBuilder sb = new StringBuilder();
        sb.append("1	The	_	DT	DT	_	3	NMOD	_	_\n");
        sb.append("2	aged	_	NN	NN	_	3	NMOD	_	_\n");
        sb.append("3	bottle	_	NN	NN	_	4	NMOD	_	_\n");
        sb.append("4	flies	_	NN	NN	_	0	SBJ	_	_\n");
        sb.append("5	fast	_	NN	NN	_	4	SBJ	_	_\n");

        final DependencyGraph g = DependencyGraph.readConll(sb.toString());
        DependencyGraph.DerivationAction[] derivation = g.derivation();
        assertArrayEquals(new DerivationAction[] { DerivationAction.SHIFT, DerivationAction.SHIFT,
                DerivationAction.SHIFT, DerivationAction.REDUCE_RIGHT, DerivationAction.REDUCE_RIGHT,
                DerivationAction.SHIFT, DerivationAction.REDUCE_RIGHT, DerivationAction.SHIFT,
                DerivationAction.REDUCE_LEFT, DerivationAction.SHIFT, DerivationAction.REDUCE_RIGHT }, derivation);

        derivation = conllExample.derivation();
        assertArrayEquals(new DerivationAction[] { DerivationAction.SHIFT, DerivationAction.SHIFT,
                DerivationAction.SHIFT, DerivationAction.SHIFT, DerivationAction.REDUCE_RIGHT,
                DerivationAction.REDUCE_RIGHT, DerivationAction.REDUCE_RIGHT, DerivationAction.SHIFT,
                DerivationAction.SHIFT, DerivationAction.REDUCE_RIGHT, DerivationAction.SHIFT,
                DerivationAction.REDUCE_RIGHT, DerivationAction.REDUCE_RIGHT, DerivationAction.SHIFT,
                DerivationAction.SHIFT, DerivationAction.REDUCE_RIGHT, DerivationAction.REDUCE_LEFT,
                DerivationAction.SHIFT, DerivationAction.SHIFT, DerivationAction.SHIFT, DerivationAction.REDUCE_RIGHT,
                DerivationAction.REDUCE_LEFT, DerivationAction.REDUCE_LEFT, DerivationAction.SHIFT,
                DerivationAction.REDUCE_RIGHT }, derivation);
    }

    @Test
    public void testTokenTree() {
        final NaryTree<String> tree = conllExample.tokenTree();
        assertEquals("(ROOT (sold (maker The luxury auto) (year last) (cars 1,214) (in (U.S. the))))", tree.toString());
    }

    @Test
    public void testSubtreeScore() {
        for (int i = 0; i < conllExample.size(); i++) {
            conllExample.arcs[i].score = 0.5f;
        }
        final NaryTree<DependencyNode> root = conllExample.goldTree();
        final float subtreeScore = root.label().subtreeScore();
        assertEquals(Math.log(1.0 / 4096), subtreeScore, 0.001);
    }

    @Test
    public void testStart() {
        final NaryTree<DependencyNode> root = conllExample.goldTree();
        assertEquals(0, root.label().start());

        final NaryTree<DependencyNode> sold = root.children().getFirst();
        assertEquals(0, sold.label().start());

        final NaryTree<DependencyNode> year = sold.children().get(1);
        assertEquals(4, year.label().start());

        final NaryTree<DependencyNode> maker = sold.children().getFirst();
        assertEquals(0, maker.label().start());

        final NaryTree<DependencyNode> in = sold.children().getLast();
        assertEquals(9, in.label().start());
    }

    @Test
    public void testSpan() {
        final NaryTree<DependencyNode> root = conllExample.goldTree();
        assertEquals(12, root.label().span());

        final NaryTree<DependencyNode> sold = root.children().getFirst();
        assertEquals(12, sold.label().span());

        final NaryTree<DependencyNode> maker = sold.children().getFirst();
        assertEquals(4, maker.label().span());

        final NaryTree<DependencyNode> in = sold.children().getLast();
        assertEquals(3, in.label().span());
    }

    // @Test
    // public void testDependencyGraphCellSelector() {
    // for (int i = 0; i < conllExample.arcs.length; i++) {
    // conllExample.arcs[i].score = 0.99f;
    // }
    //
    // final DepGraphCellSelectorModel model = new DepGraphCellSelectorModel(new ArrayList<DependencyGraph>(
    // Arrays.asList(conllExample)));
    // final DepGraphCellSelector cellSelector = ((DepGraphCellSelector) model.createCellSelector());
    // final short[] openCells = cellSelector.openCells("The luxury auto maker last year sold 1,214 cars in the U.S.");
    //
    // // A few cells closed by 'The luxury auto maker'
    // assertClosed(openCells, 1, 5);
    // assertClosed(openCells, 2, 5);
    // assertClosed(openCells, 3, 5);
    // assertClosed(openCells, 1, 12);
    // assertClosed(openCells, 2, 12);
    // assertClosed(openCells, 3, 12);
    // // TODO We should actually instantiate a chart and test this
    // // assertEquals(4, cellSelector.getMaxSpan((short) 1, (short) 3));
    //
    // // And a few by '1,214 cars'
    // assertClosed(openCells, 8, 10);
    // assertClosed(openCells, 8, 11);
    // assertClosed(openCells, 8, 12);
    // assertClosed(openCells, 6, 8);
    // assertClosed(openCells, 6, 8);
    // assertClosed(openCells, 5, 8);
    // assertClosed(openCells, 4, 8);
    // assertClosed(openCells, 0, 8);
    //
    // // And finally, the 'in the U.S.' subtree, 9,11 should be closed by the 'the U.S.' arc, but 9,12 should remain
    // // open
    // assertClosed(openCells, 9, 11);
    // assertOpen(openCells, 9, 12);
    // }
    //
    // /**
    // * Fails if the specified start,end pair is found in the open cell array
    // *
    // * @param openCells
    // * @param start
    // * @param end
    // */
    // private void assertClosed(final short[] openCells, final int start, final int end) {
    // for (int k = 0; k < openCells.length; k += 2) {
    // if (openCells[k] == start && openCells[k + 1] == end) {
    // fail("Did not expect " + start + "," + end);
    // }
    // }
    // }
    //
    // /**
    // * Fails if the specified start,end pair is not found in the open cell array
    // *
    // * @param openCells
    // * @param start
    // * @param end
    // */
    // private void assertOpen(final short[] openCells, final int start, final int end) {
    // for (int k = 0; k < openCells.length; k += 2) {
    // if (openCells[k] == start && openCells[k + 1] == end) {
    // return;
    // }
    // }
    // fail("Expected " + start + "," + end);
    // }

    @Override
    protected void run() throws Exception {

        int errors = 0, sentences = 0;
        final BufferedReader br = inputAsBufferedReader();
        for (DependencyGraph g = DependencyGraph.readConll(br); g != null; g = DependencyGraph.readConll(br)) {
            try {
                System.out.println(g.tree());
            } catch (final Exception e) {
                System.err.println("Error on sentence of " + g.size() + " words");
                // e.printStackTrace();
                errors++;
            }
            sentences++;
            System.out.println();
        }
        System.err.println("Total sentences: " + sentences + " Errors: " + errors);
    }

    public static void main(final String[] args) {
        run(args);
    }
}
