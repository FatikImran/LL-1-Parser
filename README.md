# Assignment 02 - LL(1) Parser Design and Implementation

## Team Members
- Muhammad Fatik - 23I-0655 - Section C
- Muhammad Kaleem Akhtar - 23I-0524 - Section C

## Language Used
Java


## Project Structure
- src/Main.java
- src/Grammar.java
- src/GrammarTransformer.java
- src/FirstFollow.java
- src/LL1Table.java
- src/Parser.java
- src/Stack.java
- src/Tree.java
- src/ErrorHandler.java
- src/Production.java

- input/grammar1.txt ... grammar4.txt
- input/grammarX_valid.txt, grammarX_errors.txt, grammarX_edge_cases.txt

- output/grammar_transformed.txt
- output/first_follow_sets.txt
- output/parsing_table.txt
- output/parsing_trace_master.txt
- output/parsing_trace1.txt ... parsing_traceN.txt
- output/parse_trees.txt

## Compilation Instructions
From the project root:

```powershell
javac -d bin src\*.java
```

## Execution Instructions
From the project root:

```powershell
java -cp bin Main
```

This generates all required output files in output/.


## Input File Format Specification

### Grammar File Format
- One production per line
- Arrow can be `->` or `→`
- Alternatives separated by `|`
- Non-terminals are multi-character names starting with uppercase, e.g. Expr, StmtPrime
- Epsilon can be `epsilon` or `ε`
- Symbols can be space-separated. Example:
  - `Factor -> ( Expr ) | id`

### Input String File Format
- One input string per line
- Tokens separated by spaces
- Use terminals only from the related grammar file


## Sample Input Bundle Explanation
- grammar1: simple epsilon grammar
- grammar2: classic expression grammar (needs left recursion removal)
- grammar3: dangling else grammar (shows LL(1) conflict)
- grammar4: indirect left recursion grammar (also ends non-LL(1))

Each grammar has:
- valid strings file
- errors file
- edge cases file


## Features Implemented
- CFG parsing from file with input validation
- Left factoring
- Direct + indirect left recursion removal
- FIRST and FOLLOW computation
- LL(1) parsing table construction + conflict detection
- Stack-based parser trace output
- Panic-mode style recovery on empty table entries
- Error messages with line/column and expected-vs-found details
- Parse tree generation for accepted strings
- Preorder and postorder traversal output


## Known Limitations
- Parser keeps going in panic mode but does not guarantee best possible recovery path for all malformed cases.
- Grammar lexer is permissive and simple; very exotic token naming styles are not covered.
- parse tree is generated for accepted strings only (as required), not for rejected ones.


## Quick Run
```powershell
javac -d bin src\*.java
java -cp bin Main
Get-ChildItem output
```
