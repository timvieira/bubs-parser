# Introduction #

This page describes training prioritization and pruning models; BUBS exhaustive parsing is faster than any other exhaustive inference system we know of, but at around 8 words/second (wps), it's still not fast by any reasonable measure. In combination, the prioritization and pruning models will yield speedups to around 3-4000 wps.


---


## Requirements ##

  * Trained grammar - some pruning models are generic and apply across grammars, but some pruning models and all prioritization models are trained for a specific grammar. For the examples below, we'll assume this is a 6-cycle Berkeley-style latent-variable grammar stored in `models/eng.sm6.gr.gz`, but any trained grammar will work.

  * Parser and trainer jars. You can build them as follows (in the bubs-parser directory)

```
ant parse train-fom train-cc
cp -p build-dist/parse.jar build-dist/train-fom.jar build-dist/train-cc.jar <target directory>
```


  * Gold-standard training set parse trees, binarized and annotated with the category splits from the grammar (e.g., NP\_12, not just NP). This can be either a Viterbi 1-best exhaustive parse (with the target grammar) or a parse of the training set, constrained by the original (unsplit) parses. Empirically, Viterbi parses seem to produce slightly more effective models, presumably because we're generally executing Viterbi search at inference time. But constrained parsing is much faster, so we'll demonstrate that here (on current hardware, constrained parsing of WSJ 02-21 runs in a couple minutes; exhaustive Viterbi parsing in 30-40 hours). You can populate a constrained binary trees with:

```
java -Xmx1g -jar parse.jar -g models/eng.sm6.gr.gz -rp const -binary input/wsj.02-21.mrgNB.trees >input/wsj.02-21.sm6.constrained
```

  * A development set. For our example, we'll assume WSJ section 22 in `input/wsj.22.mrgNB.trees`.

We'll start with a a prioritization model (also called a 'Figure of Merit'). This model ranks constituents within each cell. When executing a beam search, an effective prioritization model allows us to narrow the beam greatly. We'll train two types of priorization models:


---


## Boundary POS Prioritization Model (FOM) ##
This prioritization model ranks consitituents based on the probability of the observed parts-of-speech at or near the boundary of a span. For example, if a span starts with DT (determiner) and ends with NN (noun), we might consider the label NP (noun phrase) more probable than VP (verb phrase). We can train this model with the following command:

```
java -Xmx1g -jar train-fom.jar -fom BoundaryPOS -g models/eng.sm6.gr.gz input/wsj.02-21.sm6.constrained | gzip -c >eng.sm6.posfom.gz
```

Now, test the model. Since we're using a 6-cycle latent-variable grammar (with a vocabulary of approximately 1100 nonterminals), we'll use a beam width of 30. We're specifying maxBeamWidth (30), lexicalRowBeamWidth (60), and lexicalRowUnaries (20) explicitly here. As of Feb 2013, these values are the defaults. However, as demonstrated below, we can reduce them somewhat with a lexical FOM, so those defaults are subject to change as more users move to lexical models.

```
java -Xmx1g -jar parse.jar -g models/eng.sm6.gr.gz -fom eng.sm6.posfom.gz -O maxBeamWidth=30 -O lexicalRowBeamWidth=60 -O lexicalRowUnaries=20 input/wsj.22.mrgNB.trees
```

We expect approximately the same accuracy as exhaustive Viterbi search. Empirically, prioritizing by inside probability maintains accuracy with a beam around 80-100, but falls off dramatically below that. So if our FOM is working properly, we should see an F1 score of around 89. If not, something's broken. On a 2012 Core i7, these settings yield around 400 words/sec.


---


## Lexical Prioritization Model ##
This model is conceptually similar to the Boundary POS model, but conditions boundary probabilities on the lexical items near the span boundary, instead of their POS tags. This lexicalization can produce a superior model (i.e., one which allows beam search with a narrower beam), but sparse data can be problematic.

```
java -Xmx1g -jar train-fom.jar -fom BoundaryLex -g models/eng.sm6.gr.gz -clusters models/bllip-clusters.len-8 input/wsj.02-21.sm6.constrained | gzip -c >eng.sm6.lexfom.gz
```

And test:

```
java -Xmx1g -jar parse.jar -g models/eng.sm6.gr.gz -fom eng.sm6.lexfom.gz -O maxBeamWidth=30 -O lexicalRowBeamWidth=60 -O lexicalRowUnaries=20 input/wsj.22.mrgNB.trees
```

This lexicalized model uses very little smoothing. It works well for most grammars, but for the 6-cycle LV grammar we're using here, we can't reduce the beam width vs. the Boundary POS FOM (with a beam width of 20, accuracy drops to 88.8). We can alleviate some of the data-sparsity with a word clustering approach, and reduce the beam width somewhat. We'll use a lexical cluster file, defining word clusters per the Brown algorithm (Brown et al., 1992), as described in Koo et al. (2008). They cluster 300k words and apply it to WSJ text. The original cluster files are available at: http://people.csail.mit.edu/maestro/papers/bllip-clusters.gz, and the individual files are also on our Downloads page (`bllip-clusters-len-*`).

