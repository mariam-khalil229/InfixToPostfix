import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.Stack;

class ActionLog {//appears in gui
    String algorithmPhase;
    String activeToken;
    ArrayList<String> stackSnapshot;
    ArrayList<String> outputSnapshot;

    ActionLog(String phase, String token, ArrayList<String> stack, ArrayList<String> output) {
        this.algorithmPhase = phase;
        this.activeToken = token;
        this.stackSnapshot = stack;
        this.outputSnapshot = output;
    }
}

class SolutionRecord {
    String finalPostfixString;
    double finalComputedValue;
    ArrayList<ActionLog> processLogs;

    SolutionRecord(String postfix, double answer, ArrayList<ActionLog> logs) {
        this.finalPostfixString = postfix;
        this.finalComputedValue = answer;
        this.processLogs = logs;
    }
}

class ExpressionSolver {

    SolutionRecord processExpression(String infixExpression) {
        ArrayList<ActionLog> historyLogs = new ArrayList<>();

        ArrayList<String> postfixTokens = convertToPostfix(infixExpression, historyLogs);
        String finalPostfixString = String.join(" ", postfixTokens);
        double calculatedResult = calculatePostfix(postfixTokens, historyLogs);

        return new SolutionRecord(finalPostfixString, calculatedResult, historyLogs);
    }

