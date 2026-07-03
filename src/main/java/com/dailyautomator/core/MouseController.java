package com.dailyautomator.core;

import java.awt.*;
import java.awt.event.InputEvent;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

/**
 * Mouse controller that supports foreground and background modes.
 * <p>
 * <b>FOREGROUND</b> &mdash; uses java.awt.Robot to simulate physical mouse
 * movements and clicks. The cursor actually moves on screen and may
 * interfere with the user's foreground activity.
 * <br>
 * <b>BACKGROUND</b> &mdash; sends mouse events directly to a target window
 * via Win32 {@code SendMessage} (WM_MOUSEMOVE / WM_LBUTTONDOWN / etc.)
 * without taking focus or moving the physical cursor.
 */
public class MouseController {

    /** Operational mode. */
    public enum Mode {
        FOREGROUND,
        BACKGROUND
    }

    private final Robot robot;
    private Mode mode = Mode.FOREGROUND;
    private long targetHwnd;

    public MouseController(Robot robot) {
        this.robot = robot;
    }

    public MouseController(Robot robot, Mode mode) {
        this(robot);
        this.mode = mode;
    }

    /** Set the current operational mode. */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    /** Get the current operational mode. */
    public Mode getMode() {
        return mode;
    }


    /**
     * Set the target window handle for BACKGROUND mode.
     * @param hwnd native window handle (HWND)
     */
    public void setTargetHwnd(long hwnd) {
        this.targetHwnd = hwnd;
    }

    /** Get the target window handle. */
    public long getTargetHwnd() {
        return targetHwnd;
    }

    /** Move the mouse to absolute screen coordinates (x, y). */
    public void move(int x, int y) {
        if (mode == Mode.BACKGROUND) {
            backgroundMouseMove(x, y);
        } else {
            robot.mouseMove(x, y);
        }
    }

    /** Move to (x, y) then click the specified button. */
    public void click(int x, int y, int button) {
        if (mode == Mode.BACKGROUND) {
            backgroundClick(x, y, button);
            return;
        }
        foregroundClick(x, y, button);
    }

    /** Left-click at the target coordinates. */
    public void leftClick(int x, int y) {
        click(x, y, 1);
    }

    /** Right-click at the target coordinates. */
    public void rightClick(int x, int y) {
        click(x, y, 3);
    }

    /** Double-left-click at the target coordinates. */
    public void doubleClick(int x, int y) {
        if (mode == Mode.BACKGROUND) {
            backgroundClick(x, y, 1);
            backgroundClick(x, y, 1);
            return;
        }
        int mask = InputEvent.BUTTON1_DOWN_MASK;
        robot.mouseMove(x, y);
        robot.mousePress(mask);
        robot.delay(50);
        robot.mouseRelease(mask);
        robot.delay(30);
        robot.mousePress(mask);
        robot.delay(50);
        robot.mouseRelease(mask);
    }

    /** Drag from (x1, y1) to (x2, y2) with left button held. */
    public void drag(int x1, int y1, int x2, int y2) {
        if (mode == Mode.BACKGROUND) {
            backgroundDrag(x1, y1, x2, y2);
            return;
        }
        foregroundDrag(x1, y1, x2, y2);
    }

    /** Scroll the mouse wheel by the given amount (positive = down, negative = up). */
    public void scroll(int amount) {
        if (mode == Mode.BACKGROUND) {
            backgroundScroll(amount);
            return;
        }
        robot.mouseWheel(amount);
    }

    /** Returns the current mouse cursor position. */
    public Point getPosition() {
        if (mode == Mode.BACKGROUND) {
            return backgroundGetPosition();
        }
        return MouseInfo.getPointerInfo().getLocation();
    }

    // ── Foreground implementations (Robot) ─────────────────────────────────────

    private void foregroundClick(int x, int y, int button) {
        robot.mouseMove(x, y);
        int mask = buttonToMask(button);
        robot.mousePress(mask);
        robot.delay(50);
        robot.mouseRelease(mask);
    }

    private void foregroundDrag(int x1, int y1, int x2, int y2) {
        int mask = InputEvent.BUTTON1_DOWN_MASK;
        robot.mouseMove(x1, y1);
        robot.mousePress(mask);
        robot.delay(100);
        int steps = 20;
        for (int i = 1; i <= steps; i++) {
            int cx = x1 + (x2 - x1) * i / steps;
            int cy = y1 + (y2 - y1) * i / steps;
            robot.mouseMove(cx, cy);
            robot.delay(15);
        }
        robot.delay(50);
        robot.mouseRelease(mask);
    }

