import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LL1Table {
    private final Grammar g;
    private final FirstFollow ff;
    private final LinkedHashMap<String, LinkedHashMap<String, Production>> table = new LinkedHashMap<>();
    private final List<String> conflicts = new ArrayList<>();

    public LL1Table(Grammar g, FirstFollow ff) {
        this.g = g;
        this.ff = ff;
    }

    public void build() {
        table.clear();
        conflicts.clear();

        Set<String> terms = new LinkedHashSet<>(g.getTerminals());
        terms.add("$");

        for (String nt : g.getProdMap().keySet()) {
            LinkedHashMap<String, Production> row = new LinkedHashMap<>();
            for (String t : terms) row.put(t, null);
            table.put(nt, row);
        }

        for (Map.Entry<String, List<List<String>>> e : g.getProdMap().entrySet()) {
            String A = e.getKey();
            for (List<String> rhs : e.getValue()) {
                Production p = new Production(A, rhs);
                LinkedHashSet<String> firstAlpha = ff.firstOfSequence(rhs);

                for (String a : firstAlpha) {
                    if ("epsilon".equals(a)) continue;
                    putEntry(A, a, p);
                }

                if (firstAlpha.contains("epsilon")) {
                    for (String b : ff.getFollowMap().get(A)) {
                        putEntry(A, b, p);
                    }
                }
            }
        }
    }

    private void putEntry(String A, String a, Production p) {
        LinkedHashMap<String, Production> row = table.get(A);
        if (!row.containsKey(a)) {
            row.put(a, p);
            return;
        }

        Production old = row.get(a);
        if (old == null) {
            row.put(a, p);
            return;
        }

        if (!old.equals(p)) {
            conflicts.add("Conflict at M[" + A + ", " + a + "] : " + old + "  vs  " + p);
        }
    }

    public Production get(String nt, String t) {
        LinkedHashMap<String, Production> row = table.get(nt);
        if (row == null) return null;
        return row.get(t);
    }

    public boolean isLL1() {
        return conflicts.isEmpty();
    }

    public List<String> getConflicts() {
        return conflicts;
    }

    public Map<String, LinkedHashMap<String, Production>> view() {
        return table;
    }

    public String pretty() {
        List<String> cols = new ArrayList<>(g.getTerminals());
        cols.add("$");

        StringBuilder sb = new StringBuilder();
        sb.append("LL(1) Parsing Table\n");
        sb.append("===============================================================\n");
        sb.append(String.format("%-16s", "NonTerminal"));
        for (String c : cols) sb.append(String.format("| %-26s", c));
        sb.append("\n");
        sb.append("-".repeat(Math.max(40, 18 + cols.size() * 29))).append("\n");

        for (String nt : table.keySet()) {
            sb.append(String.format("%-16s", nt));
            for (String c : cols) {
                Production p = table.get(nt).get(c);
                String val = p == null ? "" : p.toString();
                sb.append(String.format("| %-26s", val));
            }
            sb.append("\n");
        }

        sb.append("\nGrammar LL(1): ").append(isLL1() ? "YES" : "NO").append("\n");
        if (!isLL1()) {
            sb.append("Conflicts:\n");
            for (String c : conflicts) sb.append(" - ").append(c).append("\n");
        }
        return sb.toString();
    }
}
