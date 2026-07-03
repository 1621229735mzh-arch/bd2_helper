package com.dailyautomator.task;

import com.dailyautomator.core.MouseController;
import com.dailyautomator.core.KeyboardController;
import com.dailyautomator.core.ScreenCapture;
import java.util.function.Consumer;

public class ActionContext {
    public final long hwnd;
    public final int anchorX;
    public final int anchorY;
    public final MouseController mouse;
    public final KeyboardController keyboard;
    public final ScreenCapture screen;
    public final Consumer<String> log;
    public final Consumer<Double> progress;

    public ActionContext(long hwnd, int anchorX, int anchorY, MouseController mouse, KeyboardController keyboard, ScreenCapture screen,
                         Consumer<String> log, Consumer<Double> progress) {
        this.hwnd = hwnd;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.mouse = mouse;
        this.keyboard = keyboard;
        this.screen = screen;
        this.log = log;
        this.progress = progress;
    }
}
