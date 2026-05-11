import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class InfixToPostfixApp extends Application {

    // ---------- DATA ----------
    static class Step {
        final int index;
        final char ch;
        final List<Character> stackTopToBottom;
        final String output;
        Step(int index, char ch, List<Character> stackTopToBottom, String output) {
            this.index = index;
            this.ch = ch;
            this.stackTopToBottom = stackTopToBottom;
            this.output = output;
        }
    }

    static class EvalStep {
        final int index;
        final String token;
        final List<String> stackTopToBottom;
        EvalStep(int index, String token, List<String> stackTopToBottom) {
            this.index = index;
            this.token = token;
            this.stackTopToBottom = stackTopToBottom;
        }
    }

    // ---------- CONVERTER ----------
    static class InfixToPostfixConverter {

        static boolean isOp(char c) {
            switch (c) {
                case '+':
                case '-':
                case '*':
                case '/':
                case '^':
                    return true;
                default:
                    return false;
            }
        }

        static int priority(char c) {
            return (c=='+'||c=='-')?1:(c=='*'||c=='/')?2:(c=='^')?3:0;
        }

        static List<Character> snap(Stack<Character> s){
            List<Character> out = new ArrayList<>();
            Stack<Character> temp = new Stack<>();

            while (!s.isEmpty()) {
                char v = s.pop();
                out.add(v);     // top to bottom
                temp.push(v);
            }
            while (!temp.isEmpty()) {
                s.push(temp.pop()); // restore original stack
            }
            return out;
        }

        static void appendTok(StringBuilder out, String token){
            if (out.length()>0) out.append(' ');
            out.append(token);
        }

        static List<Step> convertWithSteps(String exp){
            if (exp==null) throw new IllegalArgumentException("Expression is null");
            Stack<Character> stk = new Stack<>();
            StringBuilder out = new StringBuilder();
            List<Step> steps = new ArrayList<>();

            for (int i=0;i<exp.length();i++){
                char ch = exp.charAt(i);
                if (ch==' ') continue;

                if (Character.isDigit(ch)){
                    int j=i;
                    while (j<exp.length() && Character.isDigit(exp.charAt(j))) j++;
                    appendTok(out, exp.substring(i,j));
                    steps.add(new Step(i, ch, snap(stk), out.toString()));
                    i=j-1;
                    continue;
                }

                if (ch=='('){ stk.push(ch); steps.add(new Step(i,ch,snap(stk),out.toString())); }
                else if (ch==')'){
                    while (!stk.isEmpty() && stk.peek()!='('){
                        appendTok(out, String.valueOf(stk.pop()));
                        steps.add(new Step(i, ch, snap(stk), out.toString()));
                    }
                    if (stk.isEmpty()) throw new IllegalArgumentException("Mismatched parentheses: missing '('");
                    stk.pop();
                    steps.add(new Step(i, ch, snap(stk), out.toString()));
                } else if (isOp(ch)){
                    while (!stk.isEmpty() && stk.peek()!='(' && priority(ch)<=priority(stk.peek())){
                        appendTok(out, String.valueOf(stk.pop()));
                        steps.add(new Step(i, ch, snap(stk), out.toString()));
                    }
                    stk.push(ch);
                    steps.add(new Step(i, ch, snap(stk), out.toString()));
                } else {
                    throw new IllegalArgumentException(
                            "Only numbers, operators (+-*/^), spaces, and parentheses are allowed. Found: '" + ch + "'"
                    );
                }
            }

            while (!stk.isEmpty()){
                char top = stk.pop();
                if (top=='(') throw new IllegalArgumentException("Mismatched parentheses: missing ')'");
                appendTok(out, String.valueOf(top));
                steps.add(new Step(exp.length(), '\0', snap(stk), out.toString()));
            }
            steps.add(new Step(exp.length(), '\0', snap(stk), out.toString()));
            return steps;
        }
    }

    // ---------- EVALUATOR ----------
    static class PostfixEvaluatorWithSteps {
        static List<String> snap(Stack<Double> s){
            List<String> out = new ArrayList<>();
            Stack<Double> temp = new Stack<>();

            while (!s.isEmpty()) {
                double v = s.pop();
                out.add(format(v)); // top to bottom
                temp.push(v);
            }
            while (!temp.isEmpty()) {
                s.push(temp.pop()); // restore
            }
            return out;
        }

        static String format(double v){ return (v==Math.rint(v))?String.valueOf((long)v):String.valueOf(v); }

        static List<EvalStep> evaluateWithSteps(String postfix){
            if (postfix==null || postfix.trim().isEmpty())
                throw new IllegalArgumentException("Postfix expression is empty.");

            String[] tokens = postfix.trim().split("\\s+");
            Stack<Double> stack = new Stack<>();
            List<EvalStep> steps = new ArrayList<>();

            for (int i=0;i<tokens.length;i++){
                String t = tokens[i];

                if (t.matches("[-+]?\\d+(\\.\\d+)?")){
                    stack.push(Double.parseDouble(t));
                    steps.add(new EvalStep(i, t, snap(stack)));
                    continue;
                }

                if (stack.size()<2)
                    throw new IllegalArgumentException("Invalid postfix: not enough operands for operator '" + t + "'.");

                double b = stack.pop(), a = stack.pop();
                double r = switch (t) {
                    case "+" -> a+b;
                    case "-" -> a-b;
                    case "*" -> a*b;
                    case "/" -> { if (b==0.0) throw new ArithmeticException("Division by zero."); yield a/b; }
                    case "^" -> Math.pow(a,b);
                    default -> throw new IllegalArgumentException("Unknown operator: '" + t + "'");
                };
                stack.push(r);
                steps.add(new EvalStep(i, t, snap(stack)));
            }

            if (stack.size()!=1)
                throw new IllegalArgumentException("Invalid postfix: leftover operands/operators.");

            steps.add(new EvalStep(tokens.length, "(done)", snap(stack)));
            return steps;
        }

        static double evaluate(String postfix){
            if (postfix==null || postfix.trim().isEmpty())
                throw new IllegalArgumentException("Postfix expression is empty.");

            Stack<Double> stack = new Stack<>();
            for (String t : postfix.trim().split("\\s+")){
                if (t.isEmpty()) continue;

                if (t.matches("[-+]?\\d+(\\.\\d+)?")) {
                    stack.push(Double.parseDouble(t));
                    continue;
                }

                if (stack.size()<2)
                    throw new IllegalArgumentException("Invalid postfix: not enough operands for operator '" + t + "'.");

                double b = stack.pop(), a = stack.pop();
                double r = switch (t) {
                    case "+" -> a+b;
                    case "-" -> a-b;
                    case "*" -> a*b;
                    case "/" -> { if (b==0.0) throw new ArithmeticException("Division by zero."); yield a/b; }
                    case "^" -> Math.pow(a,b);
                    default -> throw new IllegalArgumentException("Unknown operator: '" + t + "'");
                };
                stack.push(r);
            }

            if (stack.size()!=1) throw new IllegalArgumentException("Invalid postfix: leftover operands/operators.");
            return stack.pop();
        }
    }

    // ---------- UI STATE ----------
    private int stepIndex = 0, evalStepIndex = 0;
    private List<Step> steps;
    private List<EvalStep> evalSteps;
    private boolean evalMode = false;

    private Label charLabel, outputLabel, evalLabel, resultLabel;
    private VBox stackBoxes, evalStackBoxes;

    @Override
    public void start(Stage stage) {
        TextField input = new TextField();
        input.setPromptText("Example: 9+5-10*8/(9+15)");

        Button buildBtn = new Button("Build");
        Button prevBtn = new Button("Prev");
        Button nextBtn = new Button("Next");
        prevBtn.setDisable(true); nextBtn.setDisable(true);

        charLabel = new Label("Current char: "); charLabel.setFont(Font.font(16));
        outputLabel = new Label("Postfix output: "); outputLabel.setFont(Font.font(16));

        evalLabel = new Label("Eval: "); evalLabel.setFont(Font.font(16));
        resultLabel = new Label("Evaluation: "); resultLabel.setFont(Font.font(16));

        Label errorLabel = new Label(); errorLabel.setTextFill(Color.DARKRED);

        stackBoxes = new VBox(6); stackBoxes.setPadding(new Insets(10)); stackBoxes.setAlignment(Pos.TOP_CENTER);
        evalStackBoxes = new VBox(6); evalStackBoxes.setPadding(new Insets(10)); evalStackBoxes.setAlignment(Pos.TOP_CENTER);

        BorderPane stackPane = buildStackPane("Operator Stack", stackBoxes);
        BorderPane evalStackPane = buildStackPane("Evaluation Stack", evalStackBoxes);

        Runnable renderConversion = () -> {
            if (steps==null || steps.isEmpty()) return;
            Step s = steps.get(stepIndex);
            String shownChar = (s.ch=='\0') ? "(none)" : "'" + s.ch + "'";
            charLabel.setText("Current char: " + shownChar);
            outputLabel.setText("Postfix output: " + s.output);
            drawStack(stackBoxes, s.stackTopToBottom);
            prevBtn.setDisable(stepIndex==0);
            nextBtn.setDisable(stepIndex>=steps.size()-1);
        };

        Runnable renderEval = () -> {
            if (evalSteps==null || evalSteps.isEmpty()) return;
            EvalStep s = evalSteps.get(evalStepIndex);
            evalLabel.setText("Eval: " + s.token);
            drawStringStack(evalStackBoxes, s.stackTopToBottom);

            if (evalStepIndex==evalSteps.size()-1) {
                String postfix = steps.get(steps.size()-1).output;
                resultLabel.setText("Evaluation: " + PostfixEvaluatorWithSteps.evaluate(postfix));
            } else {
                resultLabel.setText("Evaluation: ");
            }

            prevBtn.setDisable(evalStepIndex==0);
            nextBtn.setDisable(evalStepIndex>=evalSteps.size()-1);
        };

        buildBtn.setOnAction(e -> {
            try {
                steps = InfixToPostfixConverter.convertWithSteps(input.getText());
                stepIndex = 0; evalMode = false;
                errorLabel.setText("");
                renderConversion.run();

                evalSteps = null; evalStepIndex = 0;
                evalLabel.setText("Eval: ");
                evalStackBoxes.getChildren().clear();
                resultLabel.setText("Evaluation: ");

                prevBtn.setDisable(false); nextBtn.setDisable(false);
            } catch (Exception ex) {
                steps = null; evalSteps = null;
                stepIndex = 0; evalStepIndex = 0; evalMode = false;
                stackBoxes.getChildren().clear(); evalStackBoxes.getChildren().clear();
                charLabel.setText("Current char: ");
                outputLabel.setText("Postfix output: ");
                evalLabel.setText("Eval: ");
                resultLabel.setText("Evaluation: ");
                errorLabel.setText(ex.getMessage());
                prevBtn.setDisable(true); nextBtn.setDisable(true);
            }
        });

        prevBtn.setOnAction(e -> {
            if (!evalMode) { if (steps!=null && stepIndex>0) stepIndex--; renderConversion.run(); }
            else { if (evalSteps!=null && evalStepIndex>0) evalStepIndex--; renderEval.run(); }
        });

        nextBtn.setOnAction(e -> {
            if (!evalMode) {
                if (steps==null) return;
                if (stepIndex<steps.size()-1) { stepIndex++; renderConversion.run(); }
                if (stepIndex==steps.size()-1) {
                    String postfix = steps.get(steps.size()-1).output;
                    evalSteps = PostfixEvaluatorWithSteps.evaluateWithSteps(postfix);
                    evalStepIndex = 0; evalMode = true;
                    renderEval.run();
                }
            } else {
                if (evalSteps!=null && evalStepIndex<evalSteps.size()-1) {
                    evalStepIndex++; renderEval.run();
                }
            }
        });

        HBox controls = new HBox(10, buildBtn, prevBtn, nextBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(10,
                new Label("Infix Expression:"), input, controls,
                charLabel, outputLabel, evalLabel, resultLabel, errorLabel);
        left.setPadding(new Insets(12));
        left.setPrefWidth(520);

        HBox root = new HBox(12, left, stackPane, evalStackPane);
        root.setPadding(new Insets(12));

        stage.setTitle("Infix → Postfix + Evaluation (Visualization)");
        stage.setScene(new Scene(root, 1040, 360));
        stage.show();
    }

    private BorderPane buildStackPane(String title, VBox stackBoxes) {
        Label stackTitle = new Label(title); stackTitle.setFont(Font.font(16));
        Label topHint = new Label("TOP"); topHint.setTextFill(Color.GRAY);
        Label bottomHint = new Label("BOTTOM"); bottomHint.setTextFill(Color.GRAY);

        BorderPane pane = new BorderPane();
        pane.setTop(new VBox(4, stackTitle, topHint));
        BorderPane.setAlignment(stackTitle, Pos.CENTER);
        BorderPane.setAlignment(topHint, Pos.CENTER);

        pane.setCenter(stackBoxes);
        pane.setBottom(bottomHint);
        BorderPane.setAlignment(bottomHint, Pos.CENTER);

        pane.setPrefWidth(200);
        pane.setStyle("-fx-border-color: #444; -fx-border-width: 2; -fx-background-color: #fafafa;");
        return pane;
    }

    private void drawStack(VBox box, List<Character> stackTopToBottom) {
        box.getChildren().clear();
        if (stackTopToBottom==null || stackTopToBottom.isEmpty()) {
            Label empty = new Label("(empty)"); empty.setTextFill(Color.GRAY); box.getChildren().add(empty); return;
        }
        for (int i=0;i<stackTopToBottom.size();i++){
            char c = stackTopToBottom.get(i);
            Label node = new Label(String.valueOf(c));
            node.setMinWidth(80); node.setAlignment(Pos.CENTER); node.setFont(Font.font(18)); node.setPadding(new Insets(6));
            node.setStyle(i==0
                    ? "-fx-border-color:#0b5;-fx-border-width:3;-fx-background-color:#eafff3;"
                    : "-fx-border-color:black;-fx-border-width:2;-fx-background-color:white;");
            box.getChildren().add(node);
        }
    }

    private void drawStringStack(VBox box, List<String> stackTopToBottom) {
        box.getChildren().clear();
        if (stackTopToBottom==null || stackTopToBottom.isEmpty()) {
            Label empty = new Label("(empty)"); empty.setTextFill(Color.GRAY); box.getChildren().add(empty); return;
        }
        for (int i=0;i<stackTopToBottom.size();i++){
            String s = stackTopToBottom.get(i);
            Label node = new Label(s);
            node.setMinWidth(80); node.setAlignment(Pos.CENTER); node.setFont(Font.font(18)); node.setPadding(new Insets(6));
            node.setStyle(i==0
                    ? "-fx-border-color:#0b5;-fx-border-width:3;-fx-background-color:#eafff3;"
                    : "-fx-border-color:black;-fx-border-width:2;-fx-background-color:white;");
            box.getChildren().add(node);
        }
    }

    public static void main(String[] args) { launch(args); }
}