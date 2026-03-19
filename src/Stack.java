import java.util.ArrayList;
import java.util.List;

public class Stack<T> {
    private final List<T> bag = new ArrayList<>();

    public void push(T x) {
        bag.add(x);
    }

    public T pop() {
        if (bag.isEmpty()) return null;
        return bag.remove(bag.size() - 1);
    }

    public T top() {
        if (bag.isEmpty()) return null;
        return bag.get(bag.size() - 1);
    }

    public boolean isEmpty() {
        return bag.isEmpty();
    }

    public List<T> bottomToTopView() {
        return new ArrayList<>(bag);
    }
}
