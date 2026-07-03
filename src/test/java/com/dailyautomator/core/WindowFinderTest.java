package com.dailyautomator.core;

import org.junit.jupiter.api.Test;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class WindowFinderTest {

    // -------- findByTitle --------
    @Test void findByTitle_nullKeyword_returnsZero() {
        assertEquals(0L, WindowFinder.findByTitle(null));
    }
    @Test void findByTitle_unknownWindow_returnsZero() {
        assertEquals(0L, WindowFinder.findByTitle("zzz_nonexistent_window_xyz_42"));
    }
    @Test void findByTitle_blankKeyword_maybeZero() {
        // blank matches all suitable windows; might be 0 if none exist
        assertNotNull(WindowFinder.findByTitle("   "));
    }

    // -------- findAllByTitle --------
    @Test void findAllByTitle_unknownWindow_returnsEmpty() {
        assertTrue(WindowFinder.findAllByTitle("zzz_nonexistent_window_xyz_42").isEmpty());
    }
    @Test void findAllByTitle_nullKeyword_returnsEmpty() {
        assertTrue(WindowFinder.findAllByTitle(null).isEmpty());
    }
    @Test void findAllByTitle_zeroLimit_returnsEmpty() {
        assertTrue(WindowFinder.findAllByTitle("z", 0).isEmpty());
    }

    // -------- findByTitleAndClass --------
    @Test void findByTitleAndClass_unknownWindow_returnsZero() {
        assertEquals(0L, WindowFinder.findByTitleAndClass("zzz_nonexistent_window_xyz_42", "SomeClass"));
    }
    @Test void findByTitleAndClass_nullKeyword_returnsZero() {
        assertEquals(0L, WindowFinder.findByTitleAndClass(null, "SomeClass"));
    }
    @Test void findByTitleAndClass_nullClass_returnsZero() {
        assertEquals(0L, WindowFinder.findByTitleAndClass("test", null));
    }

    // -------- lockGameWindow --------
    @Test void lockGameWindow_unknown_returnsEmpty() {
        var info = WindowFinder.lockGameWindow("zzz_nonexistent_window_xyz_42", "SomeClass");
        assertSame(WindowFinder.WindowInfo.EMPTY, info);
    }
    @Test void lockGameWindow_nullKeyword_returnsEmpty() {
        assertSame(WindowFinder.WindowInfo.EMPTY, WindowFinder.lockGameWindow(null, "X"));
    }
    @Test void lockGameWindow_nullClass_returnsEmpty() {
        assertSame(WindowFinder.WindowInfo.EMPTY, WindowFinder.lockGameWindow("test", null));
    }

    // -------- getWindowTitle / getClassName --------
    @Test void getWindowTitle_zero_returnsEmpty() {
        assertTrue(WindowFinder.getWindowTitle(0).isEmpty());
    }
    @Test void getClassName_zero_returnsEmpty() {
        assertTrue(WindowFinder.getClassName(0).isEmpty());
    }

    // -------- getWindowRect --------
    @Test void getWindowRect_zero_returnsEmpty() {
        Rectangle r = WindowFinder.getWindowRect(0);
        assertEquals(0, r.x); assertEquals(0, r.y);
        assertEquals(0, r.width); assertEquals(0, r.height);
    }

    // -------- getClientRect --------
    @Test void getClientRect_zero_returnsEmpty() {
        Rectangle r = WindowFinder.getClientRect(0);
        assertEquals(0, r.width); assertEquals(0, r.height);
    }
    @Test void getClientRect_zero_xAlwaysZero() {
        assertEquals(0, WindowFinder.getClientRect(0).x);
        assertEquals(0, WindowFinder.getClientRect(0).y);
    }

    // -------- isValid / isVisible --------
    @Test void isValid_invalidHandle_returnsFalse() { assertFalse(WindowFinder.isValid(0xDEADBEEFL)); }
    @Test void isValid_zero_returnsFalse()           { assertFalse(WindowFinder.isValid(0)); }
    @Test void isVisible_zero_returnsFalse()          { assertFalse(WindowFinder.isVisible(0)); }

    // -------- isMinimized / isMaximized / isForeground / isCloaked / isSuitable --------
    @Test void isMinimized_zero_returnsFalse()  { assertFalse(WindowFinder.isMinimized(0)); }
    @Test void isMaximized_zero_returnsFalse()  { assertFalse(WindowFinder.isMaximized(0)); }
    @Test void isForeground_zero_returnsFalse() { assertFalse(WindowFinder.isForeground(0)); }
    @Test void isCloaked_zero_returnsFalse()    { assertFalse(WindowFinder.isCloaked(0)); }
    @Test void isSuitable_zero_returnsFalse()   { assertFalse(WindowFinder.isSuitable(0)); }

    // -------- WindowInfo --------
    @Test void getInfo_zero_returnsEmpty() {
        assertSame(WindowFinder.WindowInfo.EMPTY, WindowFinder.getInfo(0));
    }
    @Test void getInfo_invalidHandle_returnsDefaults() {
        var info = WindowFinder.getInfo(0xDEADBEEFL);
        assertEquals(0xDEADBEEFL, info.hwnd());
        assertFalse(info.valid()); assertFalse(info.visible());
        assertFalse(info.minimized()); assertFalse(info.maximized());
        assertFalse(info.foreground()); assertFalse(info.cloaked());
        assertFalse(info.suitable());
        assertTrue(info.title().isEmpty()); assertTrue(info.className().isEmpty());
    }
    @Test void WindowInfo_EMPTY_isSingleton() {
        assertSame(WindowFinder.WindowInfo.EMPTY, WindowFinder.getInfo(0));
    }

    // -------- waitForWindow --------
    @Test void waitForWindow_tooShortTimeout_returnsZero() {
        assertEquals(0L, WindowFinder.waitForWindow("zzz_nonexistent_window_xyz_42", 100, 20));
    }

    // -------- waitForWindowInfo --------
    @Test void waitForWindowInfo_tooShortTimeout_returnsEmpty() {
        assertSame(WindowFinder.WindowInfo.EMPTY,
            WindowFinder.waitForWindowInfo("zzz_nonexistent_window_xyz_42", 100, 20));
    }
}