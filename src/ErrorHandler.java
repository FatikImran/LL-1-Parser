import java.util.LinkedHashSet;
import java.util.Set;

public class ErrorHandler {

    public static String makeMsg(int lineNo, int col, String got, Set<String> expected, String kind) {
        return "Line " + lineNo + ", Col " + col + " -> " + kind + ". Expected: " + expected + " but found: '" + got + "'";
    }

    public static Set<String> expectedTerminals(Grammar g, LL1Table table, String nonTerminal) {
        Set<String> out = new LinkedHashSet<>();
        if (table.view().containsKey(nonTerminal)) {
            for (var e : table.view().get(nonTerminal).entrySet()) {
                if (e.getValue() != null) out.add(e.getKey());
            }
        }
        // safety fallback
        if (out.isEmpty()) out.addAll(g.getTerminals());
        return out;
    }
}