The clustering algorithm clusters words in a binary tree hierarchy; we can tune the depth of the hierarchy (combining leaf nodes), and produce cluster-sets of various sizes. Empirically, a depth of 4 produces 16 cluster and a depth of 12, 725 clusters. Note that these cluster files are specific to WSJ text. The resulting FOMs appear to generalize reasonably to other genres, but we don't (currently) have an implementation of Brown clustering to apply directly to other corpora.

For this example, we'll use `bllip-clusters.len-8` (merged to a depth of 8, retaining 166 clusters).

Footnote: In Koo's original file, the binary-tree cluster assignment of each word Koo's original cluster file omits some zeros for some reason, so it's not obvious by looking at the first few entries, but if you dig down a bit, how the "len" corresponds to the clusters is more obvious.

```
java -Xmx1g -jar train-fom.jar -fom BoundaryLex -g models/eng.sm6.gr.gz -clusters models/bllip-clusters.len-8 input/wsj.02-21.sm6.constrained | gzip -c >eng.sm6.lexfom2.gz
```

This FOM is somewhat more effective (at least on WSJ text); we can reduce the beam width to 20, and we still get an F1 score of around 89. And we expect somewhat faster parse times (around 600 w/s vs. 400):

```
java -Xmx1g -jar parse.jar -g models/eng.sm6.gr.gz -fom eng.sm6.lexfom2.gz -O maxBeamWidth=20 -O lexicalRowBeamWidth=60 -O lexicalRowUnaries=20 input/wsj.22.mrgNB.trees
```


---


# Pruning Models #

The next model class is a pruning model, or a cell-selector model. This model will eliminate certain cells from the parse chart. For instance, a tagging model might label certain tokens as unable to end a multi-word span (e.g., a constituent is highly unlikely to end with 'the'); similarly, we can label tokens as unable to begin a multi-word span. We can extend these word-level tags upwards through the parse chart, eliminating ('closing') many cells from consideration (see Roark and Hollingshead, 2008 and 2009).

A similar model, called 'complete closure' (Bodenstab et al., 2011), rather than tagging words, tags chart cells directly as open or closed. We'll demonstrate training this tagging model below. An adaptive beam model (also in Bodenstab et al., 2011) expands and generalizes the cell-closure approach, closing certain cells, and limiting the beam width in others.


---


## Complete Closure Model ##
The complete closure model classifies each chart cell as open or closed. As in FOM training, we can train it using gold-constrained or Viterbi 1-best parse trees. Our pruning models use discriminative classifiers (the current implementations use averaged perceptrons), so they can require several training iterations. Since training can be time-consuming, we output progress information during those training passes. Following training, we serialize the model as a Java serialized object. The complete-closure training first trains a perceptron part-of-speech tagger for several iterations (3 in the example below - "-ptti 3"). It then trains a binary cell classifier for a few more iterations ("-ti 2").

The training data is imbalanced in 2 important ways:
1) Most cells do not participate in the gold or 1-best parses, so we have many more 'closed' examples than 'open'.
2) The cost of mistakenly closing a cell (accuracy loss and potential parse failure) is much greater than the cost of mistakenly opening a cell (additional inference time).

We compensate for those imbalances by biasing the loss function heavily against misclassifying an open cell as closed ("-b 100" specifies a loss function biased 100:1).

Finally, we execute a binary search over bias parameters, optimizing for a desired rate of correct open-cell classifications ("-tnr .999" finds a bias that classifies 99.9% of the 'open' cells correctly on the development set).

```
java -Xmx1g -jar train-cc.jar -tnr .999 -ptti 3 -b 100 -ti 2 -d input/wsj.24.mrgNB.trees -m cc.mdl input/wsj.02-21.mrgNB.trees
```

This will serialize a cell-closure model to cc.mdl. Finally, we test that model:

```
java -Xmx1g -jar parse.jar -g models/eng.sm6.gr.gz -fom eng.sm6.lexfom2.gz -ccClassifier cc.mdl -O maxBeamWidth=20 -O lexicalRowBeamWidth=60 -O lexicalRowUnaries=20 -reparse escalate input/wsj.22.mrgNB.trees
```


---



# References #

Roark and Hollingshead 2008. Classifying chart cells for quadratic complexity context-free inference. Coling.

Roark and Hollingshead 2009 Linear Complexity Context-Free Parsing Pilelines via Chart Constraints, NAACL-HLT.

Bodenstab et al., 2011. Beam-Width Prediction for Efficient Context-Free Parsing, ACL-HLT.

Koo et al., 2008. Simple Semi-supervised Dependency Parsing, ACL-HLT.

Brown et al., 1992. Class-based n-gram models of natural language, Computational Linguistics, Volume 18 #4