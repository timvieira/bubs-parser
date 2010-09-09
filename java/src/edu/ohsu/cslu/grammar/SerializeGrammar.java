package edu.ohsu.cslu.grammar;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.BitVectorExactFilterFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectHashFilterFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.SimpleShiftFunction;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.UnfilteredFunction;
import edu.ohsu.cslu.parser.spmv.SparseMatrixVectorParser.CartesianProductFunctionType;

/**
 * Command-line tool to convert textual grammar formats to Java serialized object format.
 * 
 * @author Aaron Dunlop
 * @since Sep 9, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class SerializeGrammar extends BaseCommandlineTool {

    @Argument(index = 0, metaVar = "grammar", usage = "Grammar file (text, gzipped text, or binary serialized")
    private String inputGrammarFile;

    @Option(name = "-c", required = true, metaVar = "classname", usage = "Grammar class")
    private String grammarClass;

    @Option(name = "-cpf", aliases = { "--cartesian-product-function" }, metaVar = "function", usage = "Cartesian-product function (only used for sparse matrix grammars)")
    private CartesianProductFunctionType cartesianProductFunctionType;

    @Option(name = "-ser", required = true, metaVar = "filename", usage = "Serialized output file")
    private String serializedOutputFile;

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    protected void run() throws Exception {
        Reader grammarReader;

        if (inputGrammarFile != null) {
            if (inputGrammarFile.endsWith(".gz")) {
                grammarReader = new InputStreamReader(new GZIPInputStream(new FileInputStream(inputGrammarFile)));
            } else {
                grammarReader = new FileReader(inputGrammarFile);
            }
        } else {
            grammarReader = new InputStreamReader(System.in);
        }

        // Try to create the output stream before we read in the source file. At least it will fail a little sooner
        final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serializedOutputFile));

        @SuppressWarnings("unchecked")
        final Class<? extends Grammar> gc = (Class<? extends Grammar>) Class.forName(grammarClass);

        Grammar g;
        if (cartesianProductFunctionType != null) {
            Class<? extends CartesianProductFunction> cpfClass;
            switch (cartesianProductFunctionType) {
            case Unfiltered:
                cpfClass = UnfilteredFunction.class;
                break;
            case Simple:
                cpfClass = SimpleShiftFunction.class;
                break;
            case BitMatrixExactFilter:
                cpfClass = BitVectorExactFilterFunction.class;
                break;
            case PerfectHash:
                cpfClass = PerfectHashFilterFunction.class;
                break;
            case PerfectHash2:
                cpfClass = PerfectIntPairHashFilterFunction.class;
                break;
            default:
                throw new Exception("Unsupported filter type: " + cartesianProductFunctionType);
            }

            g = gc.getConstructor(new Class[] { Reader.class, Class.class }).newInstance(grammarReader, cpfClass);
        } else {
            g = gc.getConstructor(new Class[] { Reader.class }).newInstance(grammarReader);
        }

        oos.writeObject(g);
        oos.close();
    }

}
