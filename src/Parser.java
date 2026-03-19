import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Parser {
    public static class ParseResult {
        public boolean accepted;
        public int errCount;
        public final List<String> traceRows = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
        public Tree.Node root;

        public String traceAsText() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-6s %-36s %-35s %s%n", "Step", "Stack", "Input", "Action"));
            sb.append("-".repeat(110)).append("\n");
            for (String r : traceRows) sb.append(r).append("\n");
            return sb.toString();
        }
    }

    private final Grammar g;
    private final LL1Table table;
    private final FirstFollow ff;

    public Parser(Grammar g, LL1Table table, FirstFollow ff) {
        this.g = g;
        this.table = table;
        this.ff = ff;
    }

    public ParseResult parseTokens(List<String> rawTokens, int lineNo) {
        ParseResult out = new ParseResult();

        List<String> input = new ArrayList<>(rawTokens);
        if (input.isEmpty() || !"$".equals(input.get(input.size() - 1))) {
            input.add("$");
        }

        Stack<String> st = new Stack<>();
        st.push("$");
        st.push(g.getStartSymbol());

        Tree.Node root = new Tree.Node(g.getStartSymbol());
        out.root = root;

        Deque<Tree.Node> pending = new ArrayDeque<>();
        pending.push(root);

        int ip = 0;
        int step = 1;
        int safety = 0;
        boolean acceptedClean = false;

        while (safety < 10000) {
            safety++;

            String X = st.top();
            String a = ip < input.size() ? input.get(ip) : "$";

            if (X == null) {
                out.errors.add("Parser internal issue: stack became null top");
                out.errCount++;
                break;
            }

            if ("$".equals(X) && "$".equals(a)) {
                addTrace(out, step++, st.bottomToTopView(), input.subList(ip, input.size()), "Accept");
                st.pop();
                acceptedClean = true;
                break;
            }

            if ("$".equals(X) && !"$".equals(a)) {
                out.errCount++;
                out.errors.add(ErrorHandler.makeMsg(lineNo, ip + 1, a, Set.of("$"), "Unexpected extra input"));
                addTrace(out, step++, st.bottomToTopView(), input.subList(ip, input.size()), "ERROR: skip input token '" + a + "'");
                ip++;
                continue;
            }

            if (!g.isNonTerminal(X)) {
                if (X.equals(a)) {
                    st.pop();
                    Tree.Node n = pending.isEmpty() ? null : pending.pop();
                    if (n != null) {
                        // matched terminal node already created when expanded
                    }
                    addTrace(out, step++, st.bottomToTopView(), input.subList(ip, input.size()), "Match " + a);
                    ip++;
                } else {
                    out.errCount++;
                    out.errors.add(ErrorHandler.makeMsg(lineNo, ip + 1, a, Set.of(X), "Missing/Unexpected symbol"));
                    addTrace(out, step++, st.bottomToTopView(), input.subList(ip, input.size()), "ERROR: pop terminal '" + X + "' from stack");
                    st.pop(); // panic: delete expected symbol
                    if (!pending.isEmpty()) pending.pop();
                }
                continue;
            }

            Production p = table.get(X, a);
            if (p != null) {
                st.pop();
                Tree.Node parent = pending.isEmpty() ? null : pending.pop();

                List<String> rhs = p.rhsView();
                List<Tree.Node> made = new ArrayList<>();

                if (!(rhs.size() == 1 && "epsilon".equals(rhs.get(0)))) {
                    for (String s : rhs) {
                        Tree.Node child = new Tree.Node(s);
                        if (parent != null) parent.addChild(child);
                        made.add(child);
                    }

                    for (int i = rhs.size() - 1; i >= 0; i--) {
                        st.push(rhs.get(i));
                        pending.push(made.get(i));
                    }
                } else {
                    if (parent != null) parent.addChild(new Tree.Node("epsilon"));
                }

                addTrace(out, step++, st.bottomToTopView(), input.subList(ip, input.size()), p.toString());
            } else {
                // Panic mode style thing: for non-terminal on top and empty entry
                out.errCount++;
                LinkedHashSet<String> expected = new LinkedHashSet<>(ErrorHandler.expectedTerminals(g, table, X));
                out.errors.add(ErrorHandler.makeMsg(lineNo, ip + 1, a, expected, "Empty table entry"));

                Set<String> sync = ff.getFollowMap().get(X);
                if (sync != null && sync.contains(a)) {
                    addTrace(out, step++, st.bottomToTopView(), input.subList(ip, input.size()), "ERROR: pop non-terminal '" + X + "' (sync by FOLLOW)");
                    st.pop();
                    if (!pending.isEmpty()) pending.pop();
                } else {
                    addTrace(out, step++, st.bottomToTopView(), input.subList(ip, input.size()), "ERROR: skip input token '" + a + "' (panic mode)");
                    ip++;
                }

                if (ip >= input.size()) {
                    break;
                }
            }
        }

        out.accepted = acceptedClean && out.errCount == 0;
        return out;
    }

    private static void addTrace(ParseResult out, int step, List<String> stack, List<String> remainingInput, String action) {
        String st = String.join(" ", stack);
        String inp = String.join(" ", remainingInput);
        out.traceRows.add(String.format("%-6d %-36s %-35s %s", step, st, inp, action));
    }
}
