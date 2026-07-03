package com.dailyautomator.gui;

import com.dailyautomator.core.CoordUtils;
import com.dailyautomator.core.WindowFinder;
import com.dailyautomator.task.*;
import java.awt.Point;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskManagerDialog {
    private final Stage dialog;
    private final TaskRepository repo = new TaskRepository();
    private final List<TaskDefinition> tasks;
    private final ListView<String> taskListView = new ListView<>();
    private final TextField taskNameField = new TextField();
    private final ListView<String> actionListView = new ListView<>();
    private TaskDefinition currentTask = null;
    private Runnable onTasksChanged;
    private volatile boolean coordsTracking = false;
    private Thread coordThread;
    private Label coordLabel;

    public TaskManagerDialog(Stage owner) {
        this.tasks = repo.loadAll();
        dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("任务管理");
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        SplitPane split = new SplitPane();
        split.setDividerPositions(0.35);
        split.getItems().addAll(createTaskListPanel(), createEditorPanel());
        root.setCenter(split);
        dialog.setScene(new Scene(root, 1100, 650));
        refreshTaskList();
        taskListView.getSelectionModel().selectedIndexProperty().addListener((obs, old, idx) -> {
            int i = idx.intValue();
            if (i >= 0 && i < tasks.size()) selectTask(tasks.get(i));
        });
    }

    public void setOnTasksChanged(Runnable r) { this.onTasksChanged = r; }
    public void show() { dialog.show(); }

    private VBox createTaskListPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(5));
        panel.setStyle("-fx-border-color: #ccc; -fx-border-radius: 4; -fx-padding: 10;");
        Label title = new Label("所有任务");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        taskListView.setPrefHeight(500);
        HBox btnRow = new HBox(5); btnRow.setAlignment(Pos.CENTER);
        Button addBtn = new Button("+ 新建"); addBtn.setOnAction(e -> addNewTask());
        Button delBtn = new Button("删除"); delBtn.setOnAction(e -> deleteSelectedTask());
        Button refBtn = new Button("刷新"); refBtn.setOnAction(e -> { tasks.clear(); tasks.addAll(repo.loadAll()); refreshTaskList(); });
        btnRow.getChildren().addAll(addBtn, delBtn, refBtn);
        panel.getChildren().addAll(title, taskListView, btnRow);
        return panel;
    }

    private VBox createEditorPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(5));
        panel.setStyle("-fx-border-color: #ccc; -fx-border-radius: 4; -fx-padding: 10;");
        Label title = new Label("编辑任务");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        HBox nameRow = new HBox(5); nameRow.setAlignment(Pos.CENTER_LEFT);
        nameRow.getChildren().addAll(new Label("名称:"), taskNameField);
        taskNameField.setPrefWidth(200);
        taskNameField.textProperty().addListener((obs, old, name) -> { if (currentTask != null) currentTask.setName(name); });
        Label actionTitle = new Label("步骤:");
        actionListView.setPrefHeight(380);
        HBox actionBtnRow = new HBox(5); actionBtnRow.setAlignment(Pos.CENTER);
        Button addClickBtn = new Button("+ 坐标点击"); addClickBtn.setOnAction(e -> addClickAction());
        Button addWaitBtn = new Button("+ 等待"); addWaitBtn.setOnAction(e -> addWaitAction());
        Button removeBtn = new Button("删除"); removeBtn.setOnAction(e -> removeSelectedAction());
        Button upBtn = new Button("上移"); upBtn.setOnAction(e -> moveAction(-1));
        Button downBtn = new Button("下移"); downBtn.setOnAction(e -> moveAction(1));
        actionBtnRow.getChildren().addAll(addClickBtn, addWaitBtn, removeBtn, upBtn, downBtn);
        HBox saveRow = new HBox(5); saveRow.setAlignment(Pos.CENTER_RIGHT);
        Button saveBtn = new Button("保存到文件");
        saveBtn.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        saveBtn.setOnAction(e -> saveAll());
        saveRow.getChildren().add(saveBtn);
        HBox coordRow = new HBox(5); coordRow.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.control.Button coordBtn = new javafx.scene.control.Button("获取坐标");
        coordBtn.setOnAction(e -> toggleCoords());
        coordLabel = new javafx.scene.control.Label("");
        coordLabel.setStyle("-fx-text-fill: #1565c0; -fx-font-size: 12px;");
        coordRow.getChildren().addAll(coordBtn, coordLabel);
        panel.getChildren().addAll(title, nameRow, actionTitle, actionListView, actionBtnRow, coordRow, saveRow);
        return panel;
    }

    private void addNewTask() {
        TextInputDialog d = new TextInputDialog("NewTask");
        d.setTitle("新建任务"); d.setHeaderText("输入任务名称:");
        Optional<String> result = d.showAndWait();
        result.ifPresent(name -> {
            if (name.isBlank()) name = "未命名";
            TaskDefinition task = new TaskDefinition(name.trim());
            tasks.add(task); refreshTaskList();
            taskListView.getSelectionModel().selectLast(); selectTask(task); notifyChanged();
        });
    }

    private void deleteSelectedTask() {
        int idx = taskListView.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= tasks.size()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete task \"" + tasks.get(idx).getName() + "\"?", ButtonType.YES, ButtonType.NO);
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            tasks.remove(idx); refreshTaskList(); clearEditor(); notifyChanged();
        }
    }

    private void selectTask(TaskDefinition task) {
        currentTask = task; taskNameField.setText(task.getName()); refreshActionList();
    }

    private void addClickAction() {
        if (currentTask == null) return;
        TextInputDialog xd = new TextInputDialog("0");
        xd.setTitle("添加坐标点击"); xd.setHeaderText("输入 X 偏移:");
        Optional<String> xr = xd.showAndWait(); if (xr.isEmpty() || xr.get().isBlank()) return;
        TextInputDialog yd = new TextInputDialog("0");
        yd.setTitle("添加坐标点击"); yd.setHeaderText("输入 Y 偏移:");
        Optional<String> yr = yd.showAndWait(); if (yr.isEmpty() || yr.get().isBlank()) return;
        try {
            currentTask.addAction(new ClickAction(Integer.parseInt(xr.get().trim()), Integer.parseInt(yr.get().trim())));
            refreshActionList(); notifyChanged();
        } catch (NumberFormatException e) { showAlert("请输入整数。"); }
    }

    private void addWaitAction() {
        if (currentTask == null) return;
        ChoiceDialog<String> d = new ChoiceDialog<>("2000", List.of("500", "1000", "2000", "3000", "4000", "5000"));
        d.setTitle("添加等待"); d.setHeaderText("时长（毫秒）:");
        Optional<String> result = d.showAndWait();
        result.ifPresent(ms -> { currentTask.addAction(new WaitAction(Integer.parseInt(ms))); refreshActionList(); notifyChanged(); });
    }

    private void removeSelectedAction() {
        if (currentTask == null) return;
        int idx = actionListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0 && idx < currentTask.actionCount()) {
            currentTask.removeAction(idx); refreshActionList(); notifyChanged();
        }
    }

    private void moveAction(int d) {
        if (currentTask == null) return;
        int idx = actionListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        if (d < 0) currentTask.moveActionUp(idx); else currentTask.moveActionDown(idx);
        refreshActionList(); actionListView.getSelectionModel().select(idx + d); notifyChanged();
    }

        private void toggleCoords() {
        if (coordsTracking) { coordsTracking = false; return; }
        long hwnd = WindowFinder.findByTitleAndClass("BrownDust II", "UnityWndClass");
        if (hwnd == 0) { hwnd = WindowFinder.findByTitle("BrownDust II"); }
        if (hwnd == 0) { showAlert("\u672A\u627E\u5230\u6E38\u620F\u7A97\u53E3"); return; }
        final long targetHwnd = hwnd;
        coordsTracking = true;
        coordThread = new Thread(() -> {
            while (coordsTracking) {
                try {
                    Point off = CoordUtils.getMouseOffset(targetHwnd);
                    javafx.application.Platform.runLater(() ->
                        coordLabel.setText(String.format("(%d, %d)", off.x, off.y)));
                    Thread.sleep(200);
                } catch (InterruptedException e) { break; }
            }
            javafx.application.Platform.runLater(() -> coordLabel.setText(""));
        }, "CoordTracker");
        coordThread.setDaemon(true); coordThread.start();
    }

    private void saveAll() { repo.saveAll(tasks); showAlert("\u5DF2\u4FDD\u5B58 " + tasks.size() + " \u4E2A\u4EFB\u52A1\u3002"); notifyChanged(); }
    private void refreshTaskList() { taskListView.getItems().setAll(tasks.stream().map(TaskDefinition::getName).toList()); }
    private void refreshActionList() {
        if (currentTask == null) { actionListView.getItems().clear(); return; }
        actionListView.getItems().setAll(currentTask.getActions().stream().map(a -> "  " + a.toString()).toList());
    }
    private void clearEditor() { currentTask = null; taskNameField.clear(); actionListView.getItems().clear(); }
    private void notifyChanged() { if (onTasksChanged != null) onTasksChanged.run(); }
    private void showAlert(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
}

