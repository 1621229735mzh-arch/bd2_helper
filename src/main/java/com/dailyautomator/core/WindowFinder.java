package com.dailyautomator.core;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Custom JNA mapping for dwmapi.dll (Desktop Window Manager API).
 * Not provided in JNA-platform 5.14.0.
 */
interface DwmApi extends Library {
    DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class);
    int DwmGetWindowAttribute(WinDef.HWND hwnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
    int DWMWA_CLOAKED = 14;
}

/**
 * Extended user32 binding for methods missing from JNA-platform 5.14.0.
 */
interface User32Ex extends Library {
    User32Ex INSTANCE = Native.load("user32", User32Ex.class);
    boolean ClientToScreen(WinDef.HWND hWnd, WinDef.POINT lpPoint);
    boolean SetForegroundWindow(WinDef.HWND hWnd);
}

/**
 * Window lookup and management utility using JNA (user32.dll + dwmapi.dll).
 * All handles are raw HWND values ({@code long}). {@code 0L} = invalid/null handle.
 */
public class WindowFinder {

    // Window style constants
    private static final int WS_MINIMIZE = 0x20000000;
    private static final int WS_MAXIMIZE = 0x01000000;

    // ── HWND conversion ───────────────────────────────────────────────

    public static long toLong(WinDef.HWND hwnd) {
        return Pointer.nativeValue(hwnd.getPointer());
    }

    public static WinDef.HWND toHwnd(long hwnd) {
        return new WinDef.HWND(new Pointer(hwnd));
    }

    // ── Search ────────────────────────────────────────────────────