    // ── Background implementations (Win32 SendMessage) ─────────────────────────

    private void checkBackgroundMode() {
        if (targetHwnd == 0) {
            throw new IllegalStateException(
                "BACKGROUND mode requires a target window (setTargetHwnd)");
        }
    }

    private void backgroundMouseMove(int x, int y) {
        checkBackgroundMode();
        long lParam = makeLParam(x, y);
        User32.INSTANCE.SendMessage(
            WindowFinder.toHwnd(targetHwnd),
            WM_MOUSEMOVE, new WinDef.WPARAM(0), new WinDef.LPARAM(lParam));
    }

    private void backgroundClick(int x, int y, int button) {
        checkBackgroundMode();
        long lParam = makeLParam(x, y);
        int msgDown, msgUp;
        switch (button) {
            case 2 -> { msgDown = WM_MBUTTONDOWN; msgUp = WM_MBUTTONUP; }
            case 3 -> { msgDown = WM_RBUTTONDOWN; msgUp = WM_RBUTTONUP; }
            default -> { msgDown = WM_LBUTTONDOWN; msgUp = WM_LBUTTONUP; }
        }
        WinDef.HWND h = WindowFinder.toHwnd(targetHwnd);
        WinDef.LPARAM lp = new WinDef.LPARAM(lParam);
        User32.INSTANCE.SendMessage(h, msgDown, new WinDef.WPARAM(0), lp);
        User32.INSTANCE.SendMessage(h, msgUp, new WinDef.WPARAM(0), lp);
    }

    private void backgroundDrag(int x1, int y1, int x2, int y2) {
        checkBackgroundMode();
        WinDef.HWND h = WindowFinder.toHwnd(targetHwnd);
        User32.INSTANCE.SendMessage(h, WM_LBUTTONDOWN, new WinDef.WPARAM(0), new WinDef.LPARAM(makeLParam(x1, y1)));
        int steps = 20;
        for (int i = 1; i <= steps; i++) {
            int cx = x1 + (x2 - x1) * i / steps;
            int cy = y1 + (y2 - y1) * i / steps;
            User32.INSTANCE.SendMessage(h, WM_MOUSEMOVE, new WinDef.WPARAM(MK_LBUTTON), new WinDef.LPARAM(makeLParam(cx, cy)));
        }
        User32.INSTANCE.SendMessage(h, WM_LBUTTONUP, new WinDef.WPARAM(0), new WinDef.LPARAM(makeLParam(x2, y2)));
    }

    private void backgroundScroll(int amount) {
        checkBackgroundMode();
        int delta = -amount * WHEEL_DELTA;
        long wParam = (long) (delta << 16);
        User32.INSTANCE.SendMessage(
            WindowFinder.toHwnd(targetHwnd),
            WM_MOUSEWHEEL, new WinDef.WPARAM(wParam), new WinDef.LPARAM(0));
    }

    private Point backgroundGetPosition() {
        checkBackgroundMode();
        Rectangle rect = WindowFinder.getWindowRect(targetHwnd);
        if (rect == null) return new Point(0, 0);
        return new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
    }

    // ── Win32 constants & helpers ──────────────────────────────────────────────

    private static final int WM_MOUSEMOVE   = 0x0200;
    private static final int WM_LBUTTONDOWN = 0x0201;
    private static final int WM_LBUTTONUP   = 0x0202;
    private static final int WM_RBUTTONDOWN = 0x0204;
    private static final int WM_RBUTTONUP   = 0x0205;
    private static final int WM_MBUTTONDOWN = 0x0207;
    private static final int WM_MBUTTONUP   = 0x0208;
    private static final int WM_MOUSEWHEEL  = 0x020A;
    private static final int MK_LBUTTON     = 0x0001;
    private static final int WHEEL_DELTA    = 120;

    /** Pack client-area (x, y) into LPARAM (low word = x, high word = y). */
    private static long makeLParam(int x, int y) {
        return (long) x & 0xFFFFL | ((long) y & 0xFFFFL) << 16;
    }

    private static int buttonToMask(int button) {
        return switch (button) {
            case 1 -> InputEvent.BUTTON1_DOWN_MASK;
            case 2 -> InputEvent.BUTTON2_DOWN_MASK;
            case 3 -> InputEvent.BUTTON3_DOWN_MASK;
            default -> InputEvent.BUTTON1_DOWN_MASK;
        };
    }
}
