# Report - LL(1) Parser Assignment

## 1. Introduction
This project implements a complete LL(1) parser pipeline from reading CFG till parsing strings and making parse trees. We focused on handling real assignment constraints including grammar transformation, LL(1) table creation, and parser error recovery.

## 2. Approach

### Data Structures Used
- `LinkedHashMap<String, List<List<String>>>` for grammar productions
- `LinkedHashSet<String>` for FIRST and FOLLOW sets
- Custom generic `Stack<T>` for parser stack
- Tree nodes (`Tree.Node`) with children list for parse tree

### Algorithm Details
- Left factoring: repeatedly detect longest common prefix in alternatives and split using `Prime` non-terminals.
- Left recursion removal:
  - indirect recursion removed using ordered non-term substitution
  - direct recursion removed using standard `A -> beta APrime` and `APrime -> alpha APrime | epsilon`
- FIRST/FOLLOW: iterative fixed-point computation until no more changes.
- LL(1) table: fill from FIRST(rhs), and FOLLOW(lhs) when epsilon in FIRST(rhs).
- Parsing: stack-driven LL(1) algorithm with trace rows per step.

### Design Decisions
- We used modular classes to keep one responsibility per file.
- We preserve insertion order in maps/sets for stable and readable output files.
- Grammar parser supports both `->` and unicode arrow `→`.

### Indirect Left Recursion Handling
`GrammarTransformer.removeLeftRecursion` follows classic `A1..An` ordering:
1. Replace `Ai -> Aj gamma` for all `j < i`.
2. Eliminate direct recursion from `Ai`.

### Error Recovery Strategy
Panic mode style recovery:
- If table entry is empty, either pop non-terminal when lookahead is in FOLLOW(non-terminal), or skip input token.
- For terminal mismatch, pop expected terminal from stack (acts like insertion/deletion style correction).
- Continue to report multiple errors in same line.

## 3. Challenges and Fixes
- Acceptance condition initially was strict with index checks and marked valid lines as rejected. Fixed by explicit accept flag when both stack/input reached `$`.
- Table conflicts for grammars 3 and 4 were expected from theory, and are now reported clearly.
- Tokenization for grammars with and without spaces needed fallback lexical scanning.

## 4. Test Cases
- Grammars tested: 4
- Input strings: multiple files per grammar (`valid`, `errors`, `edge_cases`)
- Includes:
  - valid strings
  - invalid strings with syntax mistakes
  - missing and extra symbols
  - indirect left recursion grammar
  - grammar requiring factoring and recursion removal

## 5. Verification
- Compiled with `javac` successfully.
- Executed full run using `java -cp bin Main`.
- Verified generated outputs:
  - transformed grammars
  - FIRST/FOLLOW sets
  - LL(1) table and conflicts
  - stepwise parsing traces
  - parse trees for accepted strings

## 6. Sample Outputs
See output folder files:
- `grammar_transformed.txt`
- `first_follow_sets.txt`
- `parsing_table.txt`
- `parsing_trace_master.txt`
- `parse_trees.txt`

## 7. Conclusion
This implementation covers the full LL(1) pipeline with reusable modules, error reporting, and recovery. Biggest learning was that grammar transformation and robust error handling are where most practical complexity lives, not only parser stack logic.
