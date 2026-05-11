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
    private static final double EPSILON = 1e-10;

    private int stepIndex = 0;
    private List<InfixToPostfixConverter.Step> steps;

    // UI parts we update
    private Label charLabel;
    private Label actionLabel;
    private Label outputLabel;
    private Label resultLabel;

    // This VBox will contain stack "boxes"
    private VBox stackBoxes;

    @Override
    public void start(Stage stage) {
        TextField input = new TextField();
        input.setPromptText("Example: 3+2*7/(9-10)");

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
        resultLabel = new Label("Evaluation result: ");
        resultLabel.setFont(Font.font(16));

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.DARKRED);

        // Stack drawing area
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

        Runnable render = () -> {
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

        buildBtn.setOnAction(e -> {
            try {
                steps = InfixToPostfixConverter.convertWithSteps(input.getText());
                stepIndex = 0;
                String postfix = steps.get(steps.size() - 1).output;
                double result = InfixToPostfixConverter.evaluatePostfix(postfix);
                resultLabel.setText("Evaluation result: " + formatNumber(result));
                errorLabel.setText("");
                render.run();
                prevBtn.setDisable(false);
                nextBtn.setDisable(false);
            } catch (Exception ex) {
                steps = null;
                stepIndex = 0;
                stackBoxes.getChildren().clear();
                charLabel.setText("Current char: ");
                actionLabel.setText("Action: ");
                outputLabel.setText("Postfix output: ");
                resultLabel.setText("Evaluation result: ");
                errorLabel.setText(ex.getMessage());
                prevBtn.setDisable(true);
                nextBtn.setDisable(true);
            }
        });

        prevBtn.setOnAction(e -> {
            if (steps == null) return;
            if (stepIndex > 0) stepIndex--;
            render.run();
        });

        nextBtn.setOnAction(e -> {
            if (steps == null) return;
            if (stepIndex < steps.size() - 1) stepIndex++;
            render.run();
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
                resultLabel,
                errorLabel
        );
        left.setPadding(new Insets(12));
        left.setPrefWidth(520);

        HBox root = new HBox(12, left, stackPane);
        root.setPadding(new Insets(12));

        stage.setTitle("Infix → Postfix (Stack Visualization)");
        stage.setScene(new Scene(root, 780, 320));
        stage.show();
    }

    private void drawStack(List<Character> stackTopToBottom) {
        stackBoxes.getChildren().clear();

        if (stackTopToBottom == null || stackTopToBottom.isEmpty()) {
            Label empty = new Label("(empty)");
            empty.setTextFill(Color.GRAY);
            stackBoxes.getChildren().add(empty);
            return;
        }

        // Each entry becomes a "box"
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

            // Highlight the top element
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

    public static void main(String[] args) {
        launch(args);
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < EPSILON) {
            return String.format("%.0f", value);
        }
        return String.valueOf(value);
    }
}
