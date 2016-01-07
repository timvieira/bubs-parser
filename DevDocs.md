# Building the packaged executable with [Ant](http://ant.apache.org) #
BUBS parsing tools are built with Apache Ant and Apache Ivy

The Ivy library (from http://ant.apache.org/ivy/download.cgi) must be installed in $ANT\_HOME/lib, /usr/share/ant/lib, ~/.ant/lib, or another location available to the ant binary.

  * git clone https://code.google.com/p/bubs-parser/
  * cd bubs-parser
  * ant parse
`parse.jar` and the `parse` wrapper script will be generated in the `build-dist` directory.

During the first build, Ivy will download dependencies from Maven's
repository, and cache them locally for reuse on subsequent builds.


# Checking out as an Eclipse project #
Install the IvyDE plugin
  * (In Eclipse) Help -> 'Install New Software'
  * In the 'Work with:' dropdown, select http://www.apache.org/dist/ant/ivyde/updatesite
  * Check 'Apache Ivy Library' and 'Apache IvyDE Eclipse plugins'
  * Click Next, accept terms and conditions, and install

Note: git was integrated into Eclipse (via the EGit plugin) as of the Juno release (June, 2012). If you are using an older release, you can follow a similar process to install EGit from http://download.eclipse.org/egit/updates.

(After Eclipse restarts...)
  * Window->Open Perspective->Other...
  * Select 'Git Repository Exploring'
  * Click 'Clone a Git repository' (an icon at the top of the 'Git Repositories' pane)
  * URI:  https://code.google.com/p/bubs-parser/
  * (optional) User: GoogleCode user, password: GoogleCode password
  * Click Next

  * Select all branches and click Next

  * Directory: <Eclipse workspace>/bubs-parser
  * Initial branch: master
  * Remote name: origin
  * Select 'Import all existing projects after clone finishes'
  * (optional) Select 'Add project to working sets' and select set(s)
  * Click 'Finish'

# Unit Tests #

Most exhaustive parsers and some pruned implementations include fairly detailed unit tests, implemented using the [JUnit](http://www.junit.org/) framework. Unit tests are located in the same package as the class under test, named as `TestXyz` (e.g. class `Foo` will be tested by `TestFoo`). Base test classes are generally named `XyzTestCase`. Unit test classes are omitted from the published JavaDoc, but include reasonably detailed comments describing the functionality they verify.

Checking out the Eclipse project will add several JUnit test configurations to the debug dropdown. You can execute any of these test suites from there.

  * `AllParserTests` - This suite will exercise every parser implementation, parsing (at least) 10 sentences with each and checking the output. Note that some implementations are quite slow, so this suite takes a few minutes to run.
  * `AllParserTests (Basic)` - 'Basic' tests only. This suite will exercise the same parser implementations, but only parse a couple short sentences with each, so it runs in seconds instead of minutes.
  * `AllParserTests (Profiling)` - Executes profiling code, intended to catch performance regressions. The assertions are necessarily tied to specific test hardware; you will need to specify a tag for your own hardware in an Eclipse `test.hardware` launch variable, and might need to add assertions specific to your own hardware.
  * `All BUBS Tests` - In addition to parsing-specific test cases, executes tests of data structure and shared code, including that in `edu.ohsu.cslu.datastructs` and its sub-packages.


# Package structure #

The important packages are rooted at:
  * [edu.ohsu.cslu.grammar](http://wiki.bubs-parser.googlecode.com/git/javadoc/edu/ohsu/cslu/grammar/package-summary.html): Grammar implementations (see below for more details on the Grammar hierarchy)
  * [edu.ohsu.cslu.parser](http://wiki.bubs-parser.googlecode.com/git/javadoc/edu/ohsu/cslu/grammar/package-summary.html): The main BUBS parsing code, including implementations of various inference algorithms.

Implementations of various inference methods and supporting classes are divided into subpackages described further in the [Javadoc](http://wiki.bubs-parser.googlecode.com/git/javadoc/index.html).

# `Parser` hierarchy #

The BUBS package includes many parser implementations, including various exhaustive and pruned CYK implementations, and agenda parsers. All parser implementations extend the base class `edu.ohsu.cslu.parser.Parser`. Common code is implemented in `Parser`, and subclasses implement the various inference algorithms.

Note that `Parser` instances should be reused for multiple sentences, but are not expected to be thread-safe or reentrant, so a separate `Parser` instance should be created for each thread if parsing multiple sentences in parallel.

Some `Parser` implementations are threaded internally, using multiple threads to speed parsing of a single sentence (see, for example, `GrammarParallelCscSpmvParser`). There is often a tradeoff between total throughput (sentences per second) and latency (average time to parse a single sentence).

# `Grammar` hierarchy #

A probabilistic context-free grammar (PCFG) can be represented in a wide variety of data structures, each with various tradeoffs in space and query speed. We implement a number of representations, each as a subclass of the base `Grammar` class. Most parser classes depend explicitly on a particular grammar representation - this dependency is encoded with a generic annotation on the parser class (e.g., `public class ECPCellCrossList extends ChartParser<LeftListGrammar, CellChart>`).

# `EdgeSelector` / `EdgeSelectorModel` (edu.ohsu.cslu.parser.edgeselector) #

Many pruning methods must compare candidate edges under consideration for inclusion in the parse forest (either globally within an edge agenda or locally within a chart cell). The `EdgeSelector` hierarchy implements various methods of making such comparisons. A simple example uses the grammar model inside probability, as implemented in `InsideProb`. More complex examples might compute and incorporate a heuristic estimate of the outside probability of the edge as well.

Most `EdgeSelector` implementations are not thread-safe, and thus cannot be shared by multiple parsers parsing separate sentences. An instance of an `EdgeSelectorModel` instantiates an `EdgeSelector` for each parser (or for each sentence). For those edge ranking methods which must read in a model, the model parameters are stored in the `EdgeSelectorModel` and shared across all `EdgeSelector` instances.

# `CellSelector` / `CellSelectorModel` (`edu.ohsu.cslu.parser.cellselector`) #

In addition to pruning edges, some chart-parsing implementations classify and/or prune entire chart cells. This is implemented by the `CellSelector` and `CellSelectorModel` hierarchies, analagously to the `EdgeSelector` / `EdgeSelectorModel` pair.

# Input and Output #

Input is plain text, space segmented and optionally tokenized, or gold trees in Penn-Treebank format. Input files on the command-line will be processed in the order specified; if no input files are specified, input will be taken from standard input. If gold trees are input, evalb scores will be output in the final info line.

Parse trees are returned on standard output in Penn-Treebank bracketed tree format.


# Embedding BUBS - constructing and executing a parser #

Although BUBS is primarily designed for standalone use (through the `ParserDriver` entry point), it can also be embedded into user code. The example below (also included in the BUBS codebase) demonstrates how to create and execute a Parser instance.

```
package edu.ohsu.cslu.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.cellselector.CompleteClosureModel;
import edu.ohsu.cslu.parser.fom.BoundaryLex;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FOMType;
import edu.ohsu.cslu.parser.spmv.CscSpmvParser;

/**
 * Demonstrates a very simple method of embedding BUBS functionality into user code, including reading in a grammar and
 * pruning model and parsing simple example sentences.
 * 
 * Usage: EmbeddedExample <grammar file> <edge selector model> <cell selector model>
 * 
 * e.g. EmbeddedExample models/wsj_l0mm_16_.55_6.gr.gz models/wsj_l0mm_16_.55_6.lexfom.gz models/wsj_cc.mdl.99
 */
public class EmbeddedExample {

    public static void main(final String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {

        // The 'ParserDriver' class also serves as the container for parser options
        final ParserDriver opts = new ParserDriver();

        // Instantiate a Grammar class and load in the grammar from disk
        final LeftCscSparseMatrixGrammar grammar = new LeftCscSparseMatrixGrammar(uncompressFile(args[0]),
                new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class);
        opts.setGrammar(grammar);

        // Create FOMModel and CellSelectorModel instances and load models from disk
        opts.fomModel = new BoundaryLex(FOMType.BoundaryLex, grammar, uncompressFile(args[1]));
        opts.cellSelectorModel = new CompleteClosureModel(new File(args[2]), null);

        // Create a Parser instance
        final CscSpmvParser parser = new CscSpmvParser(opts, grammar);

        // Parse example sentences and write output to STDOUT
        ParseTask result = parser
                .parseSentence("The economy 's temperature will be taken from several vantage points this week , with readings on trade , output , housing and inflation .");
        System.out.println(result.parseBracketString(false));

        result = parser
                .parseSentence("The most troublesome report may be the August merchandise trade deficit due out tomorrow .");
        System.out.println(result.parseBracketString(false));
    }

    // Open and uncompress a gzipped file, returning a BufferedReader
    private static BufferedReader uncompressFile(final String filename) throws FileNotFoundException, IOException {
        return new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename))));
    }
}
```

# `ParserDriver` #

The `ParserDriver` class serves as the main entry point. It processes command-line arguments, creates `Grammar`, `EdgeSelector`, `CellSelector`, and `Parser` instances, and manages I/O. `ParserDriver` is based on the [cltool4j](http://code.google.com/p/cltool4j/) command-line tool framework.