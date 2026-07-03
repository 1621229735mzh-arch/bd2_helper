package com.dailyautomator.core;

import java.awt.Point;
import java.awt.MouseInfo;

public class CoordUtils {

    private CoordUtils() {}

    public static Point offsetToScreen(int anchorX, int anchorY, int offsetX, int offsetY) {
        return new Point(anchorX + offsetX, anchorY + offsetY);
    }

    public static Point getMouseScreenPosition() {
        return MouseInfo.getPointerInfo().getLocation();
    }

    /**
     * Returns the current mouse position relative to the window's client-area
     * top-left corner (0,0). Uses {@link WindowFinder#getClientScreenOrigin}
     * to determine the window's screen origin and subtracts it from the
     * absolute mouse position.
     */
    public static Point getMouseOffset(long hwnd) {
        Point mouse = getMouseScreenPosition();
        Point origin = WindowFinder.getClientScreenOrigin(hwnd);
        return new Point(mouse.x - origin.x, mouse.y - origin.y);
    }

    /**
     * Converts a window-relative offset {@code (offsetX, offsetY)} to screen
     * coordinates by adding the window's current client-area top-left screen
     * position.  Equivalent to treating the window's content top-left as (0,0)
     * and converting to absolute screen coordinates.
     */
    public static Point offsetToScreen(long hwnd, int offsetX, int offsetY) {
        Point origin = WindowFinder.getClientScreenOrigin(hwnd);
        return new Point(origin.x + offsetX, origin.y + offsetY);
    }
}
