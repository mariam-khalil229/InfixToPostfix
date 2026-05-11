import java.util.ArrayDeque;
import java.util.Deque;

public final class PostfixEvaluator {
    private PostfixEvaluator() {}

    public static double evaluate(String postfix) {
        if (postfix == null || postfix.trim().isEmpty()) {
            throw new IllegalArgumentException("Postfix expression is empty.");
        }

        Deque<Double> stack = new ArrayDeque<>();
        String[] tokens = postfix.trim().split("\\s+");

        for (String t : tokens) {
            if (t.isEmpty()) continue;

            if (t.matches("[-+]?\\d+(\\.\\d+)?")) {
                stack.push(Double.parseDouble(t));
                continue;
            }

            if (t.matches("[A-Za-z]")) {
                throw new IllegalArgumentException("Cannot evaluate variables like '" + t + "'. Use numbers only.");
            }

            if (stack.size() < 2) {
                throw new IllegalArgumentException("Invalid postfix: not enough operands for operator '" + t + "'.");
            }

            double b = stack.pop();
            double a = stack.pop();

            double r;
            switch (t) {
                case "+" -> r = a + b;
                case "-" -> r = a - b;
                case "*" -> r = a * b;
                case "/" -> {
                    if (b == 0.0) throw new ArithmeticException("Division by zero.");
                    r = a / b;
                }
                case "^" -> r = Math.pow(a, b);
                default -> throw new IllegalArgumentException("Unknown operator: '" + t + "'");
            }

            stack.push(r);
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid postfix: leftover operands/operators.");
        }

        return stack.pop();
    }
}