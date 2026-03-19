import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class FirstFollow {
    private final Grammar g;
    private final LinkedHashMap<String, LinkedHashSet<String>> first = new LinkedHashMap<>();
    private final LinkedHashMap<String, LinkedHashSet<String>> follow = new LinkedHashMap<>();

    public FirstFollow(Grammar g) {
        this.g = g;
        for (String nt : g.getProdMap().keySet()) {
            first.put(nt, new LinkedHashSet<>());
            follow.put(nt, new LinkedHashSet<>());
        }
    }

    public void computeAll() {
        computeFIRST();
        computeFOLLOW();
    }

    public Map<String, LinkedHashSet<String>> getFirstMap() {
        return first;
    }

    public Map<String, LinkedHashSet<String>> getFollowMap() {
        return follow;
    }

    public LinkedHashSet<String> firstOfSequence(List<String> seq) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (seq.isEmpty()) {
            out.add("epsilon");
            return out;
        }

        boolean allCanEps = true;
        for (String sym : seq) {
            LinkedHashSet<String> fs = firstOfSymbol(sym);
            for (String t : fs) {
                if (!"epsilon".equals(t)) out.add(t);
            }
            if (!fs.contains("epsilon")) {
                allCanEps = false;
                break;
            }
        }

        if (allCanEps) out.add("epsilon");
        return out;
    }

    public LinkedHashSet<String> firstOfSymbol(String sym) {
        if ("epsilon".equals(sym)) {
            return new LinkedHashSet<>(List.of("epsilon"));
        }
        if (!g.isNonTerminal(sym)) {
            return new LinkedHashSet<>(List.of(sym));
        }
        return new LinkedHashSet<>(first.get(sym));
    }

    private void computeFIRST() {
        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, List<List<String>>> e : g.getProdMap().entrySet()) {
                String A = e.getKey();
                LinkedHashSet<String> fA = first.get(A);

                for (List<String> rhs : e.getValue()) {
                    if (rhs.size() == 1 && "epsilon".equals(rhs.get(0))) {
                        if (fA.add("epsilon")) changed = true;
                        continue;
                    }

                    boolean allEps = true;
                    for (String X : rhs) {
                        LinkedHashSet<String> fX = firstOfSymbol(X);
                        for (String t : fX) {
                            if (!"epsilon".equals(t) && fA.add(t)) {
                                changed = true;
                            }
                        }
                        if (!fX.contains("epsilon")) {
                            allEps = false;
                            break;
                        }
                    }

                    if (allEps && fA.add("epsilon")) {
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    private void computeFOLLOW() {
        follow.get(g.getStartSymbol()).add("$");

        boolean changed;
        do {
            changed = false;
            for (Map.Entry<String, List<List<String>>> e : g.getProdMap().entrySet()) {
                String A = e.getKey();
                for (List<String> rhs : e.getValue()) {
                    for (int i = 0; i < rhs.size(); i++) {
                        String B = rhs.get(i);
                        if (!g.isNonTerminal(B)) continue;

                        List<String> beta = new ArrayList<>();
                        for (int k = i + 1; k < rhs.size(); k++) beta.add(rhs.get(k));

                        LinkedHashSet<String> firstBeta = firstOfSequence(beta);
                        for (String t : firstBeta) {
                            if (!"epsilon".equals(t) && follow.get(B).add(t)) {
                                changed = true;
                            }
                        }

                        if (beta.isEmpty() || firstBeta.contains("epsilon")) {
                            for (String x : follow.get(A)) {
                                if (follow.get(B).add(x)) changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);
    }

    public String prettySets() {
        StringBuilder sb = new StringBuilder();
        sb.append("FIRST Sets\n");
        sb.append("==============================\n");
        for (String nt : first.keySet()) {
            sb.append(String.format("%-18s : %s%n", "FIRST(" + nt + ")", first.get(nt)));
        }

        sb.append("\nFOLLOW Sets\n");
        sb.append("==============================\n");
        for (String nt : follow.keySet()) {
            sb.append(String.format("%-18s : %s%n", "FOLLOW(" + nt + ")", follow.get(nt)));
        }
        return sb.toString();
    }
}
