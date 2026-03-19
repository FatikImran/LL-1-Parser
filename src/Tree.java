import java.util.ArrayList;
import java.util.List;

public class Tree {
    public static class Node {
        public final String symbol;
        public final List<Node> kids = new ArrayList<>();

        public Node(String symbol) {
            this.symbol = symbol;
        }

        public void addChild(Node c) {
            kids.add(c);
        }
    }

    public static String toIndented(Node root) {
        StringBuilder sb = new StringBuilder();
        walk(root, "", true, sb);
        return sb.toString();
    }

    private static void walk(Node n, String pref, boolean tail, StringBuilder sb) {
        sb.append(pref).append(tail ? "└── " : "├── ").append(n.symbol).append("\n");
        for (int i = 0; i < n.kids.size(); i++) {
            walk(n.kids.get(i), pref + (tail ? "    " : "│   "), i == n.kids.size() - 1, sb);
        }
    }

    public static void preorder(Node n, List<String> out) {
        out.add(n.symbol);
        for (Node c : n.kids) preorder(c, out);
    }

    public static void postorder(Node n, List<String> out) {
        for (Node c : n.kids) postorder(c, out);
        out.add(n.symbol);
    }
}
