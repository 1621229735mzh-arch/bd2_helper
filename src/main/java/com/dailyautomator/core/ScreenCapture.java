package com.dailyautomator.core;

import java.awt.*;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinNT;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Screen capture utility using java.awt.Robot.
 * Provides raw screen-region capture and HWND-based window capture.
 * The caller is responsible for ensuring the target window is visible
 * and on screen before calling {@link #captureWindow(long)}.
 */
public class ScreenCapture {

    private final Robot robot;

    public ScreenCapture(Robot robot) {
        this.robot = robot;
    }

    /** Capture the entire virtual screen (all monitors combined). */
    public BufferedImage captureFullScreen() {
        Rectangle fullScreen = new Rectangle(
            Toolkit.getDefaultToolkit().getScreenSize()
        );
        return robot.createScreenCapture(fullScreen);
    }

    /** Capture a specific region of the screen. */
    public BufferedImage captureRegion(int x, int y, int width, int height) {
        return robot.createScreenCapture(new Rectangle(x, y, width, height));
    }

    /** Capture the current default monitor's full area. */
    public BufferedImage captureCurrentMonitor() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice defaultDevice = ge.getDefaultScreenDevice();
        DisplayMode mode = defaultDevice.getDisplayMode();
        Rectangle monitorBounds = new Rectangle(0, 0, mode.getWidth(), mode.getHeight());
        return robot.createScreenCapture(monitorBounds);
    }

    /** Capture the area defined by a window rectangle. */
    public BufferedImage captureWindow(Rectangle windowRect) {
        return robot.createScreenCapture(windowRect);
    }

    /**
     * Capture a window's client area by HWND.
     * The window must be visible and on screen (foreground or not obscured).
     * Use {@link WindowFinder#isSuitable(long)} to verify before calling.
     *
     * @param hwnd the target window handle (from {@link WindowFinder})
     * @return the captured client area image
     * @throws IllegalArgumentException if {@code hwnd} is 0
     */
    public BufferedImage captureWindow(long hwnd) {
        if (hwnd == 0) {
            throw new IllegalArgumentException("HWND must not be 0");
        }
        Point clientOrigin = WindowFinder.getClientScreenOrigin(hwnd);
        Rectangle clientRect = WindowFinder.getClientRect(hwnd);
        return robot.createScreenCapture(
            new Rectangle(clientOrigin.x, clientOrigin.y,
                          clientRect.width, clientRect.height)
        );
    }

    /** Saves image to a timestamped PNG file in the working directory. */
    public String saveToFile(BufferedImage image) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        return saveToFile(image, "screenshot_" + timestamp + ".png");
    }

    /** Saves image to the specified path (PNG by default). */
    public String saveToFile(BufferedImage image, String path) {
        return saveToFile(image, path, "png");
    }

    /** Saves image to the specified path with the given format. */
    public String saveToFile(BufferedImage image, String path, String format) {
        try {
            File file = new File(path);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            ImageIO.write(image, format, file);
            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save screenshot to " + path, e);
        }
    }

    // ── BitBlt + CAPTUREBLT capture (works with DirectX/Unity windows) ──

    /** Extended user32 binding for WindowFromDC etc. */
    private interface User32Capture extends com.sun.jna.Library {
        User32Capture INSTANCE = Native.load("user32", User32Capture.class);
        boolean PrintWindow(WinDef.HWND hwnd, WinDef.HDC hdc, int nFlags);
        int PW_CLIENTONLY = 1;
    }

    /** Extended GDI32 binding for BitBlt with CAPTUREBLT */
    private interface GDI32Capture extends com.sun.jna.Library {
        GDI32Capture INSTANCE = Native.load("gdi32", GDI32Capture.class);
        boolean BitBlt(WinDef.HDC hdcDest, int xDest, int yDest, int wDest, int hDest,
                       WinDef.HDC hdcSrc, int xSrc, int ySrc, int rop);
        int SRCCOPY = 0x00CC0020;
        int CAPTUREBLT = 0x40000000;
    }

    /**
     * Capture a window by BitBlt from the screen DC at the window's current screen position.
     * Uses CAPTUREBLT flag to capture DirectX/Unity hardware-accelerated content.
     * Falls back to Robot.createScreenCapture if BitBlt fails.
     */
    public BufferedImage captureWindowByBitBlt(long hwnd) {
        if (hwnd == 0) return null;

        WinDef.HWND windowHandle = WindowFinder.toHwnd(hwnd);

        // Get the window rect in screen coordinates
        WinDef.RECT rect = new WinDef.RECT();
        if (!User32.INSTANCE.GetWindowRect(windowHandle, rect)) return null;
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;
        if (width <= 0 || height <= 0) return null;

        // Get the screen DC
        WinDef.HDC hdcScreen = User32.INSTANCE.GetDC(null);
        if (hdcScreen == null) return null;
        try {
            // Create memory DC and bitmap
            WinDef.HDC hdcMem = GDI32.INSTANCE.CreateCompatibleDC(hdcScreen);
            if (hdcMem == null) return null;
            try {
                WinDef.HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcScreen, width, height);
                if (hBitmap == null) return null;
                try {
                    GDI32.INSTANCE.SelectObject(hdcMem, hBitmap);

                    // BitBlt with CAPTUREBLT from screen DC
                    GDI32Capture.INSTANCE.BitBlt(
                        hdcMem, 0, 0, width, height,
                        hdcScreen, rect.left, rect.top,
                        GDI32Capture.SRCCOPY | GDI32Capture.CAPTUREBLT
                    );

                    // Read the bitmap into a BufferedImage
                    WinGDI.BITMAPINFO bmi = new WinGDI.BITMAPINFO();
                    bmi.bmiHeader.biWidth = width;
                    bmi.bmiHeader.biHeight = -height; // top-down
                    bmi.bmiHeader.biPlanes = 1;
                    bmi.bmiHeader.biBitCount = 32;
                    bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
                    bmi.bmiHeader.biSize = bmi.bmiHeader.size();

                    Memory pixelBuffer = new Memory((long) width * height * 4L);
                    GDI32.INSTANCE.GetDIBits(hdcMem, hBitmap, 0, height, pixelBuffer, bmi, WinGDI.DIB_RGB_COLORS);

                    int[] pixels = new int[width * height];
                    pixelBuffer.read(0, pixels, 0, width * height);

                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    image.setRGB(0, 0, width, height, pixels, 0, width);
                    return image;
                } finally {
                    GDI32.INSTANCE.DeleteObject(hBitmap);
                }
            } finally {
                GDI32.INSTANCE.DeleteDC(hdcMem);
            }
        } finally {
            User32.INSTANCE.ReleaseDC(null, hdcScreen);
        }
    }
}