    /**
     * Find the first suitable visible window whose title contains {@code titleKeyword}.
     * Pass {@code ""} or {@code null} to return the first suitable window regardless of title.
     * "Suitable" = visible, not minimized, not DWM-cloaked, has a non-empty title.
     */
    public static long findByTitle(String titleKeyword) {
        if (titleKeyword == null) return 0L;
        List<Long> results = new ArrayList<>(1);
        String lowerKw = titleKeyword.toLowerCase();
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            long h = toLong(hwnd);
            if (isSuitable(h) && titleContains(h, lowerKw)) {
                results.add(h);
                return false;
            }
            return true;
        }, null);
        return results.isEmpty() ? 0L : results.get(0);
    }

    /**
     * Find all suitable visible windows whose titles contain the given keyword.
     * Pass {@code ""} or {@code null} to list all suitable windows.
     */
    public static List<Long> findAllByTitle(String titleKeyword, int limit) {
        if (titleKeyword == null || limit <= 0) return List.of();
        List<Long> results = Collections.synchronizedList(new ArrayList<>());
        String lowerKw = titleKeyword.toLowerCase();
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            if (results.size() >= limit) return false;
            long h = toLong(hwnd);
            if (isSuitable(h) && titleContains(h, lowerKw)) results.add(h);
            return true;
        }, null);
        return List.copyOf(results);
    }

    /** Convenience: findAllByTitle with no limit. */
    public static List<Long> findAllByTitle(String titleKeyword) {
        return findAllByTitle(titleKeyword, Integer.MAX_VALUE);
    }

    /**
     * Find the first suitable visible window matching both title keyword and class name.
     * More precise than {@link #findByTitle(String)}, avoids false matches when
     * multiple apps share similar titles.
     * <p>Example: {@code findByTitleAndClass("BrownDust II", "UnityWndClass")}
     */
    public static long findByTitleAndClass(String titleKeyword, String className) {
        if (titleKeyword == null || className == null || className.isBlank()) return 0L;
        List<Long> results = new ArrayList<>(1);
        String lowerKw = titleKeyword.toLowerCase();
        User32.INSTANCE.EnumWindows((hwnd, data) -> {
            long h = toLong(hwnd);
            if (isSuitable(h) && titleContains(h, lowerKw) && className.equals(getClassName(h))) {
                results.add(h);
                return false;
            }
            return true;
        }, null);
        return results.isEmpty() ? 0L : results.get(0);
    }

    // ── Lock (convenience) ──────────────────────────────────────────

    /**
     * One-shot "lock onto a game/app window": find by title + class, then
     * capture full info in a single step.
     * <p>Returns {@link WindowInfo#EMPTY} if the window is not found.
     */
    public static WindowInfo lockGameWindow(String titleKeyword, String className) {
        long hwnd = findByTitleAndClass(titleKeyword, className);
        return hwnd != 0 ? getInfo(hwnd) : WindowInfo.EMPTY;
    }

    // ── Window info queries ─────────────────────────────────────────

    public static String getWindowTitle(long hwnd) {
        if (hwnd == 0) return "";
        char[] buf = new char[1024];
        int len = User32.INSTANCE.GetWindowText(toHwnd(hwnd), buf, 1024);
        return len > 0 ? new String(buf, 0, len) : "";
    }

    public static String getClassName(long hwnd) {
        if (hwnd == 0) return "";
        char[] buf = new char[256];
        int len = User32.INSTANCE.GetClassName(toHwnd(hwnd), buf, 256);
        return len > 0 ? new String(buf, 0, len) : "";
    }

    /** Bounding rectangle in virtual-screen coordinates. Returns empty rect on failure. */
    public static Rectangle getWindowRect(long hwnd) {
        if (hwnd == 0) return new Rectangle(0, 0, 0, 0);
        WinDef.RECT rect = new WinDef.RECT();
        if (!User32.INSTANCE.GetWindowRect(toHwnd(hwnd), rect)) return new Rectangle(0, 0, 0, 0);
        int w = Math.max(0, rect.right - rect.left);
       int h = Math.max(0, rect.bottom - rect.top);
       return new Rectangle(rect.left, rect.top, w, h);
    }

    /**
     * Move the window to screen position (x, y) without changing its size or Z-order.
     */
    public static void moveWindow(long hwnd, int x, int y) {
        if (hwnd == 0) return;
        User32.INSTANCE.SetWindowPos(toHwnd(hwnd), null, x, y, 0, 0, 0x0001 | 0x0004);
    }

    /**
     * Client (content) rectangle of the window. The returned rectangle's
     * x/y are always 0; width/height give the usable drawing area.
     * To map to screen coordinates, combine with {@link #getWindowRect(long)}.
     */
    public static Rectangle getClientRect(long hwnd) {
        if (hwnd == 0) return new Rectangle(0, 0, 0, 0);
        WinDef.RECT rect = new WinDef.RECT();
        if (!User32.INSTANCE.GetClientRect(toHwnd(hwnd), rect)) return new Rectangle(0, 0, 0, 0);
        return new Rectangle(0, 0,
            Math.max(0, rect.right - rect.left),
            Math.max(0, rect.bottom - rect.top));
    }

    // ── Window state ──────────────────────────────────────────────

    public static boolean isValid(long hwnd) {
        return hwnd != 0 && User32.INSTANCE.IsWindow(toHwnd(hwnd));
    }

    public static boolean isVisible(long hwnd) {
        return hwnd != 0 && User32.INSTANCE.IsWindowVisible(toHwnd(hwnd));
    }

    public static boolean isMinimized(long hwnd) {
        if (hwnd == 0) return false;
        return (User32.INSTANCE.GetWindowLong(toHwnd(hwnd), WinUser.GWL_STYLE) & WS_MINIMIZE) != 0;
    }

    public static boolean isMaximized(long hwnd) {
        if (hwnd == 0) return false;
        return (User32.INSTANCE.GetWindowLong(toHwnd(hwnd), WinUser.GWL_STYLE) & WS_MAXIMIZE) != 0;
    }

    public static boolean isForeground(long hwnd) {
        if (hwnd == 0) return false;
        WinDef.HWND fg = User32.INSTANCE.GetForegroundWindow();
        return fg != null && toLong(fg) == hwnd;
    }

    /**
     * Convert client (0, 0) to screen coordinates for the given window.
     * Returns the screen-space origin of the client area top-left corner.
     */
    public static Point getClientScreenOrigin(long hwnd) {
        if (hwnd == 0) return new Point(0, 0);
        WinDef.POINT p = new WinDef.POINT(0, 0);
        User32Ex.INSTANCE.ClientToScreen(toHwnd(hwnd), p);
        return new Point(p.x, p.y);
    }

    /** Returns the HWND of the current foreground (active) window, or 0L if none. */
    public static long getForegroundHwnd() {
        WinDef.HWND fg = User32.INSTANCE.GetForegroundWindow();
        return fg != null ? toLong(fg) : 0L;
    }

    /** DWM-cloaked (hidden by virtual-desktop switch, snap layouts, etc.). */
    public static boolean isCloaked(long hwnd) {
        if (hwnd == 0) return false;
        IntByReference cloaked = new IntByReference(0);
        try {
            int hr = DwmApi.INSTANCE.DwmGetWindowAttribute(toHwnd(hwnd),
                DwmApi.DWMWA_CLOAKED, cloaked.getPointer(), 4);
            return hr == 0 && cloaked.getValue() != 0;
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * Composite check: is the window in a suitable state for automation?
     * Returns true when valid, visible, not minimized, not cloaked, and
     * has a non-empty title.
     */
    public static boolean isSuitable(long hwnd) {
        if (hwnd == 0 || !User32.INSTANCE.IsWindow(toHwnd(hwnd))) return false;
        if (!isVisible(hwnd) || isMinimized(hwnd) || isCloaked(hwnd)) return false;
        String title = getWindowTitle(hwnd);
        return title != null && !title.isBlank();
    }

    // ── Snapshot: WindowInfo (TOCTOU-safe query-at-once) ────────────

    /** Immutable snapshot of a window's state at one point in time. */
    public record WindowInfo(
        long hwnd, String title, String className, Rectangle rect,
        boolean valid, boolean visible, boolean minimized, boolean maximized,
        boolean foreground, boolean cloaked, boolean suitable
    ) {
        public static final WindowInfo EMPTY = new WindowInfo(
            0L, "", "", new Rectangle(0, 0, 0, 0),
            false, false, false, false, false, false, false
        );
    }

    /** Capture all window state in a single call to minimise TOCTOU races. */
    public static WindowInfo getInfo(long hwnd) {
        if (hwnd == 0) return WindowInfo.EMPTY;
        if (!isValid(hwnd)) {
            return new WindowInfo(hwnd, "", "", new Rectangle(0, 0, 0, 0),
                false, false, false, false, false, false, false);
        }
        String title   = getWindowTitle(hwnd);
        String cls     = getClassName(hwnd);
        Rectangle rect = getWindowRect(hwnd);
        boolean visible    = isVisible(hwnd);
        boolean minimized  = isMinimized(hwnd);
        boolean maximized  = isMaximized(hwnd);
        boolean foreground = isForeground(hwnd);
        boolean cloaked    = isCloaked(hwnd);
        boolean suitable   = visible && !minimized && !cloaked &&
                             title != null && !title.isBlank();

        return new WindowInfo(hwnd, title, cls, rect,
            true, visible, minimized, maximized, foreground, cloaked, suitable);
    }

    // ── Wait / polling ─────────────────────────────────────────────

    /** Poll until a window matching the title keyword appears, up to timeoutMs. */
    public static long waitForWindow(String titleKeyword, long timeoutMs, long intervalMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            long hwnd = findByTitle(titleKeyword);
            if (hwnd != 0) return hwnd;
            try { Thread.sleep(intervalMs); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 0;
            }
        }
        return 0;
    }

    /** Wait for window, return full info. */
    public static WindowInfo waitForWindowInfo(String titleKeyword, long timeoutMs, long intervalMs) {
        long hwnd = waitForWindow(titleKeyword, timeoutMs, intervalMs);
        return hwnd != 0 ? getInfo(hwnd) : WindowInfo.EMPTY;
    }

    // ── Internal ──────────────────────────────────────────────────

    private static boolean titleContains(long hwnd, String lowerKeyword) {
        String title = getWindowTitle(hwnd);
        return title != null && title.toLowerCase().contains(lowerKeyword);
    }
}