    boolean isNumeric(String strToken) {
        try {
            Double.parseDouble(strToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    boolean isMathSymbol(char symbol) {
        return symbol == '+' || symbol == '-' || symbol == '*' || symbol == '/' || symbol == '^';
    }

    int getPriority(char symbol) {
        if (symbol == '+' || symbol == '-') return 1;
        if (symbol == '*' || symbol == '/') return 2;
        if (symbol == '^') return 3; // Highest priority
        return 0;
    }

    ArrayList<String> convertToPostfix(String infixString, ArrayList<ActionLog> historyLogs) {
        Stack<Character> operatorStack = new Stack<>();
        ArrayList<String> postfixOutput = new ArrayList<>();

        for (int i = 0; i < infixString.length(); i++) {
            char currentChar = infixString.charAt(i);

            if (currentChar == ' ') continue;

            if (Character.isDigit(currentChar) || currentChar == '.') {
                String parsedOperand = "";
                int decimalPointCount = 0;

                while (i < infixString.length()
                        && (Character.isDigit(infixString.charAt(i)) || infixString.charAt(i) == '.')) {

                    if (infixString.charAt(i) == '.') {
                        decimalPointCount++;
                        if (decimalPointCount > 1) throw new RuntimeException("Invalid number format: multiple decimal points");
                    }

                    parsedOperand += infixString.charAt(i);
                    i++;
                }
                i--;

                postfixOutput.add(parsedOperand);
                logConversionState(operatorStack, postfixOutput, historyLogs, parsedOperand);
            }

            else if (currentChar == '(') {
                operatorStack.push(currentChar);
                logConversionState(operatorStack, postfixOutput, historyLogs, String.valueOf(currentChar));
            }

            else if (currentChar == ')') {
                while (!operatorStack.isEmpty() && operatorStack.peek() != '(') {
                    postfixOutput.add(String.valueOf(operatorStack.pop()));
                    logConversionState(operatorStack, postfixOutput, historyLogs, String.valueOf(currentChar));
                }


                if (operatorStack.isEmpty()) {
                    throw new RuntimeException("Mismatched brackets: Missing opening '('");
                }

                operatorStack.pop(); // pop '('

                logConversionState(operatorStack, postfixOutput, historyLogs, String.valueOf(currentChar));
            }

            else if (isMathSymbol(currentChar)) {
                while (!operatorStack.isEmpty()
                        && operatorStack.peek() != '('
                        && (
                        getPriority(operatorStack.peek()) > getPriority(currentChar) ||
                                (getPriority(operatorStack.peek()) == getPriority(currentChar) && currentChar != '^')
                )
                ) {
                    postfixOutput.add(String.valueOf(operatorStack.pop()));
                    logConversionState(operatorStack, postfixOutput, historyLogs, String.valueOf(currentChar));
                }

                operatorStack.push(currentChar);
                logConversionState(operatorStack, postfixOutput, historyLogs, String.valueOf(currentChar));
            }
            else {
                throw new RuntimeException("Invalid character in expression: " + currentChar);
            }
        }


        while (!operatorStack.isEmpty()) {
            char remainingOp = operatorStack.pop();

            if (remainingOp == '(') {
                throw new RuntimeException("Mismatched brackets: Missing closing ')'");
            }

            postfixOutput.add(String.valueOf(remainingOp));
            logConversionState(operatorStack, postfixOutput, historyLogs, "end");
        }

        return postfixOutput;
    }

    double calculatePostfix(ArrayList<String> postfixTokens, ArrayList<ActionLog> historyLogs) {
        Stack<Double> evaluationStack = new Stack<>();

        for (int i = 0; i < postfixTokens.size(); i++) {
            String currentToken = postfixTokens.get(i);

            if (isNumeric(currentToken)) {
                evaluationStack.push(Double.parseDouble(currentToken));
                logEvaluationState(evaluationStack, postfixTokens, historyLogs, currentToken);
            }

            else if (isMathSymbol(currentToken.charAt(0))) {
                if (evaluationStack.size() < 2) {
                    throw new RuntimeException("Invalid expression: Not enough operands for operator '" + currentToken + "'");
                }
                double rightOperand = evaluationStack.pop();
                double leftOperand = evaluationStack.pop();

                char activeOperator = currentToken.charAt(0);
                double operationResult = 0;

                if (activeOperator == '+')
                    operationResult = leftOperand + rightOperand;
                else if (activeOperator == '-')
                    operationResult = leftOperand - rightOperand;
                else if (activeOperator == '*')
                    operationResult = leftOperand * rightOperand;
                else if (activeOperator == '/')
                    operationResult = leftOperand / rightOperand;
                else if (activeOperator == '^')
                    operationResult = Math.pow(leftOperand, rightOperand);

                evaluationStack.push(operationResult);
                logEvaluationState(evaluationStack, postfixTokens, historyLogs, currentToken);
            }
        }

        if (evaluationStack.size() != 1) {
            throw new RuntimeException("Invalid expression: Too many operands");
        }

        return evaluationStack.pop();
    }

    void logConversionState(
            Stack<Character> currentStack,
            ArrayList<String> currentOutput,
            ArrayList<ActionLog> historyLogs,
            String activeToken
    ) {
        ArrayList<String> clonedStack = new ArrayList<>();
        for (Character c : currentStack) clonedStack.add(String.valueOf(c));
        ArrayList<String> clonedOutput = new ArrayList<>(currentOutput);

        historyLogs.add(new ActionLog(
                "Infix to Postfix Conversion",
                activeToken,
                clonedStack,
                clonedOutput
        ));
    }

    void logEvaluationState(
            Stack<Double> currentStack,
            ArrayList<String> postfixTokens,
            ArrayList<ActionLog> historyLogs,
            String activeToken
    ) {
        ArrayList<String> clonedStack = new ArrayList<>();
        for (Double val : currentStack) clonedStack.add(formatFinalNumber(val));
        ArrayList<String> clonedPostfix = new ArrayList<>(postfixTokens);

        historyLogs.add(new ActionLog(
                "Postfix Evaluation",
                activeToken,
                clonedStack,
                clonedPostfix
        ));
    }

    String formatFinalNumber(double value) {
        if (value == (int) value) return String.valueOf((int) value);
        return String.valueOf(value);
    }
}

//---------------------------GUI----------------------------------
public class InfixApp extends Application {

    private TextField inputField;
    private Label phaseLabel, tokenLabel, outputLabel, evalLabel;
    private ListView<String> stackView;

    // Logic Variables
    private ExpressionSolver logicSolver = new ExpressionSolver();
    private SolutionRecord currentSolution = null;
    private int currentStepIndex = -1;

    @Override
    public void start(Stage primaryStage) {
        inputField = new TextField();

        Button buildBtn = new Button("Build");
        Button nextBtn = new Button("Next Step");
        nextBtn.setStyle("-fx-base: #b3e5fc;");

        HBox inputRow = new HBox(10, new Label("Infix:"), inputField, buildBtn, nextBtn);
        inputRow.setAlignment(Pos.CENTER);

        phaseLabel = new Label("Phase: Waiting...");
        phaseLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        tokenLabel = new Label("Current Token: -");
        outputLabel = new Label("Output List: -");
        evalLabel = new Label("Final Answer: -");

        VBox statusBox = new VBox(8, phaseLabel, tokenLabel, outputLabel, evalLabel);
        statusBox.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 15; -fx-border-color: #ccc;");




        stackView = createStyledListView();
        VBox stackContainer = new VBox(5, new Label("Main Stack"), new Label("▼ TOP"), stackView, new Label("BOTTOM"));
        stackContainer.setAlignment(Pos.CENTER);

        VBox root = new VBox(20, inputRow, statusBox, stackContainer);
        root.setPadding(new Insets(25));

        buildBtn.setOnAction(e -> calculateAllSteps(inputField.getText()));
        nextBtn.setOnAction(e -> goNext());

        primaryStage.setTitle("Visualizer");
        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.show();
    }

    private void calculateAllSteps(String rawInput) {
        if (rawInput == null || rawInput.trim().isEmpty()) return;


        try {
            currentSolution = logicSolver.processExpression(rawInput);
            currentStepIndex = -1;

            phaseLabel.setText("Phase: Built successfully! Click Next.");
            tokenLabel.setText("Current Token: -");
            outputLabel.setText("Output List: -");
            evalLabel.setText("Final Answer: -");
            stackView.getItems().clear();

        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage());
            alert.showAndWait();
        }
    }

    private void goNext() {
        if (currentSolution == null || currentStepIndex >= currentSolution.processLogs.size() - 1) return;
        currentStepIndex++;
        updateUIFromLog(currentSolution.processLogs.get(currentStepIndex));
    }

    private void updateUIFromLog(ActionLog logData) {
        phaseLabel.setText("Phase: " + logData.algorithmPhase);
        tokenLabel.setText("Current Token: " + logData.activeToken);

        outputLabel.setText("Output List: " + String.join(" ", logData.outputSnapshot));
        if(logData.stackSnapshot.isEmpty()) {
            stackView.getItems().add("-");
        }
        else{
            stackView.getItems().clear();
            for (int i = logData.stackSnapshot.size() - 1; i >= 0; i--) {
                stackView.getItems().add(logData.stackSnapshot.get(i));
            }
        }




        if (currentStepIndex == currentSolution.processLogs.size() - 1) {
            evalLabel.setText("Final Answer: " + logicSolver.formatFinalNumber(currentSolution.finalComputedValue));
        } else {
            evalLabel.setText("Final Answer: -");
        }
    }

    private ListView<String> createStyledListView() {
        ListView<String> lv = new ListView<>();
        lv.setPrefSize(160, 250);
        lv.setCellFactory(param -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle("-fx-background-color: transparent;"); }
                else { setText(item); setAlignment(Pos.CENTER); setStyle("-fx-border-color: #333; -fx-background-color: white; -fx-padding: 5; -fx-font-weight: bold;"); }
            }
        });
        return lv;
    }

    public static void main(String[] args) { launch(args); }
}