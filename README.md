# About
**PRISM-Leak** is a tool to evaluate **secure information flow** of concurrent probabilistic programs, written in the [PRISM language](http://www.prismmodelchecker.org/manual/ThePRISMLanguage/Introduction). PRISM-Leak contains two packages, a qualitative package that checks observational determinism and a quantitative package which computes various types of information leakage, including expected, minimum, maximum, and bounded time, using a trace-based algorithm and a back-bisimulation algorithm.

The tool is built upon the [PRISM model checker](http://www.prismmodelchecker.org/). PRISM compiles a program written in the PRISM language, builds a discrete-time Markov chain model of the program and stores it using BDDs (Binary Decision Diagrams) and MTBDDs (Multi-Terminal Binary Decision Diagrams). PRISM-Leak uses these data structures to extract the set of reachable states and also create a sparse matrix containing the transitions. It then verifies observational determinism or computes the amount of information leakage.

A main difference of PRISM-Leak and other related leakage quantification tools is that PRISM-Leak takes into account *intermediate leakages*. This is suitable for *concurrent* programs, in which the attacker is able to observe intermediate values of publicly observable variables. 

# Installation
Compiling:
```
cd prism-leak
make
```


# Usage
Change directory to `bin` and run `prism`:
```
cd bin
prism  [options] <model-file> [more-options]
```
Options:

`-min`  Compute the expected leakage using min-entropy 

`-shannon`  Compute the expected leakage using Shannon entropy. The default is Shannon entropy

`-leakbounds`  Compute maximum and minimum leakages, which are upper and lower leakage bounds for an attacker with a given prior knowledge about the secret input

`-bounded <n>`  Compute bounded time leakage, which is the amount of expected leakage at a given time (step)

`-initdist <file>`  Specify the initial probability distribution of the secret input. If not specified, the uniform distribution is assumed

`-help | -h | -?`  Display this help message

`-prismhelp`  Display PRISM help message

`-version`  Display PRISM-Leak and PRISM version info


# People
The people currently working on the tool are:

* [Ali A. Noroozi](https://alianoroozi.github.io), currently a Ph.D. student at [University of Tabriz](http://tabrizu.ac.ir/en) and developer of the project,

* Khayyam Salehi, currently a Ph.D. student at University of Tabriz and developer of the project,

* [Jaber Karimpour](http://simap.tabrizu.ac.ir/cv/karimpour/?lang=en-gb), an associate professor at University of Tabriz and supervisor of the project,

* [Ayaz Isazadeh](http://isazadeh.net/ayaz), a professor at University of Tabriz and supervisor of the project.

