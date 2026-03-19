import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Grammar {
    private static final Pattern NON_TERM_PATTERN = Pattern.compile("[A-Z][A-Za-z0-9]*");

    private final LinkedHashMap<String, List<List<String>>> prodMap = new LinkedHashMap<>();
    private final LinkedHashSet<String> non_terms = new LinkedHashSet<>();
    private final LinkedHashSet<String> terminals = new LinkedHashSet<>();
    private String startSym;

    public static Grammar fromFile(Path filePath) throws IOException {
        Grammar g = new Grammar();
        g.readAndValidate(filePath);
        g.refreshTerminals();
        return g;
    }

    public Grammar deepCopy() {
        Grammar cp = new Grammar();
        cp.startSym = this.startSym;
        for (Map.Entry<String, List<List<String>>> e : prodMap.entrySet()) {
            List<List<String>> all = new ArrayList<>();
            for (List<String> rhs : e.getValue()) {
                all.add(new ArrayList<>(rhs));
            }
            cp.prodMap.put(e.getKey(), all);
            cp.non_terms.add(e.getKey());
        }
        cp.refreshTerminals();
        return cp;
    }

    public void addProduction(String lhs, List<String> rhs) {
        prodMap.computeIfAbsent(lhs, k -> new ArrayList<>()).add(new ArrayList<>(rhs));
        non_terms.add(lhs);
        if (startSym == null) startSym = lhs;
    }

    public LinkedHashMap<String, List<List<String>>> getProdMap() {
        return prodMap;
    }

    public Set<String> getNonTerminals() {
        return Collections.unmodifiableSet(non_terms);
    }

    public Set<String> getTerminals() {
        return Collections.unmodifiableSet(terminals);
    }

    public String getStartSymbol() {
        return startSym;
    }

    public void setStartSymbol(String s) {
        this.startSym = s;
    }

    public boolean isNonTerminal(String s) {
        return non_terms.contains(s);
    }

    public void rebuildNonTerminalSetFromKeys() {
        non_terms.clear();
        non_terms.addAll(prodMap.keySet());
    }

    public void refreshTerminals() {
        terminals.clear();
        for (Map.Entry<String, List<List<String>>> e : prodMap.entrySet()) {
            for (List<String> rhs : e.getValue()) {
                for (String sym : rhs) {
                    if (!"epsilon".equals(sym) && !prodMap.containsKey(sym)) {
                        terminals.add(sym);
                    }
                }
            }
        }
    }

    public String pretty() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<List<String>>> e : prodMap.entrySet()) {
            sb.append(e.getKey()).append(" -> ");
            List<String> alts = new ArrayList<>();
            for (List<String> rhs : e.getValue()) {
                alts.add(String.join(" ", rhs));
            }
            sb.append(String.join(" | ", alts)).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private void readAndValidate(Path filePath) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(filePath)) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String arrow = line.contains("->") ? "->" : "→";
                String[] parts = line.split(Pattern.quote(arrow));
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Bad production at line " + lineNo + ": " + line);
                }

                String lhs = parts[0].trim();
                validateNonTerm(lhs, lineNo);
                if (lhs.length() == 1) {
                    throw new IllegalArgumentException("Single-character non-terminal not allowed at line " + lineNo + ": " + lhs);
                }

                if (startSym == null) startSym = lhs;
                prodMap.computeIfAbsent(lhs, k -> new ArrayList<>());
                non_terms.add(lhs);

                String rhsPart = parts[1].trim();
                String[] alts = rhsPart.split("\\|");
                for (String altRaw : alts) {
                    String alt = altRaw.trim();
                    if (alt.isEmpty()) {
                        throw new IllegalArgumentException("Empty alternative at line " + lineNo);
                    }
                    List<String> syms = tokenizeAlternative(alt);
                    prodMap.get(lhs).add(syms);
                }
            }
        }

        if (prodMap.isEmpty()) {
            throw new IllegalArgumentException("Grammar file is empty. at least one production is needed");
        }

        // now we know keys, verify non-terminal-looking symbols are defined
        for (Map.Entry<String, List<List<String>>> e : prodMap.entrySet()) {
            for (List<String> rhs : e.getValue()) {
                for (String sym : rhs) {
                    if ("epsilon".equals(sym)) continue;
                    if (looksLikeNonTerminal(sym) && !prodMap.containsKey(sym)) {
                        throw new IllegalArgumentException("Undefined non-terminal used: " + sym + " in production " + e.getKey());
                    }
                }
            }
        }
    }

    private static void validateNonTerm(String lhs, int lineNo) {
        if (!NON_TERM_PATTERN.matcher(lhs).matches()) {
            throw new IllegalArgumentException("Invalid non-terminal at line " + lineNo + ": " + lhs);
        }
    }

    public static boolean looksLikeNonTerminal(String s) {
        return NON_TERM_PATTERN.matcher(s).matches();
    }

    public static List<String> tokenizeAlternative(String alt) {
        alt = alt.trim();
        if (alt.equals("ε") || alt.equals("epsilon")) {
            return new ArrayList<>(List.of("epsilon"));
        }

        if (alt.contains(" ")) {
            String[] parts = alt.split("\\s+");
            List<String> out = new ArrayList<>();
            for (String p : parts) {
                if (p.equals("ε") || p.equals("epsilon")) out.add("epsilon");
                else out.add(p);
            }
            return out;
        }

        // no spaces, do a light lexical scan so grammar like (Expr) works
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < alt.length()) {
            char c = alt.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (Character.isUpperCase(c)) {
                int j = i + 1;
                while (j < alt.length() && Character.isLetterOrDigit(alt.charAt(j))) j++;
                out.add(alt.substring(i, j));
                i = j;
                continue;
            }

            if (Character.isLowerCase(c)) {
                int j = i + 1;
                while (j < alt.length() && Character.isLetterOrDigit(alt.charAt(j))) j++;
                String w = alt.substring(i, j);
                out.add(w.equals("epsilon") ? "epsilon" : w);
                i = j;
                continue;
            }

            out.add(String.valueOf(c));
            i++;
        }

        if (out.isEmpty()) {
            throw new IllegalArgumentException("Could not tokenize RHS alternative: " + alt);
        }
        return out;
    }
}
