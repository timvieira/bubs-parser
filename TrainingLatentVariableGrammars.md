# Introduction #

This page describes training PCFGs using the tools included with BUBS. The grammar training tools are derived from those of the Berkeley Parser, and retain the split-merge training approach described in Petrov et al., 2006 (citations listed below). Most of the original features are retained, along with various additions and improvements. Notably:

  * Alternate merge objective functions, primarily targeting efficiency as well as accuracy
  * Performance optimizations, yielding a 2-3x speedup during training

License note: The Berkeley Parser is released under the GPLv2, while the core BUBS code is under the somewhat more restrictive AGPLv3. We maintain our forked version of the Berkeley grammar training code in a separate source directory; that code remains under the original GPLv2 license (although it is unlikely that grammar training would be directly utilized in a production server setting, where the differences between GPL and AGPL come into play).


---


## Requirements ##

  * Gold-standard training set parse trees. The examples below use the WSJ training corpus

  * Grammar training jar. You can build it as follows (in the bubs-parser directory - see DevDocs for checkout instructions.)

```
ant train-grammar
cp -p build-dist/train-grammar.jar <target directory>
```


---


Grammar training is then a single command:

```
java -Xmx2g -jar train-grammar.jar -rs 1 -cycles 4 -mf .55 -mrp 1e-6 -gd grammars -gp wsj_1_.55 wsj.02-21.mrgNB.trees
```

This example uses a random seed of 1 (-rs 1), trains for 4 split-merge cycles (-cycles 4), re-merges 55% of all splits in each cycle (-mf .55), and retains productions of probability greater than 10^-6 (-mrp 1e-6).

The resulting grammar is stored in `grammars/wsj_1_.55_4.gr.gz`

Optimum performance on WSJ text is usually obtained with a 6-cycle grammar, re-merging around 55% of all splits during each cycle.


---



# References #

Petrov, et al., 2006. Learning accurate, compact, and interpretable tree annotation. ACL

Roark and Hollingshead 2008. Classifying chart cells for quadratic complexity context-free inference. Coling.

Roark and Hollingshead 2009 Linear Complexity Context-Free Parsing Pilelines via Chart Constraints, NAACL-HLT

Bodenstab et al., 2011. Beam-Width Prediction for Efficient Context-Free Parsing, ACL-HLT