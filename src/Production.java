import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Production {
    public final String lhs;
    public final List<String> rhs;

    public Production(String lhs, List<String> rhs) {
        this.lhs = lhs;
        this.rhs = new ArrayList<>(rhs);
    }

    public boolean isEpsilon() {
        return rhs.size() == 1 && "epsilon".equals(rhs.get(0));
    }

    @Override
    public String toString() {
        return lhs + " -> " + String.join(" ", rhs);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Production)) return false;
        Production other = (Production) obj;
        return Objects.equals(lhs, other.lhs) && Objects.equals(rhs, other.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, rhs);
    }

    public List<String> rhsView() {
        return Collections.unmodifiableList(rhs);
    }
}
