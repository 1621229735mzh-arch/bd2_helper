package com.dailyautomator.gui;

import com.dailyautomator.core.*;
import com.dailyautomator.task.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainApp extends Application {
    private Robot robot;
    private ScreenCapture screenCapture;
    private MouseController mouseController;
    private KeyboardController keyboardController;
    private TaskRunner taskRunner;
    private volatile BufferedImage lastScreenshot;
    private final List<TaskDefinition> allTasks = new ArrayList<>();
    private final List<TaskDefinition> queueTasks = new ArrayList<>();
    private final TaskRepository repo = new TaskRepository();
    private Label statusLabel;
    private Label currentTaskLabel;
    private ProgressBar progressBar;
    private TextArea logArea;
    private Button startButton;
    private Button stopButton;
    private boolean coordsTracking = false;
    private Thread coordThread;
    private Thread workerThread;
    private Label coordLabel;
    private ListView<String> savedTaskListView;
    private ListView<String> taskActionDetailView;
    private ListView<String> queueListView;
    private TaskDefinition currentEditingTask = null;
    private Label currentTaskNameLabel;

    // ── Launch ──────────────────────────────────────────────

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) {
        try { robot = new Robot(); } catch (AWTException e) { showAlert("Error", "Robot init failed"); return; }
        screenCapture = new ScreenCapture(robot);
        mouseController = new MouseController(robot, MouseController.Mode.FOREGROUND);
        keyboardController = new KeyboardController(robot);
        taskRunner = new TaskRunner(mouseController, keyboardController, screenCapture);
        primaryStage.setTitle("\u6BCF\u65E5\u81EA\u52A8\u5316 v1.0.0");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(8));

        root.setTop(createHeaderBar());

        SplitPane split = new SplitPane();
        split.setDividerPositions(0.28, 0.72);
        split.getItems().addAll(createTaskListPanel(), createActionEditorPanel(), createQueuePanel());

        VBox bottomSection = new VBox(5);
        bottomSection.setPadding(new Insets(6, 0, 0, 0));
        bottomSection.getChildren().addAll(createControlBar(), createStatusRow());

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        logArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px;");
        logArea.textProperty().addListener((o, old, t) -> logArea.setScrollTop(Double.MAX_VALUE));
        bottomSection.getChildren().add(logArea);

        root.setCenter(split);
        root.setBottom(bottomSection);

        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.setResizable(false);
        primaryStage.show();
        reloadTasks();
        queueTasks.addAll(repo.loadQueue());
        rebuildQueueList();
        log("\u6BCF\u65E5\u81EA\u52A8\u5316 v1.0.0 \u5DF2\u542F\u52A8\u3002\u5C31\u7EEA\u3002");
    }

    // ── Header Bar ──────────────────────────────────────────

    private HBox createHeaderBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(4, 0, 8, 0));
        bar.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("\u6BCF\u65E5\u81EA\u52A8\u5316 v1.0.0");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        coordLabel = new Label("");
        coordLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #1565c0; -fx-padding: 0 10 0 0;");
        statusLabel = new Label("\u5C31\u7EEA");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        bar.getChildren().addAll(title, spacer, coordLabel, statusLabel);
        return bar;
    }

    // ── Saved Tasks Panel (Left) ────────────────────────────

    private VBox createTaskListPanel() {
        VBox panel = new VBox(6);
        panel.setPadding(new Insets(8));
        panel.setStyle("-fx-border-color: #bbb; -fx-border-radius: 4;");

        Label title = new Label("\u4EFB\u52A1\u5217\u8868");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        savedTaskListView = new ListView<>();
        VBox.setVgrow(savedTaskListView, Priority.ALWAYS);
        savedTaskListView.getSelectionModel().selectedIndexProperty().addListener((obs, old, idx) -> {
            int i = idx.intValue();
            if (i >= 0 && i < allTasks.size()) {
                selectTask(allTasks.get(i));
            } else {
                clearEditor();
            }
        });

        HBox btnRow = new HBox(4);
        btnRow.setAlignment(Pos.CENTER);
        Button newBtn = new Button("+\u65B0\u5EFA");
        newBtn.setOnAction(e -> addNewTask());
        Button renameBtn = new Button("\u91CD\u547D\u540D");
        renameBtn.setOnAction(e -> renameSelectedTask());
        Button delBtn = new Button("\u5220\u9664");
        delBtn.setOnAction(e -> deleteSelectedTask());
        Button reloadBtn = new Button("\u5237\u65B0");
        reloadBtn.setOnAction(e -> reloadTasks());
        btnRow.getChildren().addAll(newBtn, renameBtn, delBtn, reloadBtn);

        panel.getChildren().addAll(title, savedTaskListView, btnRow);
        return panel;
    }

    // ── Action Editor Panel (Center) ────────────────────────

    private VBox createActionEditorPanel() {
        VBox panel = new VBox(6);
        panel.setPadding(new Insets(8));
        panel.setStyle("-fx-border-color: #bbb; -fx-border-radius: 4;");

        currentTaskNameLabel = new Label("(\u672A\u9009\u62E9\u4EFB\u52A1)");
        currentTaskNameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1565c0;");

        Label stepsTitle = new Label("\u6B65\u9AA4\uFF1A");
        stepsTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        taskActionDetailView = new ListView<>();
        VBox.setVgrow(taskActionDetailView, Priority.ALWAYS);
        taskActionDetailView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && currentEditingTask != null) editSelectedAction();
        });

        HBox addRow = new HBox(4);
        addRow.setAlignment(Pos.CENTER);
        Button addClickBtn = new Button("+\u70B9\u51FB");
        addClickBtn.setOnAction(e -> addClickAction());
        Button addWaitBtn = new Button("+\u7B49\u5F85");
        addWaitBtn.setOnAction(e -> addWaitAction());
        Button addKeyBtn = new Button("+\u6309\u952E");
        addKeyBtn.setOnAction(e -> addKeyPressAction());
        addRow.getChildren().addAll(addClickBtn, addWaitBtn, addKeyBtn);

        HBox editRow = new HBox(4);
        editRow.setAlignment(Pos.CENTER);
        Button editBtn = new Button("\u7F16\u8F91");
        editBtn.setOnAction(e -> editSelectedAction());
        Button removeBtn = new Button("\u79FB\u9664");
        removeBtn.setOnAction(e -> removeSelectedAction());
        Button upBtn = new Button("\u4E0A\u79FB");
        upBtn.setOnAction(e -> moveSelectedAction(-1));
        Button dnBtn = new Button("\u4E0B\u79FB");
        dnBtn.setOnAction(e -> moveSelectedAction(1));
        editRow.getChildren().addAll(editBtn, removeBtn, upBtn, dnBtn);

        panel.getChildren().addAll(currentTaskNameLabel, stepsTitle, taskActionDetailView, addRow, editRow);
        return panel;
    }

    // ── Launch Queue Panel (Right) ──────────────────────────

    private VBox createQueuePanel() {
        VBox panel = new VBox(6);
        panel.setPadding(new Insets(8));
        panel.setStyle("-fx-border-color: #bbb; -fx-border-radius: 4;");

        Label title = new Label("\u542F\u52A8\u961F\u5217");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        queueListView = new ListView<>();
        VBox.setVgrow(queueListView, Priority.ALWAYS);

        HBox btnRow = new HBox(4);
        btnRow.setAlignment(Pos.CENTER);
        Button addBtn = new Button(">>\u6DFB\u52A0");
        addBtn.setStyle("-fx-font-weight: bold;");
        addBtn.setOnAction(e -> addToQueue());
        Button upBtn = new Button("\u4E0A\u79FB");
        upBtn.setOnAction(e -> moveQueueItem(-1));
        Button dnBtn = new Button("\u4E0B\u79FB");
        dnBtn.setOnAction(e -> moveQueueItem(1));
        Button removeBtn = new Button("\u79FB\u9664");
        removeBtn.setOnAction(e -> removeFromQueue());
        Button clearBtn = new Button("\u6E05\u7A7A");
        clearBtn.setOnAction(e -> clearQueue());
        btnRow.getChildren().addAll(addBtn, upBtn, dnBtn, removeBtn, clearBtn);

        panel.getChildren().addAll(title, queueListView, btnRow);
        return panel;
    }

    // ── Control Bar ─────────────────────────────────────────

    private FlowPane createControlBar() {
        FlowPane bar = new FlowPane(8, 4);
        bar.setAlignment(Pos.CENTER);

        startButton = new Button("\u5F00\u59CB");
        startButton.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-base: #4CAF50;");
        startButton.setOnAction(e -> startAutomation());

        stopButton = new Button("\u505C\u6B62");
        stopButton.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-base: #f44336;");
        stopButton.setDisable(true);
        stopButton.setOnAction(e -> stopAutomation());

        Button coordBtn = new Button("\u5750\u6807");
        coordBtn.setOnAction(e -> toggleCoords());

        Button testBtn = new Button("\u6D4B\u8BD5\u70B9\u51FB");
        testBtn.setOnAction(e -> testClick());

        Button ssBtn = new Button("\u622A\u56FE\u67E5\u770B");
        ssBtn.setOnAction(e -> showScreenshot());

        Button saveBtn = new Button("\u4FDD\u5B58\u622A\u56FE");
        saveBtn.setOnAction(e -> saveWindowScreenshot());

        Button guiweiBtn = new Button("\u5F52\u4F4D");
        guiweiBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-base: #42a5f5;");
        guiweiBtn.setOnAction(e -> snapWindowToTopLeft());

        Button clearLogBtn = new Button("\u6E05\u9664\u65E5\u5FD7");
        clearLogBtn.setOnAction(e -> logArea.clear());

        bar.getChildren().addAll(startButton, stopButton, coordBtn, testBtn, ssBtn, saveBtn, guiweiBtn, clearLogBtn);
        return bar;
    }

    // ── Status Row ──────────────────────────────────────────

    private HBox createStatusRow() {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));
        Label curLabel = new Label("\u5F53\u524D\u4EFB\u52A1\uFF1A");
        currentTaskLabel = new Label("(\u65E0)");
        currentTaskLabel.setStyle("-fx-text-fill: #1565c0; -fx-font-weight: bold;");
        Label progLabel = new Label("\u8FDB\u5EA6\uFF1A");
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(180);
        row.getChildren().addAll(curLabel, currentTaskLabel, progLabel, progressBar);
        return row;
    }

    // ── Task Operations ─────────────────────────────────────

    private void reloadTasks() {
        currentEditingTask = null;
        allTasks.clear();
        allTasks.addAll(repo.loadAll());
        savedTaskListView.getItems().setAll(allTasks.stream().map(t -> t.getName() + " (" + t.actionCount() + " \u6B65)").toList());
        clearEditor();
    }

    private void selectTask(TaskDefinition task) {
        currentEditingTask = task;
        currentTaskNameLabel.setText(task.getName());
        taskActionDetailView.getItems().setAll(task.getActions().stream().map(a -> "  " + a.toString()).toList());
    }

    private void clearEditor() {
        currentEditingTask = null;
        currentTaskNameLabel.setText("(\u672A\u9009\u62E9\u4EFB\u52A1)");
        taskActionDetailView.getItems().clear();
    }

    private void refreshActionList() {
        if (currentEditingTask == null) { taskActionDetailView.getItems().clear(); return; }
        taskActionDetailView.getItems().setAll(currentEditingTask.getActions().stream().map(a -> "  " + a.toString()).toList());
        int idx = allTasks.indexOf(currentEditingTask);
        if (idx >= 0) {
            savedTaskListView.getItems().set(idx, currentEditingTask.getName() + " (" + currentEditingTask.actionCount() + " \u6B65)");
        }
    }

    private void addNewTask() {
        TextInputDialog d = new TextInputDialog("NewTask");
        d.setTitle("\u65B0\u5EFA\u4EFB\u52A1");
        d.setHeaderText("\u8BF7\u8F93\u5165\u4EFB\u52A1\u540D\u79F0\uFF1A");
        Optional<String> result = d.showAndWait();
        result.ifPresent(name -> {
            if (name.isBlank()) name = "\u672A\u547D\u540D";
            TaskDefinition task = new TaskDefinition(name.trim());
            allTasks.add(task);
            savedTaskListView.getItems().add(task.getName() + " (0 \u6B65)");
            savedTaskListView.getSelectionModel().selectLast();
            selectTask(task);
            repo.saveAll(allTasks, queueTasks);
            log("\u5DF2\u521B\u5EFA\u4EFB\u52A1\uFF1A" + name.trim());
        });
    }

    private void renameSelectedTask() {
        int idx = savedTaskListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= allTasks.size()) return;
        TaskDefinition task = allTasks.get(idx);
        TextInputDialog d = new TextInputDialog(task.getName());
        d.setTitle("\u91CD\u547D\u540D\u4EFB\u52A1");
        d.setHeaderText("\u65B0\u540D\u79F0\uFF1A");
        Optional<String> result = d.showAndWait();
        result.ifPresent(name -> {
            if (name.isBlank()) return;
            task.setName(name.trim());
            savedTaskListView.getItems().set(idx, task.getName() + " (" + task.actionCount() + " \u6B65)");
            if (currentEditingTask == task) currentTaskNameLabel.setText(task.getName());
            repo.saveAll(allTasks, queueTasks);
            log("\u5DF2\u91CD\u547D\u540D\u4EFB\u52A1\uFF1A" + name.trim());
        });
    }

    private void deleteSelectedTask() {
        int idx = savedTaskListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= allTasks.size()) return;
        TaskDefinition task = allTasks.get(idx);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "\u5220\u9664 \"" + task.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            queueTasks.removeIf(t -> t.getName().equals(task.getName()));
            allTasks.remove(idx);
            savedTaskListView.getItems().remove(idx);
            rebuildQueueList();
            if (currentEditingTask == task) clearEditor();
            repo.saveAll(allTasks, queueTasks);
            log("\u5DF2\u5220\u9664\u4EFB\u52A1\uFF1A" + task.getName());
        }
    }

    // ── Action Operations ───────────────────────────────────

    private void addClickAction() {
        if (currentEditingTask == null) return;
        TextInputDialog xd = new TextInputDialog("0");
        xd.setTitle("\u6DFB\u52A0\u70B9\u51FB");
        xd.setHeaderText("\u8BF7\u8F93\u5165 X \u5750\u6807(\u7A97\u53E3\u76F8\u5BF9)\uFF1A");
        Optional<String> xr = xd.showAndWait();
        if (xr.isEmpty() || xr.get().isBlank()) return;
        TextInputDialog yd = new TextInputDialog("0");
        yd.setTitle("\u6DFB\u52A0\u70B9\u51FB");
        yd.setHeaderText("\u8BF7\u8F93\u5165 Y \u5750\u6807(\u7A97\u53E3\u76F8\u5BF9)\uFF1A");
        Optional<String> yr = yd.showAndWait();
        if (yr.isEmpty() || yr.get().isBlank()) return;
        try {
            currentEditingTask.addAction(new ClickAction(Integer.parseInt(xr.get().trim()), Integer.parseInt(yr.get().trim())));
            refreshActionList();
            repo.saveAll(allTasks, queueTasks);
            log("\u6DFB\u52A0\u70B9\u51FB(" + xr.get().trim() + "," + yr.get().trim() + ")");
        } catch (NumberFormatException e) {
            showAlert("\u9519\u8BEF", "\u8BF7\u8F93\u5165\u6709\u6548\u7684\u6574\u6570\u3002");
        }
    }

    private void addWaitAction() {
        if (currentEditingTask == null) return;
        ChoiceDialog<String> d = new ChoiceDialog<>("2000", List.of("500", "1000", "2000", "3000", "5000", "10000"));
        d.setTitle("\u6DFB\u52A0\u7B49\u5F85");
        d.setHeaderText("\u65F6\u957F(\u6BEB\u79D2)\uFF1A");
        Optional<String> result = d.showAndWait();
        result.ifPresent(ms -> {
            currentEditingTask.addAction(new WaitAction(Integer.parseInt(ms)));
            refreshActionList();
            repo.saveAll(allTasks, queueTasks);
            log("\u6DFB\u52A0\u7B49\u5F85(" + ms + "ms)");
        });
    }

    private void addKeyPressAction() {
        if (currentEditingTask == null) return;
        ChoiceDialog<String> d = new ChoiceDialog<>("H", List.of("H", "F", "ESC"));
        d.setTitle("\u6DFB\u52A0\u6309\u952E");
        d.setHeaderText("\u9009\u62E9\u6309\u952E\uFF1A");
        Optional<String> result = d.showAndWait();
        result.ifPresent(key -> {
            currentEditingTask.addAction(new KeyPressAction(key));
            refreshActionList();
            repo.saveAll(allTasks, queueTasks);
            log("\u6DFB\u52A0\u6309\u952E(" + key + ")");
        });
    }

    private void editSelectedAction() {
        if (currentEditingTask == null) return;
        int idx = taskActionDetailView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= currentEditingTask.actionCount()) return;
        Action action = currentEditingTask.getActions().get(idx);
        if (action instanceof ClickAction ca) {
            TextInputDialog xd = new TextInputDialog(String.valueOf(ca.getX()));
            xd.setTitle("\u7F16\u8F91\u70B9\u51FB");
            xd.setHeaderText("\u4FEE\u6539 X \u504F\u79FB\uFF1A");
            Optional<String> xr = xd.showAndWait();
            if (xr.isEmpty() || xr.get().isBlank()) return;
            TextInputDialog yd = new TextInputDialog(String.valueOf(ca.getY()));
            yd.setTitle("\u7F16\u8F91\u70B9\u51FB");
            yd.setHeaderText("\u4FEE\u6539 Y \u504F\u79FB\uFF1A");
            Optional<String> yr = yd.showAndWait();
            if (yr.isEmpty() || yr.get().isBlank()) return;
            try {
                currentEditingTask.setAction(idx, new ClickAction(Integer.parseInt(xr.get().trim()), Integer.parseInt(yr.get().trim())));
                refreshActionList();
                repo.saveAll(allTasks, queueTasks);
                log("\u5DF2\u7F16\u8F91\u7B2C" + (idx + 1) + "\u6B65\uFF1AClick(" + xr.get().trim() + "," + yr.get().trim() + ")");
            } catch (NumberFormatException e) {
                showAlert("\u9519\u8BEF", "\u8BF7\u8F93\u5165\u6709\u6548\u7684\u6574\u6570\u3002");
            }
        } else if (action instanceof WaitAction wa) {
            TextInputDialog d = new TextInputDialog(String.valueOf(wa.getDurationMs()));
            d.setTitle("\u7F16\u8F91\u7B49\u5F85");
            d.setHeaderText("\u4FEE\u6539\u65F6\u957F(\u6BEB\u79D2)\uFF1A");
            Optional<String> result = d.showAndWait();
            if (result.isEmpty() || result.get().isBlank()) return;
            try {
                currentEditingTask.setAction(idx, new WaitAction(Integer.parseInt(result.get().trim())));
                refreshActionList();
                repo.saveAll(allTasks, queueTasks);
                log("\u5DF2\u7F16\u8F91\u7B2C" + (idx + 1) + "\u6B65\uFF1AWait(" + result.get().trim() + "ms)");
            } catch (NumberFormatException e) {
                showAlert("\u9519\u8BEF", "\u8BF7\u8F93\u5165\u6709\u6548\u7684\u6574\u6570\u3002");
            }
        } else if (action instanceof KeyPressAction kpa) {
            ChoiceDialog<String> d = new ChoiceDialog<>(kpa.getKey().toUpperCase(), List.of("H", "F", "ESC"));
            d.setTitle("\u7F16\u8F91\u6309\u952E");
            d.setHeaderText("\u9009\u62E9\u6309\u952E\uFF1A");
            Optional<String> result = d.showAndWait();
            if (result.isEmpty() || result.get().isBlank()) return;
            currentEditingTask.setAction(idx, new KeyPressAction(result.get().trim()));
            refreshActionList();
            repo.saveAll(allTasks, queueTasks);
            log("\u5DF2\u7F16\u8F91\u7B2C" + (idx + 1) + "\u6B65\uFF1AKeyPress(" + result.get().trim() + ")");
        }
    }

    private void removeSelectedAction() {
        if (currentEditingTask == null) return;
        int idx = taskActionDetailView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= currentEditingTask.actionCount()) return;
        currentEditingTask.removeAction(idx);
        refreshActionList();
        repo.saveAll(allTasks, queueTasks);
        log("\u5DF2\u5220\u9664\u7B2C" + (idx + 1) + "\u6B65");
    }

    private void moveSelectedAction(int d) {
        if (currentEditingTask == null) return;
        int idx = taskActionDetailView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        if (d < 0) currentEditingTask.moveActionUp(idx); else currentEditingTask.moveActionDown(idx);
        refreshActionList();
        int newIdx = idx + d;
        if (newIdx >= 0) taskActionDetailView.getSelectionModel().select(newIdx);
        repo.saveAll(allTasks, queueTasks);
    }

    // ── Queue Operations ────────────────────────────────────

    private void addToQueue() {
        int idx = savedTaskListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= allTasks.size()) return;
        TaskDefinition task = allTasks.get(idx);
        TaskDefinition copy = new TaskDefinition(task.getName(), task.getActions());
        queueTasks.add(copy);
        rebuildQueueList();
        log("\u5DF2\u6DFB\u52A0\u5230\u961F\u5217\uFF1A" + task.getName());
    }

    private void rebuildQueueList() {
        queueListView.getItems().setAll(queueTasks.stream().map(t -> t.getName() + " (" + t.actionCount() + " \u6B65)").toList());
    }

    private void moveQueueItem(int d) {
        int idx = queueListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        int t = idx + d;
        if (t < 0 || t >= queueTasks.size()) return;
        TaskDefinition tmp = queueTasks.get(idx);
        queueTasks.set(idx, queueTasks.get(t));
        queueTasks.set(t, tmp);
        rebuildQueueList();
        queueListView.getSelectionModel().select(t);
        repo.saveAll(allTasks, queueTasks);
    }

    private void removeFromQueue() {
        int idx = queueListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= queueTasks.size()) return;
        queueTasks.remove(idx);
        rebuildQueueList();
        repo.saveAll(allTasks, queueTasks);
    }

    private void clearQueue() {
        if (queueTasks.isEmpty()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "\u6E05\u7A7A\u6574\u4E2A\u542F\u52A8\u961F\u5217\uFF1F", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            queueTasks.clear();
            rebuildQueueList();
            log("\u542F\u52A8\u961F\u5217\u5DF2\u6E05\u7A7A\u3002");
        }
    }

    // ── Automation ──────────────────────────────────────────

    private void startAutomation() {
        if (taskRunner.isRunning()) { log("\u6B63\u5728\u8FD0\u884C\u4E2D\u3002"); return; }
        if (queueTasks.isEmpty()) { log("\u542F\u52A8\u961F\u5217\u4E3A\u7A7A\uFF0C\u8BF7\u5148\u6DFB\u52A0\u4EFB\u52A1\u3002"); return; }
        startButton.setDisable(true);
        stopButton.setDisable(false);
        setStatus("\u8FD0\u884C\u4E2D", "#e65100");
        log("\u6B63\u5728\u542F\u52A8 " + queueTasks.size() + " \u4E2A\u4EFB\u52A1...");
        List<TaskDefinition> runTasks = new ArrayList<>(queueTasks);
        workerThread = new Thread(() -> {
            try {
                taskRunner.setLogCallback(m -> Platform.runLater(() -> log(m)));
                taskRunner.setProgressCallback(p -> Platform.runLater(() -> progressBar.setProgress(p)));
                taskRunner.setTaskNameCallback(nm -> Platform.runLater(() -> currentTaskLabel.setText(nm)));
                taskRunner.runAll(runTasks, buildEnabledFlags());
            } catch (Exception e) {
                Platform.runLater(() -> log("\u9519\u8BEF\uFF1A" + e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    startButton.setDisable(false);
                    stopButton.setDisable(true);
                    currentTaskLabel.setText("(\u65E0)");
                    setStatus("\u5C31\u7EEA", "#2e7d32");
                    log("\u81EA\u52A8\u5316\u8FD0\u884C\u5B8C\u6210\u3002");
                });
            }
        }, "Worker");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    private boolean[] buildEnabledFlags() {
        boolean[] f = new boolean[queueTasks.size()];
        for (int i = 0; i < queueTasks.size(); i++) f[i] = true;
        return f;
    }

    private void stopAutomation() {
        taskRunner.stop();
        if (workerThread != null) workerThread.interrupt();
        stopButton.setDisable(true);
        setStatus("\u6B63\u5728\u505C\u6B62...", "#e65100");
        log("\u5DF2\u8BF7\u6C42\u505C\u6B62\u3002");
    }

    // ── Coords & Test Click ─────────────────────────────────

    private void toggleCoords() {
        if (coordsTracking) { coordsTracking = false; return; }
        long hwnd = findGameWindow();
        if (hwnd == 0) { log("\u5750\u6807\uFF1A\u672A\u627E\u5230\u6E38\u620F\u7A97\u53E3\u3002"); return; }
        coordsTracking = true;
        log("\u5750\u6807\uFF1A\u5F00\u59CB\u8FFD\u8E2A\u3002");
        coordThread = new Thread(() -> {
            while (coordsTracking) {
                try {
                    Point off = CoordUtils.getMouseOffset(hwnd);
                    Platform.runLater(() -> coordLabel.setText(String.format("(%d,%d)", off.x, off.y)));
                    Thread.sleep(200);
                } catch (InterruptedException e) { break; }
            }
            Platform.runLater(() -> coordLabel.setText(""));
        }, "CoordTracker");
        coordThread.setDaemon(true);
        coordThread.start();
    }

    private void testClick() {
        long hwnd = findGameWindow();
        if (hwnd == 0) { log("\u6D4B\u8BD5\uFF1A\u672A\u627E\u5230\u6E38\u620F\u7A97\u53E3\u3002"); return; }
        mouseController.setMode(MouseController.Mode.FOREGROUND);
        Point screen = CoordUtils.offsetToScreen(hwnd, 273, 223);
        log("\u6D4B\u8BD5\u70B9\u51FB\u5C4F\u5E55\u5750\u6807(" + screen.x + "," + screen.y + ")");
        mouseController.leftClick(screen.x, screen.y);
        log("\u6D4B\u8BD5\u5B8C\u6210\u3002");
    }

    private long findGameWindow() {
        long hwnd = WindowFinder.findByTitleAndClass("BrownDust II", "UnityWndClass");
        if (hwnd != 0) return hwnd;
        hwnd = WindowFinder.findByTitle("BrownDust II");
        if (hwnd != 0) return hwnd;
        return WindowFinder.getForegroundHwnd();
    }

    // ── Snap Window to Top-Left ─────────────────────────────

    private void snapWindowToTopLeft() {
        long hwnd = findGameWindow();
        if (hwnd == 0) { log("\u5F52\u4F4D\u5931\u8D25\uFF1A\u672A\u627E\u5230\u6E38\u620F\u7A97\u53E3"); return; }
        Rectangle rect = WindowFinder.getWindowRect(hwnd);
        WindowFinder.moveWindow(hwnd, 0, 0);
        log(String.format("\u5F52\u4F4D\u5B8C\u6210 (0,0) -> (%d,%d) [%dx%d]", 0, 0, rect.width, rect.height));
    }

    // ── Screenshots ─────────────────────────────────────────

    private void saveWindowScreenshot() {
        long hwnd = findGameWindow();
        if (hwnd == 0) { log("\u672A\u627E\u5230\u6E38\u620F\u7A97\u53E3\u3002"); return; }
        try {
            BufferedImage img = screenCapture.captureWindowByBitBlt(hwnd);
            if (img == null) img = screenCapture.captureWindow(WindowFinder.getWindowRect(hwnd));
            lastScreenshot = img;
            log("\u5DF2\u4FDD\u5B58 " + screenCapture.saveToFile(img));
        } catch (Exception e) { log("\u64CD\u4F5C\u5931\u8D25\uFF1A" + e.getMessage()); }
    }

    private void showScreenshot() {
        if (lastScreenshot == null) { log("\u6CA1\u6709\u622A\u56FE\u3002"); return; }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(lastScreenshot, "png", baos);
            ImageView iv = new ImageView(new Image(new ByteArrayInputStream(baos.toByteArray())));
            iv.setPreserveRatio(true);
            iv.setFitWidth(900);
            iv.setFitHeight(700);
            Stage s = new Stage();
            s.setTitle("\u622A\u56FE\u67E5\u770B");
            s.setScene(new Scene(new ScrollPane(iv)));
            s.show();
        } catch (Exception e) { log("\u64CD\u4F5C\u5931\u8D25\uFF1A" + e.getMessage()); }
    }

        // ── Helpers ─────────────────────────────────────────────

    private void setStatus(String text, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
    }

    private void log(String msg) {
        Platform.runLater(() -> logArea.appendText(msg + "\n"));
    }

    private void showAlert(String t, String m) {
        Platform.runLater(() -> { Alert a = new Alert(Alert.AlertType.ERROR, m); a.setTitle(t); a.showAndWait(); });
    }
}