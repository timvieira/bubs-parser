The BUBS Parser is a grammar-agnostic constituency parser, designed
and tuned for efficient context-free inference.
Using a high-accuracy grammar (such as the Berkeley
latent-variable grammar), it achieves high accuracy and
throughput superior to other state-of-the-art parsers.

From a user's perspective, BUBS is simply a fast, high-accuracy
parser. From the perspective of a researcher or developer, BUBS is
actually a parser _framework_, rather than a single inference engine.
Most state-of-the-art parsers are strictly tied to a specific
grammar (e.g. the implicit Charniak grammar or the latent-variable
Berkeley grammar), and their inference algorithms are designed
specifically for efficient search using that grammar. BUBS, on the
other hand, is grammar agnostic and implements a wide variety of search
algorithms, from exhaustive CYK and agenda-based methods to heavily
pruned search techniques. See the notes below, the [developer documentation](http://code.google.com/p/bubs-parser/wiki/DevDocs), and the [Javadoc](http://wiki.bubs-parser.googlecode.com/git/javadoc/index.html) for further details.

## Usage ##

Basic usage information is always available by running
`java -jar parse.jar -help` (or, through the wrapper script,
`parse -help`). Use 'parse -long-help' for detailed usage
information, including expanded descriptions of each command-line
option and all standard configuration properties.

## Quick Start ##
Note: Google Code no longer accepts uploads, so new packaged files are hosted on Dropbox instead. The packaged files on the Downloads page are older, and missing more recent enhancements and bug fixes.

Download [bubs-parser-20150206.tgz](https://www.dropbox.com/s/bppl5o7jwqsu8yt/bubs-parser-20150206.tgz?dl=0)

```
tar zxvf bubs-parser-20150206.tgz
cd bubs-parser
chmod a+x parse
echo "This is a test" | ./parse -g wsj_l0mm_16_.55_6.gr.gz -fom wsj_l0mm_16_.55_6.lexfom.gz -ccClassifier wsj_cc.mdl.995
```

Or, more generally, from an input file:
```
./parse -g wsj_l0mm_16_.55_6.gr.gz -fom wsj_l0mm_16_.55_6.lexfom.gz -ccClassifier wsj_cc.mdl.995 [input file]
```

Input: one sentence per line, plain text (for pre-tokenized text, see the `-if` option. Note: BUBS will attempt to detect input in tree format (e.g. in testing), and automatically evaluate accuracy against those gold labels).

Output: parse trees, one sentence per line


## Standard Parser Implementations ##

The BUBS Parser implements many distinct parsing algorithms, including
several methods of efficient pruned search as well as a number of
exhaustive parsing methods. The standard implementations are described
here. Some of the other (research) implementations are described
briefly later in this document. All efficiency metrics are for the
latent-variable [Berkeley](http://code.google.com/p/berkeleyparser/) grammar.

> `-p Matrix`: Uses an efficient matrix-encoded grammar (see Dunlop et al., 2011; citation information below). Supports pruning using -fom and -beamModel. Parses exhaustively with the Berkeley grammar at approximately 8 words/second (3 seconds per sentence) or pruned at up to 5700 words/sec (200+ sentences/second), as measured on an Intel Core i7-3520M.

> `-p Agenda`: An agenda parser, using a figure-of-merit to rank edges. See -fom option below.

> `-p Beam`: A bottom-up beam-search implementation. Optionally uses a figure-of-merit (-fom) to rank edges and / or a Beam Confidence Model (-beamModel) to further prune the search space. Using an FOM and a Beam Confidence Model, parses at approximately 10 sentences/second (250 words/sec) using the Berkeley grammar.

> `-p CYK`: Simple chart-parsing implementation, using a simple (and not terribly efficient) grammar representation. When last tested, the efficiency was around 30-60 seconds per sentence (clearly, this implementation is not recommended for production usage).


## Configuration Options ##

Some parser implementations accept or require configuration
options. All configuration is specified with the '-O' option, using
either '-O key=value' or '-O <properties file>' form. Multiple -O
options are allowed. key=value options override those found in
property files. So, for example, you might use a property file
containing most configuration and override a single option with:

> `parse -O parser.properties -O foo=bar`

The default options are:
```
  cellThreads    : 1
  grammarThreads : 1
    (see below for details on threading)

  maxBeamWidth        : 30
  lexicalRowBeamWidth : 60
  lexicalRowUnaries   : 20
```
> These beam limits assume a boundary POS or lexical FOM and complete closure model
> (see below). maxBeamWidth applies to cells of span > 1. For
> span-1 cells, we allow a larger beam width, and reserve some space
> for unary productions. These default options are tuned on a
> WSJ development set. Parsing out-of-domain text may require wider
> beam widths. Note: To perform exhaustive inference, use `-O maxBeamWidth=0`. When
> maxBeamWidth is set to 0, the other beam parameters are ignored.


## Multithreading ##

Sentence-level threading assigns each sentence of the input to a separate
thread as one becomes available). The number of threads is controlled by the
`-xt <count>` option. In general, you want to use the same number of threads
as CPU cores (or slightly lower, to reserve some CPU capacity for OS or other
simultaneous tasks).

Some parser implementation also support cell-level or grammar-level threading, using `-O cellThreads=x` and `-O grammarThreads=y` options.

## Research Parser Implementations ##

In addition to the standard implementations described above, many other
parsing algorithms are available using the `-rp` (research parser type)
option. The general classes are:

> --Other exhaustive implementations (ECPxyz). All exhaustive implementations produce identical parses, but use various grammar intersection methods and differ considerably in efficiency.

> --Agenda parsers (APxyz).

> --Beam Search parsers (BSCPxyz). Various methods of beam pruning.

> --Matrix Loop parsers (xyzMl). Parsers using a matrix grammar representation. Various implementation use different methods of iterating over the matrix during grammar intersection. These methods vary greatly in efficiency, from around 4-5 seconds up to several minutes per sentence (exhaustive) and up to 60+ sentences/second (pruned).

> --Sparse Matrix/Vector parsers (xyzSpmv). Parsers using a matrix grammar and a matrix-vector operation to perform grammar intersection. Some are very efficient for exhaustive inference (as fast as 3 seconds/sentence), and most are very efficient for pruned inference (upwards of 20 sentences/second).

## Pruning (Figure of Merit and Beam Confidence Model) ##

A figure-of-merit (FOM) ranks chart edges locally within a cell or
globally across the entire chart. All agenda parsers and bottom-up
pruned parsers use an FOM of some sort. The simplest FOM is local
inside probability, but a lexical FOM (as described by Bodenstab, 2012) performs considerably better, allowing a much smaller
beam width (and thus a smaller search space) before accuracy
declines. The wsj\_6.lexfom.gz model encodes such an FOM trained on WSJ text.

A complete-closure pruning model (see Bodenstab et al., 2011) labels certain
chart cells as extremely unlikely to participate in a correct parse, and
omits processing of those cells. The CC models on the download page also
prune unary processing in certain cells; in combination, those two pruning
approaches greatly improve processing speed, and the impact on accuracy is
minimal (accuracy over a sizable corpus is improved in some cases, and
rarely degraded measurably).

BUBS also supports a Beam Confidence Model (BCM). The BCM rates the FOM's confidence in each cell, closing some cells and further limiting the beam width in
others, where the FOM is expected to make accurate predictions. This
further limits the search space and speeds parsing (see Bodenstab
et al., 2011). However, after subsequent research and tuning, we've found
that in most cases, the simpler complete-closure model outperforms a BCM,
so we recommend the CC model for most users.


## Acknowledgements ##

Development of the BUBS parser was partially supported by NSF Grant #IIS-0811745, DARPA grant HR0011-09-1-0041 and a Google Faculty Research Award, Brian Roark, PI.

## License ##

BUBS is licensed under the Gnu Affero GPL license (http://www.gnu.org/licenses/agpl.html). We anticipate that BUBS will primarily be used for research
purposes, and the AGPL license should place few restrictions on
research usage. If you wish to make use of it under a
less restrictive license, including commercial licensing, please
contact:

  * Aaron Dunlop (aaron.dunlop@gmail.com)
  * Nathan Bodenstab (bodenstab@gmail.com)

## Citing ##

If you use the BUBS parser in research, please cite one of the following:

Adaptive Beam-Width Prediction for Efficient CYK Parsing
Nathan Bodenstab, Aaron Dunlop, Keith Hall, and Brian Roark -
ACL/HLT 2011, pages 440-449.

Efficient matrix-encoded grammars and low latency parallelization strategies for CYK
Aaron Dunlop, Nathan Bodenstab, and Brian Roark - IWPT 2011, pages 163-174.