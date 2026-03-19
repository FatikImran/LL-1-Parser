import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Main {

    private static final String TEAM = "Muhammad Fatik (23I-0655, Section C), Muhammad Kaleem Akhtar (23I-0524, Section C)";


    public static void main(String[] args) {
        // this main do the whole flow in one run, less headache later
        Path root = Paths.get("");
        Path inputDir = root.resolve("input");
        Path outputDir = root.resolve("output");

        try {
            Files.createDirectories(outputDir);

            List<Path> grammars = findGrammarFiles(inputDir);
            if (grammars.isEmpty()) {
                System.err.println("No grammar files found in input/. Add grammar1.txt etc.");
                return;
            }

            StringBuilder transformedAll = new StringBuilder();
            StringBuilder firstFollowAll = new StringBuilder();
            StringBuilder tableAll = new StringBuilder();
            StringBuilder parseTreesAll = new StringBuilder();
            StringBuilder traceMaster = new StringBuilder();

            transformedAll.append(header("Assignment 02 - Grammar Transformations"));
            transformedAll.append("Team: ").append(TEAM).append("\n\n");

            firstFollowAll.append(header("Assignment 02 - FIRST/FOLLOW Sets"));
            firstFollowAll.append("Team: ").append(TEAM).append("\n\n");

            tableAll.append(header("Assignment 02 - LL(1) Parsing Tables"));
            tableAll.append("Team: ").append(TEAM).append("\n\n");

            parseTreesAll.append(header("Assignment 02 - Parse Trees"));
            parseTreesAll.append("Team: ").append(TEAM).append("\n\n");

            traceMaster.append(header("Assignment 02 - Parsing Traces"));
            traceMaster.append("Team: ").append(TEAM).append("\n\n");

            int traceCounter = 1;

            for (Path gf : grammars) {
                String gName = gf.getFileName().toString().replace(".txt", "");

                transformedAll.append("======================== ").append(gName).append(" ========================\n");
                firstFollowAll.append("======================== ").append(gName).append(" ========================\n");
                tableAll.append("======================== ").append(gName).append(" ========================\n");
                parseTreesAll.append("======================== ").append(gName).append(" ========================\n");
                traceMaster.append("======================== ").append(gName).append(" ========================\n");

                Grammar original = Grammar.fromFile(gf);
                Grammar leftFactored = GrammarTransformer.applyLeftFactoring(original);
                Grammar transformed = GrammarTransformer.removeLeftRecursion(leftFactored);

                transformedAll.append("Original Grammar:\n").append(original.pretty()).append("\n");
                transformedAll.append("After Left Factoring:\n").append(leftFactored.pretty()).append("\n");
                transformedAll.append("After Left Recursion Removal:\n").append(transformed.pretty()).append("\n\n");

                FirstFollow ff = new FirstFollow(transformed);
                ff.computeAll();
                firstFollowAll.append(ff.prettySets()).append("\n");

                LL1Table table = new LL1Table(transformed, ff);
                table.build();
                tableAll.append(table.pretty()).append("\n");

                List<Path> inputFiles = findInputFilesForGrammar(inputDir, gName);
                if (inputFiles.isEmpty()) {
                    traceMaster.append("No input files matched for ").append(gName).append("\n\n");
                    continue;
                }

                Parser parser = new Parser(transformed, table, ff);

                for (Path inputFile : inputFiles) {
                    traceMaster.append("Input File: ").append(inputFile.getFileName()).append("\n");
                    List<String> lines = Files.readAllLines(inputFile, StandardCharsets.UTF_8);
                    int lineNo = 0;
                    for (String line : lines) {
                        lineNo++;
                        if (line.trim().isEmpty()) continue;

                        List<String> toks = tokenizeInputLine(line);
                        String validationErr = validateInputTokens(toks, transformed);

                        traceMaster.append("\nLine ").append(lineNo).append(": ").append(line).append("\n");

                        if (validationErr != null) {
                            traceMaster.append("Input validation ERROR: ").append(validationErr).append("\n\n");
                            continue;
                        }

                        Parser.ParseResult rs = parser.parseTokens(toks, lineNo);
                        traceMaster.append(rs.traceAsText());

                        if (!rs.errors.isEmpty()) {
                            traceMaster.append("Errors:\n");
                            for (String e : rs.errors) traceMaster.append(" - ").append(e).append("\n");
                        }
                        traceMaster.append("Result: ").append(rs.accepted ? "Accepted" : "Rejected/Recovered with errors");
                        traceMaster.append(" (errors=").append(rs.errCount).append(")\n\n");

                        // Keep individual trace files also, demo me handy hota
                        String singleTraceName = "parsing_trace" + traceCounter + ".txt";
                        String oneTrace = "Grammar: " + gName + "\nInput file: " + inputFile.getFileName() + "\nLine " + lineNo + ": " + line + "\n\n" +
                                rs.traceAsText() + "\nErrors:\n" + String.join("\n", rs.errors) + "\n\nResult: " + (rs.accepted ? "Accepted" : "Rejected/Recovered");
                        Files.writeString(outputDir.resolve(singleTraceName), oneTrace, StandardCharsets.UTF_8);
                        traceCounter++;

                        if (rs.accepted && rs.root != null) {
                            parseTreesAll.append("Input: ").append(line).append("\n");
                            parseTreesAll.append(Tree.toIndented(rs.root)).append("\n");

                            List<String> pre = new ArrayList<>();
                            List<String> post = new ArrayList<>();
                            Tree.preorder(rs.root, pre);
                            Tree.postorder(rs.root, post);
                            parseTreesAll.append("Preorder: ").append(pre).append("\n");
                            parseTreesAll.append("Postorder: ").append(post).append("\n\n");
                        }
                    }
                    traceMaster.append("\n");
                }
            }

            Files.writeString(outputDir.resolve("grammar_transformed.txt"), transformedAll.toString(), StandardCharsets.UTF_8);
            Files.writeString(outputDir.resolve("first_follow_sets.txt"), firstFollowAll.toString(), StandardCharsets.UTF_8);
            Files.writeString(outputDir.resolve("parsing_table.txt"), tableAll.toString(), StandardCharsets.UTF_8);
            Files.writeString(outputDir.resolve("parse_trees.txt"), parseTreesAll.toString(), StandardCharsets.UTF_8);
            Files.writeString(outputDir.resolve("parsing_trace_master.txt"), traceMaster.toString(), StandardCharsets.UTF_8);

            System.out.println("All done. Output files generated in output/");
        } catch (Exception ex) {
            // keep it plain and useful
            System.err.println("Run failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static String validateInputTokens(List<String> toks, Grammar g) {
        if (toks.isEmpty()) {
            return "Input line has no tokens";
        }
        Set<String> allowed = new LinkedHashSet<>(g.getTerminals());
        allowed.add("$");

        for (String t : toks) {
            if (!allowed.contains(t)) {
                return "Unknown token '" + t + "'. Allowed terminals are: " + allowed;
            }
        }
        return null;
    }

    private static List<String> tokenizeInputLine(String line) {
        String[] arr = line.trim().split("\\s+");
        return new ArrayList<>(Arrays.asList(arr));
    }

    private static List<Path> findGrammarFiles(Path inputDir) throws IOException {
        List<Path> out = new ArrayList<>();
        if (!Files.exists(inputDir)) return out;

        try (var st = Files.list(inputDir)) {
            st.filter(p -> p.getFileName().toString().matches("grammar\\d+\\.txt"))
              .sorted()
              .forEach(out::add);
        }
        return out;
    }

    private static List<Path> findInputFilesForGrammar(Path inputDir, String grammarFileNoExt) throws IOException {
        List<Path> out = new ArrayList<>();
        String n = grammarFileNoExt.replace("grammar", "");
        if (n.isBlank()) return out;

        try (var st = Files.list(inputDir)) {
            st.filter(p -> p.getFileName().toString().matches("grammar" + n + "_.*\\.txt"))
              .sorted()
              .forEach(out::add);
        }
        return out;
    }

    private static String header(String title) {
        String ts = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        return title + "\nGenerated: " + ts + "\n" + "=".repeat(70) + "\n";
    }
}
