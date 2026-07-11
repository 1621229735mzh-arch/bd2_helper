package com.dailyautomator.task;

import com.dailyautomator.core.MouseController;
import com.dailyautomator.core.KeyboardController;
import com.dailyautomator.core.ScreenCapture;
import com.dailyautomator.core.WindowFinder;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import java.awt.Point;
import java.util.List;
import java.util.function.Consumer;
public class TaskRunner {
    private static final String GAME_TITLE = "BrownDust II";
    private static final String GAME_CLASS = "UnityWndClass";
   private volatile boolean running = false;
  private volatile boolean paused = false;
  private final KeyboardController keyboardController;
    private final MouseController mouseController;
    private final ScreenCapture screenCapture;
    private Consumer<String> logCallback;
    private Consumer<String> taskNameCallback;
   private Consumer<Double> progressCallback;
    private Runnable onAutoPaused;

    

    public TaskRunner(MouseController mouseController, KeyboardController keyboardController, ScreenCapture screenCapture) {
        this.keyboardController = keyboardController;
        this.mouseController = mouseController;
        this.screenCapture = screenCapture;
    }

    public TaskRunner setLogCallback(Consumer<String> c) { this.logCallback = c; return this; }
    public TaskRunner setTaskNameCallback(Consumer<String> c) { this.taskNameCallback = c; return this; }
   public TaskRunner setProgressCallback(Consumer<Double> c) { this.progressCallback = c; return this; }
    public TaskRunner setOnAutoPaused(Runnable r) { this.onAutoPaused = r; return this; }
   public void stop() { running = false; }
    public void pause() { paused = true; }
    public void resume() { paused = false; }
   public boolean isRunning() { return running; }
  public boolean isPaused() { return paused; }

  public void runAll(List<TaskDefinition> tasks, boolean[] enabled) {
       running = true;
        paused = false;
       long hwnd = findWindow();
        if (hwnd == 0) {
            log("Game window \"" + GAME_TITLE + "\" not found.");
            running = false;
            return;
        }

        // Determine the window's current client-area screen origin to use as anchor,
        // so that all coordinates in task actions are treated as window-relative.
        Point windowOrigin = WindowFinder.getClientScreenOrigin(hwnd);
        int anchorX = windowOrigin.x;
        int anchorY = windowOrigin.y;

        // Bring the game window to the foreground so keyboard events land correctly
        User32.INSTANCE.ShowWindow(new WinDef.HWND(new Pointer(hwnd)), 9);
        User32.INSTANCE.SetForegroundWindow(new WinDef.HWND(new Pointer(hwnd)));

        mouseController.setMode(MouseController.Mode.FOREGROUND);
        log("=== Starting automation (" + countEnabled(enabled) + " tasks) ===");
        log("Window: " + GAME_TITLE + " (HWND=" + hwnd + ")");
        log("Anchor: (" + anchorX + "," + anchorY + ")");
        log("Starting in 5 seconds...");
        try { Thread.sleep(2000); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
            return;
        }

        int totalTasks = 0;
        for (boolean e : enabled) if (e) totalTasks++;
        if (totalTasks == 0) { log("No tasks enabled."); running = false; return; }

        int taskIndex = 0;
        for (int ti = 0; ti < tasks.size() && running; ti++) {
            if (!enabled[ti]) continue;
            TaskDefinition task = tasks.get(ti);
            log("--- Task: " + task.getName() + " ---");
            if (taskNameCallback != null) taskNameCallback.accept(task.getName());
           if (task.actionCount() == 0) { log("Empty task, skipped."); taskIndex++; continue; }

            // breakpoint: pause before executing this task
           if (task.isBreakpoint() && running) {
               paused = true;
                if (onAutoPaused != null) onAutoPaused.run();
               log("Breakpoint: Task \"" + task.getName() + "\". Paused before execution. Click continue to proceed.");
            }

           int curTaskIdx = taskIndex;
           int curTotal = totalTasks;
            ActionContext ctx = new ActionContext(hwnd, anchorX, anchorY, mouseController, keyboardController, screenCapture,
                msg -> log("[TASK] " + msg),
                pct -> {
                    if (progressCallback != null)
                        progressCallback.accept((double)curTaskIdx / curTotal + pct / curTotal);
                });

           for (int ai = 0; ai < task.actionCount() && running; ai++) {
                while (paused && running) {
                    try { Thread.sleep(200); } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        running = false;
                        break;
                    }
                }
                if (!running) break;
               try {
                   task.getActions().get(ai).execute(ctx);
                } catch (InterruptedException e) {
                    log("Interrupted."); running = false; break;
               } catch (Exception e) {
                   log("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
               }
          }
          taskIndex++;
        }
        if (progressCallback != null) progressCallback.accept(1.0);
        log(running ? "All tasks completed." : "Stopped by user.");
        running = false;
    }

    private long findWindow() {
        long hwnd = WindowFinder.findByTitleAndClass(GAME_TITLE, GAME_CLASS);
        if (hwnd != 0) return hwnd;
        hwnd = WindowFinder.findByTitle(GAME_TITLE);
        if (hwnd != 0) return hwnd;
        return WindowFinder.getForegroundHwnd();
    }

    private int countEnabled(boolean[] arr) {
        int c = 0; for (boolean b : arr) if (b) c++; return c;
    }

    private void log(String msg) { if (logCallback != null) logCallback.accept(msg); }
}
