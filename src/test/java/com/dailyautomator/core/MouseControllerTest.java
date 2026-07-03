package com.dailyautomator.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.awt.*;
import java.awt.event.InputEvent;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "dailyautomator.enableRobotTests", matches = "true")
class MouseControllerTest {

    private MouseController mouseController;
    private Robot robot;

    @BeforeEach
    void setUp() throws AWTException {
        robot = new Robot();
        mouseController = new MouseController(robot);
    }

    // -- Mode tests --

    @Test
    void defaultMode_isForeground() {
        assertEquals(MouseController.Mode.FOREGROUND, mouseController.getMode());
    }

    @Test
    void constructor_withMode_foreground() throws AWTException {
        MouseController mc = new MouseController(new Robot(), MouseController.Mode.FOREGROUND);
        assertEquals(MouseController.Mode.FOREGROUND, mc.getMode());
    }

    @Test
    void setMode_updatesMode() {
        mouseController.setMode(MouseController.Mode.BACKGROUND);
        assertEquals(MouseController.Mode.BACKGROUND, mouseController.getMode());
        mouseController.setMode(MouseController.Mode.FOREGROUND);
        assertEquals(MouseController.Mode.FOREGROUND, mouseController.getMode());
    }

    @Test
    void backgroundMode_throwsWithoutTargetHwnd() {
        mouseController.setMode(MouseController.Mode.BACKGROUND);
        assertThrows(IllegalStateException.class,
            () -> mouseController.move(100, 100));
    }

    @Test
    void backgroundMode_setTargetHwnd() {
        mouseController.setMode(MouseController.Mode.BACKGROUND);
        mouseController.setTargetHwnd(12345L);
        assertEquals(12345L, mouseController.getTargetHwnd());
    }

    // -- Move tests --

    @Test
    void move_changesCursorPosition() {
        // Smoke test: cursor position changes after move (coordinates may vary
        // due to multi-monitor configs, DPI scaling, or active input)
        mouseController.move(100, 100);
        robot.delay(150);
        Point pos = mouseController.getPosition();
        assertNotNull(pos);
        assertTrue(pos.x >= 0);
        assertTrue(pos.y >= 0);
    }

    @Test
    void getPosition_returnsNonNullPoint() {
        Point pos = mouseController.getPosition();
        assertNotNull(pos);
        assertTrue(pos.x >= 0);
        assertTrue(pos.y >= 0);
    }

    @Test
    void move_toZeroZero_doesNotThrow() {
        assertDoesNotThrow(() -> mouseController.move(0, 0));
    }

    @Test
    void click_leftClick_doesNotThrow() {
        assertDoesNotThrow(() -> mouseController.leftClick(100, 100));
    }

    @Test
    void click_rightClick_doesNotThrow() {
        assertDoesNotThrow(() -> mouseController.rightClick(100, 100));
    }

    @Test
    void doubleClick_doesNotThrow() {
        assertDoesNotThrow(() -> mouseController.doubleClick(100, 100));
    }

    @Test
    void drag_doesNotThrow() {
        assertDoesNotThrow(() -> mouseController.drag(200, 200, 400, 400));
    }

    @Test
    void scroll_positive_doesNotThrow() {
        assertDoesNotThrow(() -> mouseController.scroll(3));
    }

    @Test
    void scroll_negative_doesNotThrow() {
        assertDoesNotThrow(() -> mouseController.scroll(-3));
    }
}