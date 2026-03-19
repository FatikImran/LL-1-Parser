import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GrammarTransformer {

    public static Grammar applyLeftFactoring(Grammar src) {
        Grammar g = src.deepCopy();
        boolean changed;

        do {
            changed = false;
            List<String> nts = new ArrayList<>(g.getProdMap().keySet());
            for (String A : nts) {
                List<List<String>> alts = g.getProdMap().get(A);
                FactoringPick pick = findBestFactoring(alts);
                if (pick == null) continue;

                String newNt = freshName(g, A + "Prime");
                List<List<String>> Anew = new ArrayList<>();
                List<List<String>> primeAlts = new ArrayList<>();

                for (int idx = 0; idx < alts.size(); idx++) {
                    List<String> rhs = alts.get(idx);
                    if (pick.groupIndexes.contains(idx)) {
                        List<String> suffix = rhs.subList(pick.prefix.size(), rhs.size());
                        if (suffix.isEmpty()) {
                            primeAlts.add(new ArrayList<>(List.of("epsilon")));
                        } else {
                            primeAlts.add(new ArrayList<>(suffix));
                        }
                    } else {
                        Anew.add(new ArrayList<>(rhs));
                    }
                }

                List<String> prefThenPrime = new ArrayList<>(pick.prefix);
                prefThenPrime.add(newNt);
                Anew.add(prefThenPrime);

                g.getProdMap().put(A, dedupAlternatives(Anew));
                g.getProdMap().put(newNt, dedupAlternatives(primeAlts));
                g.rebuildNonTerminalSetFromKeys();
                g.refreshTerminals();

                changed = true;
                break; // restart from first non-terminal
            }
        } while (changed);

        return g;
    }

    public static Grammar removeLeftRecursion(Grammar src) {
        Grammar g = src.deepCopy();
        LinkedHashMap<String, List<List<String>>> P = g.getProdMap();
        List<String> order = new ArrayList<>(P.keySet());

        for (int i = 0; i < order.size(); i++) {
            String Ai = order.get(i);

            for (int j = 0; j < i; j++) {
                String Aj = order.get(j);
                List<List<String>> aiAlts = P.get(Ai);
                List<List<String>> replaced = new ArrayList<>();

                for (List<String> rhs : aiAlts) {
                    if (!rhs.isEmpty() && rhs.get(0).equals(Aj)) {
                        List<String> gamma = rhs.subList(1, rhs.size());
                        for (List<String> delta : P.get(Aj)) {
                            List<String> combo = new ArrayList<>();
                            combo.addAll(delta);
                            combo.addAll(gamma);
                            replaced.add(combo);
                        }
                    } else {
                        replaced.add(new ArrayList<>(rhs));
                    }
                }
                P.put(Ai, dedupAlternatives(replaced));
            }

            eliminateDirectFor(g, Ai, order);
        }

        g.rebuildNonTerminalSetFromKeys();
        g.refreshTerminals();
        return g;
    }

    private static void eliminateDirectFor(Grammar g, String A, List<String> order) {
        List<List<String>> alts = g.getProdMap().get(A);
        List<List<String>> recursive = new ArrayList<>();
        List<List<String>> nonRecursive = new ArrayList<>();

        for (List<String> rhs : alts) {
            if (!rhs.isEmpty() && rhs.get(0).equals(A)) {
                recursive.add(new ArrayList<>(rhs.subList(1, rhs.size())));
            } else {
                nonRecursive.add(new ArrayList<>(rhs));
            }
        }

        if (recursive.isEmpty()) return;

        String Aprime = freshName(g, A + "Prime");

        List<List<String>> newA = new ArrayList<>();
        if (nonRecursive.isEmpty()) {
            // weird grammar case, but we still make it workable for parser
            newA.add(new ArrayList<>(List.of(Aprime)));
        } else {
            for (List<String> beta : nonRecursive) {
                List<String> rhs;
                if (beta.size() == 1 && "epsilon".equals(beta.get(0))) {
                    rhs = new ArrayList<>(List.of(Aprime));
                } else {
                    rhs = new ArrayList<>(beta);
                    rhs.add(Aprime);
                }
                newA.add(rhs);
            }
        }

        List<List<String>> newAprime = new ArrayList<>();
        for (List<String> alpha : recursive) {
            List<String> rhs = new ArrayList<>(alpha);
            rhs.add(Aprime);
            newAprime.add(rhs);
        }
        newAprime.add(new ArrayList<>(List.of("epsilon")));

        g.getProdMap().put(A, dedupAlternatives(newA));
        g.getProdMap().put(Aprime, dedupAlternatives(newAprime));

        if (!order.contains(Aprime)) order.add(Aprime);
    }

    private static String freshName(Grammar g, String base) {
        String name = base;
        int i = 2;
        while (g.getProdMap().containsKey(name)) {
            name = base + i;
            i++;
        }
        return name;
    }

    private static List<List<String>> dedupAlternatives(List<List<String>> arr) {
        List<List<String>> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (List<String> rhs : arr) {
            String key = String.join("\u0001", rhs);
            if (seen.add(key)) {
                out.add(new ArrayList<>(rhs));
            }
        }
        return out;
    }

    private static FactoringPick findBestFactoring(List<List<String>> alts) {
        if (alts == null || alts.size() < 2) return null;
        FactoringPick best = null;

        // all pairs, get common prefix and then collect full group for that prefix
        for (int i = 0; i < alts.size(); i++) {
            for (int j = i + 1; j < alts.size(); j++) {
                List<String> p = commonPrefix(alts.get(i), alts.get(j));
                if (p.isEmpty()) continue;
                Set<Integer> grp = new LinkedHashSet<>();
                for (int k = 0; k < alts.size(); k++) {
                    if (startsWith(alts.get(k), p)) grp.add(k);
                }
                if (grp.size() < 2) continue;

                FactoringPick now = new FactoringPick(p, grp);
                if (best == null || now.prefix.size() > best.prefix.size() ||
                    (now.prefix.size() == best.prefix.size() && now.groupIndexes.size() > best.groupIndexes.size())) {
                    best = now;
                }
            }
        }
        return best;
    }

    private static List<String> commonPrefix(List<String> a, List<String> b) {
        int n = Math.min(a.size(), b.size());
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!a.get(i).equals(b.get(i))) break;
            out.add(a.get(i));
        }
        return out;
    }

    private static boolean startsWith(List<String> src, List<String> pref) {
        if (pref.size() > src.size()) return false;
        for (int i = 0; i < pref.size(); i++) {
            if (!src.get(i).equals(pref.get(i))) return false;
        }
        return true;
    }

    private static class FactoringPick {
        final List<String> prefix;
        final Set<Integer> groupIndexes;

        FactoringPick(List<String> prefix, Set<Integer> groupIndexes) {
            this.prefix = new ArrayList<>(prefix);
            this.groupIndexes = new LinkedHashSet<>(groupIndexes);
        }
    }
}
