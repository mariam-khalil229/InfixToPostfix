import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class PostfixEvaluatorWithSteps {
    private PostfixEvaluatorWithSteps() {}

    public static class EvalStep {
        public final int index;
        public final String token;
        public final String action;
        public final List<String> stackTopToBottom;

        public EvalStep(int index, String token, String action, List<String> stackTopToBottom) {
            this.index = index;
            this.token = token;
            this.action = action;
            this.stackTopToBottom = stackTopToBottom;
        }
    }

    private static List<String> snapshotTopToBottom(Deque<Double> stk) {
        List<Double> temp = new ArrayList<>(stk);
        List<String> snap = new ArrayList<>();
        for (Double v : temp) snap.add(format(v));
        return snap;
    }

    private static String format(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    public static List<EvalStep> evaluateWithSteps(String postfix) {
        if (postfix == null || postfix.trim().isEmpty()) {
            throw new IllegalArgumentException("Postfix expression is empty.");
        }

        String[] tokens = postfix.trim().split("\\s+");
        Deque<Double> stack = new ArrayDeque<>();
        List<EvalStep> steps = new ArrayList<>();

        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];

            if (t.matches("[-+]?\\d+(\\.\\d+)?")) {
                double val = Double.parseDouble(t);
                stack.push(val);
                steps.add(new EvalStep(i, t, "Number → push " + format(val), snapshotTopToBottom(stack)));
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
            steps.add(new EvalStep(
                    i,
                    t,
                    "Apply " + format(a) + " " + t + " " + format(b) + " = " + format(r) + " → push result",
                    snapshotTopToBottom(stack)
            ));
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid postfix: leftover operands/operators.");
        }

        steps.add(new EvalStep(tokens.length, "(done)", "Final result = " + format(stack.peek()), snapshotTopToBottom(stack)));
        return steps;
    }
}