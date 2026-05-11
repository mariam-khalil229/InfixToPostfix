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

import java.util.List;

public class InfixToPostfixApp extends Application {

    private int stepIndex = 0;
    private List<InfixToPostfixConverter.Step> steps;

    private int evalStepIndex = 0;
    private List<PostfixEvaluatorWithSteps.EvalStep> evalSteps;

    // mode: false = conversion, true = evaluation
    private boolean evalMode = false;

    private Label charLabel;
    private Label actionLabel;
    private Label outputLabel;

    private Label evalLabel;
    private Label evalActionLabel;
    private Label resultLabel;

    private VBox stackBoxes;      // operator stack
    private VBox evalStackBoxes;  // evaluation stack

    @Override
    public void start(Stage stage) {
        TextField input = new TextField();
        input.setPromptText("Example: 9+5-10*8/(9+15)");

        Button buildBtn = new Button("Build");
        Button prevBtn = new Button("Prev");
        Button nextBtn = new Button("Next");
        prevBtn.setDisable(true);
        nextBtn.setDisable(true);

        charLabel = new Label("Current char: ");
        charLabel.setFont(Font.font(16));

        actionLabel = new Label("Action: ");
        outputLabel = new Label("Postfix output: ");
        outputLabel.setFont(Font.font(16));

        evalLabel = new Label("Eval: ");
        evalLabel.setFont(Font.font(16));

        evalActionLabel = new Label("Eval action: ");

        resultLabel = new Label("Evaluation: ");
        resultLabel.setFont(Font.font(16));

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.DARKRED);

        // Operator stack
        stackBoxes = new VBox(6);
        stackBoxes.setPadding(new Insets(10));
        stackBoxes.setAlignment(Pos.TOP_CENTER);

        Label stackTitle = new Label("Operator Stack");
        stackTitle.setFont(Font.font(16));

        Label topHint = new Label("TOP");
        topHint.setTextFill(Color.GRAY);

        Label bottomHint = new Label("BOTTOM");
        bottomHint.setTextFill(Color.GRAY);

        BorderPane stackPane = new BorderPane();
        stackPane.setTop(new VBox(4, stackTitle, topHint));
        BorderPane.setAlignment(stackTitle, Pos.CENTER);
        BorderPane.setAlignment(topHint, Pos.CENTER);

        stackPane.setCenter(stackBoxes);
        stackPane.setBottom(bottomHint);
        BorderPane.setAlignment(bottomHint, Pos.CENTER);

        stackPane.setPrefWidth(200);
        stackPane.setStyle(
                "-fx-border-color: #444; " +
                        "-fx-border-width: 2; " +
                        "-fx-background-color: #fafafa;"
        );

        // Evaluation stack (new)
        evalStackBoxes = new VBox(6);
        evalStackBoxes.setPadding(new Insets(10));
        evalStackBoxes.setAlignment(Pos.TOP_CENTER);

        BorderPane evalStackPane = buildStackPane("Evaluation Stack", evalStackBoxes);

        Runnable renderConversion = () -> {
            if (steps == null || steps.isEmpty()) return;

            var s = steps.get(stepIndex);

            String shownChar = (s.ch == '\0') ? "(none)" : "'" + s.ch + "'";
            charLabel.setText("Current char: " + shownChar + "   (step " + (stepIndex + 1) + "/" + steps.size() + ")");
            actionLabel.setText("Action: " + s.action);
            outputLabel.setText("Postfix output: " + s.output);

            drawStack(s.stackTopToBottom);

            prevBtn.setDisable(stepIndex == 0);
            nextBtn.setDisable(stepIndex >= steps.size() - 1);
        };

        Runnable renderEval = () -> {
            if (evalSteps == null || evalSteps.isEmpty()) return;

            var s = evalSteps.get(evalStepIndex);
            evalLabel.setText("Eval: " + s.token + "   (step " + (evalStepIndex + 1) + "/" + evalSteps.size() + ")");
            evalActionLabel.setText("Eval action: " + s.action);

            drawStringStack(evalStackBoxes, s.stackTopToBottom);

            prevBtn.setDisable(evalStepIndex == 0);
            nextBtn.setDisable(evalStepIndex >= evalSteps.size() - 1);
        };

        buildBtn.setOnAction(e -> {
            try {
                steps = InfixToPostfixConverter.convertWithSteps(input.getText());
                stepIndex = 0;

                errorLabel.setText("");
                evalMode = false;

                renderConversion.run();

                // reset evaluation
                evalSteps = null;
                evalStepIndex = 0;
                evalLabel.setText("Eval: ");
                evalActionLabel.setText("Eval action: ");
                evalStackBoxes.getChildren().clear();
                resultLabel.setText("Evaluation: ");

                prevBtn.setDisable(false);
                nextBtn.setDisable(false);
            } catch (Exception ex) {
                steps = null;
                stepIndex = 0;
                evalSteps = null;
                evalStepIndex = 0;
                evalMode = false;

                stackBoxes.getChildren().clear();
                evalStackBoxes.getChildren().clear();

                charLabel.setText("Current char: ");
                actionLabel.setText("Action: ");
                outputLabel.setText("Postfix output: ");
                evalLabel.setText("Eval: ");
                evalActionLabel.setText("Eval action: ");
                resultLabel.setText("Evaluation: ");

                errorLabel.setText(ex.getMessage());

                prevBtn.setDisable(true);
                nextBtn.setDisable(true);
            }
        });

