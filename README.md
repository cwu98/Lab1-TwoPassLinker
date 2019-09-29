Lab 1 - Two Pass Linker
==========================================
**Author: Cary Wu**
<br>
*CSCI-UA 202: Operating Systems Fall 2019*
<br>
<br>


The TwoPassLinker program is written in Java. The program is an implementation of a Two Pass Linker. <br>
It takes in an input set of a series of modules and passes it twice through the linker. <br>
The first pass computes the absolute addresses for the defined symbols and outputs the corresponding symbol map. <br>
The second pass uses what was computed in the symbol table to generate the actual output in the form of a memory map
by relocating relative addresses and resolving external references. <br>
<br>

The following instructions are based on Linux Terminal.

## To compile the program: 
```
javac TwoPassLinker.java
```

## To run the program:

### If input is in the form of a <u>text file</u>: 
Type in: 
```
java TwoPassLinker.java < input-text.txt
```

### If input is in the form of <u>keyboard input</u>: 
Type in:
```
java TwoPassLinker.java
```
Press <kbd>Enter</kbd>
```
Enter input here ......
....
..
.
```
Press <kbd>Ctrl</kbd>+<kbd>D</kbd> to signal <b>end of input</b>.

