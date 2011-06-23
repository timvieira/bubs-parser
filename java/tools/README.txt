BUBS Parser

  Copyright 2010, 2011 Nathan Bodenstab and Aaron Dunlop

The BUBS Parser is a grammar-agnostic context-free constituent
parser. Using a high-accuracy grammar (such as the Berkeley
latent-variable grammar), it achieves high accuracy and
throughput superior to other state-of-the-art parsers.

This README describes commonly-used options and some details of the
implementation. Basic usage information is always available by
running  'java -jar parser.jar -help' (or, through the wrapper script, 
'parse -help')


=== Quick Start ===

java -jar parse.jar -g berkeley-SM6.gz -fom berk.boundary.gz \
-beamConfModel berk.bcm.gz \
[input files]

Input: one sentence per line, tokenized according to standard Treebank
  tokenization.

Output: parse trees, one sentence per line


=== Standard Parser Implementations ===

The BUBS Parser implements many distinct parsing algorithms, including
several methods of efficient pruned search as well as a number of
exhaustive parsing methods. The standard implementations are described
here. Some of the other (research) implementations are described
briefly later in this document.

Timings listed are all using the Berkeley grammar 
(see http://berkeleyparser.googlecode.com/).

  -parser CYK: Default exhaustive chart-parsing
    implementation. Approximately 1 minute per sentence.

  -parser Agenda: An agenda parser, using a figure-of-merit to rank
    edges. See -fom option below. (~~ Time?)

  -parser Beam: A bottom-up beam-search implementation. Optionally
    uses a figure-of-merit (-fom) to rank edges and / or a Beam
    Confidence Model (-beamConfModel) to further prune the search
    space. Using an FOM and a Beam Confidence Model, parses at
    approximately 10 sentences/second.

  -parser Matrix: Uses an efficient matrix grammar encoding and
    grammar intersection method. Like the 'Beam' parser, the Matrix
    parser optionally uses an FOM and a Beam Confidence
    Model. Performs exhaustive search in 2.5 - 3 seconds per
    sentence, or pruned search (using FOM and Beam Confidence models)
    at approximately 20 sentences/second.


=== Configuration Options ===

Some parser implementations accept or require configuration
options. All configuration is specified with the '-O' option, using
either '-O key=value' or '-O <properties file>' form. Multiple -O
options are allowed. key=value options override those found in
property files. So, for example, you might use a property file
containing most configuration and override a single option with:

  parse -O parser.properties -O foo=bar

The default options are:

  cellThreads    : 1
  grammarThreads : 1
    (see below for details on threading)

  maxBeamWidth        : 30
  lexicalRowBeamWidth : 60
  lexicalRowUnaries   : 20

    These beam limits assume a boundary FOM and Beam Confidence Model
    (see below). maxBeamWidth applies to cells of span > 1. For
    span-1 cells, we allow a larger beam width, and reserve some space
    for unary productions. These default options are tuned on a
    WSJ development set. Parsing out-of-domain text may require wider
    beam widths.


=== Multithreading ===

The BUBS parser supports threading at several levels. Sentence-level
threading assigns each sentence of the input to a separate thread as
one becomes available). The number of threads is controlled by the 
'-xt <count>' option. In general, if threading only at the sentence level, 
you want to use the same number of threads as CPU cores (or slightly lower, 
to reserve some CPU capacity for OS or other simultaneous tasks).

Cell-level and grammar-level threading are also supported. Cell-level
threading assigns the processing of individual chart cells to threads
(again, as threads become available in the thread pool).

Grammar-level threading subdivides the grammar intersection operation
within an individual cell and splits those tasks across threads.

Cell-level and grammar-level threading are specified with the
'cellThreads' and 'grammarThreads' options. e.g.:

  parse -O cellThreads=4 -O grammarThreads=2

The three levels of threading can interact safely (i.e., you can use
-xt, cellThreads, and grammarThreads simultaneously), and we have shown 
that cell-level and grammar-level threading can provide additive benefits, 
but we make no claims about the efficiency impact of combining sentence-level 
threading with other parallelization methods.


=== Research Parser Implementations ===

In addition to the standard implementions described above, many other
parsing algorithms are available using the -researchParserType
option. The general classes are:

  --Other exhaustive implementations (ECPxyz). All exhaustive
    implementations produce identical parses, but use various grammar
    intersection methods and differ considerably in efficiency.

  --Agenda parsers (APxyz).

  --Beam Search parsers (BSCPxyz). Various methods of beam
    pruning.

  --SpMV parsers (xyzSpmv). Sparse Matrix x Vector grammar
    intersection methods.

  --Matrix Loop parsers (xyzMl). Exhaustive parsers using a matrix
    grammar representation and implement various methods of iterating
    over the matrix during grammar intersection. These methods vary
    greatly in efficiency, from around 4-5 seconds up to several
    minutes per sentence.


=== Pruning (Figure of Merit and Beam Confidence Model) ===

A figure-of-merit (FOM) ranks chart edges locally within a cell or
globally across the entire chart. All agenda parsers and bottom-up
pruned parsers use an FOM of some sort. The simplest FOM is local
inside probability, but a boundary FOM (as described by Carabello and
Charniak, 1998) performs considerably better, allowing a much smaller
beam width (and thus a smaller search space) before accuracy
declines. The berk.boundary.gz model file encodes such an FOM for the
Berkeley grammar.

The Beam Confidence Model (BCM) rates the FOM's confidence in each
cell, closing some cells and further limiting the beam width in
others, where the FOM is expected to make accurate predictions. This
further limits the search space and speeds parsing (see Bodenstab
et. al, 2011).


=== Citing ===

If you use the BUBS parser in research, please cite:

Adaptive Beam-Width Prediction for Efficient CYK Parsing
Nathan Bodenstab, Aaron Dunlop, Keith Hall, and Brian Roark - 
ACL/HLT 2011, pages 440-449.

