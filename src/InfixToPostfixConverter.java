import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class InfixToPostfixConverter {

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

    private static List<Character> snapshotTopToBottom(Stack<Character> stk) {
        List<Character> snap = new ArrayList<>();
        for (int i = stk.size() - 1; i >= 0; i--) { // top -> bottom
            snap.add(stk.get(i));
        }
        return snap;
    }

    private static void appendToken(StringBuilder out, String token) {
        if (out.length() > 0) out.append(' ');
        out.append(token);
    }

    public static List<Step> convertWithSteps(String exp) {
        if (exp == null) throw new IllegalArgumentException("Expression is null");

        Stack<Character> stk = new Stack<>();
        StringBuilder output = new StringBuilder();
        List<Step> steps = new ArrayList<>();

        for (int i = 0; i < exp.length(); i++) {
            char ch = exp.charAt(i);
            if (ch == ' ') continue;

            // Parse multi-digit number
            if (Character.isDigit(ch)) {
                int j = i;
                while (j < exp.length() && Character.isDigit(exp.charAt(j))) j++;
                String number = exp.substring(i, j);

                appendToken(output, number);
                steps.add(new Step(i, ch, "Number " + number + " → append to output",
                        snapshotTopToBottom(stk), output.toString()));

                i = j - 1; // advance
                continue;
            }

            // Single-letter variable (kept as token)
            if (Character.isLetter(ch)) {
                appendToken(output, String.valueOf(ch));
                steps.add(new Step(i, ch, "Operand → append to output",
                        snapshotTopToBottom(stk), output.toString()));
                continue;
            }

            if (ch == '(') {
                stk.push(ch);
                steps.add(new Step(i, ch, "Push '('",
                        snapshotTopToBottom(stk), output.toString()));
            } else if (ch == ')') {
                while (!stk.isEmpty() && stk.peek() != '(') {
                    char popped = stk.pop();
                    appendToken(output, String.valueOf(popped));
                    steps.add(new Step(i, ch, "Pop '" + popped + "' → output (until '(')",
                            snapshotTopToBottom(stk), output.toString()));
                }
                if (stk.isEmpty()) throw new IllegalArgumentException("Mismatched parentheses: missing '('");

                stk.pop(); // remove '('
                steps.add(new Step(i, ch, "Pop '(' and discard",
                        snapshotTopToBottom(stk), output.toString()));
            } else if (isOperator(ch)) {
                while (!stk.isEmpty() && stk.peek() != '(' && priority(ch) <= priority(stk.peek())) {
                    char popped = stk.pop();
                    appendToken(output, String.valueOf(popped));
                    steps.add(new Step(i, ch, "Pop '" + popped + "' → output (priority)",
                            snapshotTopToBottom(stk), output.toString()));
                }
                stk.push(ch);
                steps.add(new Step(i, ch, "Push operator '" + ch + "'",
                        snapshotTopToBottom(stk), output.toString()));
            } else {
                throw new IllegalArgumentException("Unsupported character: '" + ch + "'");
            }
        }

        while (!stk.isEmpty()) {
            char top = stk.pop();
            if (top == '(') throw new IllegalArgumentException("Mismatched parentheses: missing ')'");
            appendToken(output, String.valueOf(top));
            steps.add(new Step(exp.length(), '\0', "End: pop '" + top + "' → output (drain)",
                    snapshotTopToBottom(stk), output.toString()));
        }

        steps.add(new Step(exp.length(), '\0', "Done",
                snapshotTopToBottom(stk), output.toString()));

        return steps;
    }
}