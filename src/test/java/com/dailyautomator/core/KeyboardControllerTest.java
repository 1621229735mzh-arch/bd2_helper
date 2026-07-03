package com.dailyautomator.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.awt.*;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "dailyautomator.enableRobotTests", matches = "true")
class KeyboardControllerTest {

    private KeyboardController keyboardController;

    @BeforeEach
    void setUp() throws AWTException {
        keyboardController = new KeyboardController(new Robot());
    }

    @Test
    void tap_key_doesNotThrow() {
        assertDoesNotThrow(() -> keyboardController.tap(KeyEvent.VK_SPACE));
    }

    @Test
    void pressAndRelease_doesNotThrow() {
        assertDoesNotThrow(() -> {
            keyboardController.press(KeyEvent.VK_A);
            robot().delay(50);
            keyboardController.release(KeyEvent.VK_A);
        });
    }

    @Test
    void combo_singleModifier_doesNotThrow() {
        assertDoesNotThrow(() -> keyboardController.combo(KeyEvent.VK_CONTROL, KeyEvent.VK_C));
    }

    @Test
    void combo_multiModifier_doesNotThrow() {
        assertDoesNotThrow(() -> keyboardController.combo(
            new int[]{KeyEvent.VK_CONTROL, KeyEvent.VK_SHIFT}, KeyEvent.VK_ESCAPE));
    }

    @Test
    void type_text_doesNotThrow() {
        assertDoesNotThrow(() -> keyboardController.type("Hello, World! 123"));
    }

    @Test
    void type_emptyString_doesNotThrow() {
        assertDoesNotThrow(() -> keyboardController.type(""));
    }

    @Test
    void shortcutMethods_doNotThrow() {
        assertDoesNotThrow(keyboardController::copy);
        assertDoesNotThrow(keyboardController::paste);
        assertDoesNotThrow(keyboardController::selectAll);
        assertDoesNotThrow(keyboardController::save);
        assertDoesNotThrow(keyboardController::escape);
        assertDoesNotThrow(keyboardController::enter);
        assertDoesNotThrow(keyboardController::tab);
        assertDoesNotThrow(keyboardController::delete);
        assertDoesNotThrow(keyboardController::backspace);
        assertDoesNotThrow(keyboardController::space);
    }

    @Test
    void arrowMethods_doNotThrow() {
        assertDoesNotThrow(keyboardController::arrowUp);
        assertDoesNotThrow(keyboardController::arrowDown);
        assertDoesNotThrow(keyboardController::arrowLeft);
        assertDoesNotThrow(keyboardController::arrowRight);
    }

    private Robot robot() {
        try {
            return new Robot();
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }
}
