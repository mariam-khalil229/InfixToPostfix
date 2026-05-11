import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class InfixToPostfixConverter {
    private static final double EPSILON = 1e-10;

    public static class Step {
        public final int index;
        public final char ch;
        public final String action;
        public final List<Character> stackTopToBottom; // snapshot for drawing
        public final String output;

        public Step(int index, char ch, String action, List<Character> stackTopToBottom, String output) {
            this.index = index;
            this.ch = ch;
            this.action = action;
            this.stackTopToBottom = stackTopToBottom;
            this.output = output;
        }
    }

    private static int priority(char c) {
        if (c == '-' || c == '+') return 1;
        if (c == '*' || c == '/') return 2;
        if (c == '^') return 3;
        return 0;
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private static double applyOperator(char operator, double left, double right) {
        return switch (operator) {
            case '+' -> left + right;
            case '-' -> left - right;
            case '*' -> left * right;
            case '/' -> {
                if (Math.abs(right) < EPSILON) {
                    throw new IllegalArgumentException("Division by zero");
                }
                yield left / right;
            }
            case '^' -> Math.pow(left, right);
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };
    }

    private static List<Character> snapshotTopToBottom(Stack<Character> stk) {
        List<Character> snap = new ArrayList<>();
        for (int i = stk.size() - 1; i >= 0; i--) { // top -> bottom
            snap.add(stk.get(i));
        }
        return snap;
    }

    public static List<Step> convertWithSteps(String exp) {
        if (exp == null) throw new IllegalArgumentException("Expression is null");

        Stack<Character> stk = new Stack<>();
        StringBuilder output = new StringBuilder();
        List<Step> steps = new ArrayList<>();

        for (int i = 0; i < exp.length(); i++) {
            char ch = exp.charAt(i);
            if (ch == ' ') continue;

            if (Character.isDigit(ch) || Character.isLetter(ch)) {
                output.append(ch);
                steps.add(new Step(i, ch, "Operand → append to output",
                        snapshotTopToBottom(stk), output.toString()));
            } else if (ch == '(') {
                stk.push(ch);
                steps.add(new Step(i, ch, "Push '('",
                        snapshotTopToBottom(stk), output.toString()));
            } else if (ch == ')') {
                while (!stk.isEmpty() && stk.peek() != '(') {
                    char popped = stk.pop();
                    output.append(popped);
                    steps.add(new Step(i, ch, "Pop '" + popped + "' → output (until '(')",
                            snapshotTopToBottom(stk), output.toString()));
                }
                if (stk.isEmpty()) throw new IllegalArgumentException("Mismatched parentheses: missing '('");

                stk.pop(); // remove '('
                steps.add(new Step(i, ch, "Pop '(' and discard",
                        snapshotTopToBottom(stk), output.toString()));
            } else {
                // operator
                while (!stk.isEmpty() && stk.peek() != '(' && priority(ch) <= priority(stk.peek())) {
                    char popped = stk.pop();
                    output.append(popped);
                    steps.add(new Step(i, ch, "Pop '" + popped + "' → output (priority)",
                            snapshotTopToBottom(stk), output.toString()));
                }
                stk.push(ch);
                steps.add(new Step(i, ch, "Push operator '" + ch + "'",
                        snapshotTopToBottom(stk), output.toString()));
            }
        }

        while (!stk.isEmpty()) {
            char top = stk.pop();
            if (top == '(') throw new IllegalArgumentException("Mismatched parentheses: missing ')'");
            output.append(top);
            steps.add(new Step(exp.length(), '\0', "End: pop '" + top + "' → output (drain)",
                    snapshotTopToBottom(stk), output.toString()));
        }

        steps.add(new Step(exp.length(), '\0', "Done",
                snapshotTopToBottom(stk), output.toString()));

        return steps;
    }

    public static double evaluatePostfix(String postfix) {
        if (postfix == null) throw new IllegalArgumentException("Postfix expression is null");

        String trimmed = postfix.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Postfix expression is empty");

        Stack<Double> values = new Stack<>();
        boolean whitespaceSeparated = trimmed.contains(" ");

        if (whitespaceSeparated) {
            String[] tokens = trimmed.split("\\s+");
            for (String token : tokens) {
                if (token.length() == 1 && isOperator(token.charAt(0))) {
                    if (values.size() < 2) throw new IllegalArgumentException("Invalid postfix expression: not enough operands");
                    double right = values.pop();
                    double left = values.pop();
                    values.push(applyOperator(token.charAt(0), left, right));
                } else {
                    try {
                        values.push(Double.parseDouble(token));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Invalid token in postfix expression: " + token);
                    }
                }
            }
        } else {
            for (int i = 0; i < trimmed.length(); i++) {
                char ch = trimmed.charAt(i);
                if (ch == ' ') continue;

                if (Character.isDigit(ch)) {
                    values.push((double) (ch - '0'));
                } else if (isOperator(ch)) {
                    if (values.size() < 2) throw new IllegalArgumentException("Invalid postfix expression: not enough operands");
                    double right = values.pop();
                    double left = values.pop();
                    values.push(applyOperator(ch, left, right));
                } else {
                    throw new IllegalArgumentException("Invalid token in postfix expression: " + ch);
                }
            }
        }

        if (values.size() != 1) {
            throw new IllegalArgumentException("Invalid postfix expression: leftover operands/operators");
        }
        return values.pop();
    }
}