        prevBtn.setOnAction(e -> {
            if (!evalMode) {
                if (steps == null) return;
                if (stepIndex > 0) stepIndex--;
                renderConversion.run();
            } else {
                if (evalSteps == null) return;
                if (evalStepIndex > 0) evalStepIndex--;
                renderEval.run();
            }
        });

        nextBtn.setOnAction(e -> {
            if (!evalMode) {
                if (steps == null) return;

                if (stepIndex < steps.size() - 1) {
                    stepIndex++;
                    renderConversion.run();
                }

                // When conversion ends → start evaluation
                if (stepIndex == steps.size() - 1) {
                    String postfix = steps.get(steps.size() - 1).output;

                    evalSteps = PostfixEvaluatorWithSteps.evaluateWithSteps(postfix);
                    evalStepIndex = 0;
                    evalMode = true;

                    double value = PostfixEvaluator.evaluate(postfix);
                    resultLabel.setText("Evaluation: " + value);

                    renderEval.run();
                }
            } else {
                if (evalSteps == null) return;
                if (evalStepIndex < evalSteps.size() - 1) {
                    evalStepIndex++;
                    renderEval.run();
                }
            }
        });

        HBox controls = new HBox(10, buildBtn, prevBtn, nextBtn);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(10,
                new Label("Infix Expression:"),
                input,
                controls,
                charLabel,
                actionLabel,
                outputLabel,
                evalLabel,
                evalActionLabel,
                resultLabel,
                errorLabel
        );
        left.setPadding(new Insets(12));
        left.setPrefWidth(520);

        HBox root = new HBox(12, left, stackPane, evalStackPane);
        root.setPadding(new Insets(12));

        stage.setTitle("Infix → Postfix + Evaluation (Visualization)");
        stage.setScene(new Scene(root, 1040, 360));
        stage.show();
    }

    private BorderPane buildStackPane(String title, VBox stackBoxes) {
        Label stackTitle = new Label(title);
        stackTitle.setFont(Font.font(16));

        Label topHint = new Label("TOP");
        topHint.setTextFill(Color.GRAY);

        Label bottomHint = new Label("BOTTOM");
        bottomHint.setTextFill(Color.GRAY);

        BorderPane pane = new BorderPane();
        pane.setTop(new VBox(4, stackTitle, topHint));
        BorderPane.setAlignment(stackTitle, Pos.CENTER);
        BorderPane.setAlignment(topHint, Pos.CENTER);

        pane.setCenter(stackBoxes);
        pane.setBottom(bottomHint);
        BorderPane.setAlignment(bottomHint, Pos.CENTER);

        pane.setPrefWidth(200);
        pane.setStyle(
                "-fx-border-color: #444; " +
                        "-fx-border-width: 2; " +
                        "-fx-background-color: #fafafa;"
        );
        return pane;
    }

    private void drawStack(List<Character> stackTopToBottom) {
        stackBoxes.getChildren().clear();

        if (stackTopToBottom == null || stackTopToBottom.isEmpty()) {
            Label empty = new Label("(empty)");
            empty.setTextFill(Color.GRAY);
            stackBoxes.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < stackTopToBottom.size(); i++) {
            char c = stackTopToBottom.get(i);

            Label box = new Label(String.valueOf(c));
            box.setMinWidth(80);
            box.setAlignment(Pos.CENTER);
            box.setFont(Font.font(18));
            box.setPadding(new Insets(6));
            box.setStyle(
                    "-fx-border-color: black;" +
                            "-fx-border-width: 2;" +
                            "-fx-background-color: white;"
            );

            if (i == 0) {
                box.setStyle(
                        "-fx-border-color: #0b5;" +
                                "-fx-border-width: 3;" +
                                "-fx-background-color: #eafff3;"
                );
            }

            stackBoxes.getChildren().add(box);
        }
    }

    private void drawStringStack(VBox box, List<String> stackTopToBottom) {
        box.getChildren().clear();

        if (stackTopToBottom == null || stackTopToBottom.isEmpty()) {
            Label empty = new Label("(empty)");
            empty.setTextFill(Color.GRAY);
            box.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < stackTopToBottom.size(); i++) {
            String s = stackTopToBottom.get(i);

            Label node = new Label(s);
            node.setMinWidth(80);
            node.setAlignment(Pos.CENTER);
            node.setFont(Font.font(18));
            node.setPadding(new Insets(6));

            if (i == 0) {
                node.setStyle(
                        "-fx-border-color: #0b5;" +
                                "-fx-border-width: 3;" +
                                "-fx-background-color: #eafff3;"
                );
            } else {
                node.setStyle(
                        "-fx-border-color: black;" +
                                "-fx-border-width: 2;" +
                                "-fx-background-color: white;"
                );
            }

            box.getChildren().add(node);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}