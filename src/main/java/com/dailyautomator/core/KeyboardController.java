package com.dailyautomator.core;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;

/**
 * Foreground keyboard controller using java.awt.Robot.
 * All keystrokes are sent via Robot — the target window must have focus.
 */
public class KeyboardController {

    private final Robot robot;

    // ── Constructors ────────────────────────────────────────────────────────────

    public KeyboardController(Robot robot) {
        this.robot = robot;
    }

    // ── Public API ──────────────────────────────────────────────────────────────

    /** Press a key down. */
    public void press(int keyCode) {
        robot.keyPress(keyCode);
    }

    /** Release a key. */
    public void release(int keyCode) {
        robot.keyRelease(keyCode);
    }

    /** Tap (press and release) a single key. */
    public void tap(int keyCode) {
        press(keyCode);
        robot.delay(50);
        release(keyCode);
    }

    /** Tap a modifier + key combination (e.g., Ctrl+C). */
    public void combo(int modifier, int key) {
        combo(new int[]{modifier}, key);
    }

    /** Tap a multi-modifier combination (e.g., Ctrl+Shift+Esc). */
    public void combo(int[] modifiers, int key) {
        for (int mod : modifiers) press(mod);
        robot.delay(20);
        tap(key);
        robot.delay(20);
        for (int i = modifiers.length - 1; i >= 0; i--) release(modifiers[i]);
    }

    /** Type a string of text character by character. */
    public void type(String text) {
        for (char c : text.toCharArray()) {
            int keyCode = charToKeyCode(c);
            boolean shiftNeeded = Character.isUpperCase(c) || isShiftChar(c);
            if (shiftNeeded) {
                combo(KeyEvent.VK_SHIFT, keyCode);
            } else {
                tap(keyCode);
            }
            robot.delay(30);
        }
    }

    /** Type text by placing it on the clipboard and pasting (Ctrl+V). */
    public void typeByClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(text);
        clipboard.setContents(selection, null);
        robot.delay(100);
        combo(KeyEvent.VK_CONTROL, KeyEvent.VK_V);
    }

    // ── Convenience shortcuts ──────────────────────────────────────────────────

    public void copy()       { combo(KeyEvent.VK_CONTROL, KeyEvent.VK_C); }
    public void paste()      { combo(KeyEvent.VK_CONTROL, KeyEvent.VK_V); }
    public void selectAll()  { combo(KeyEvent.VK_CONTROL, KeyEvent.VK_A); }
    public void save()       { combo(KeyEvent.VK_CONTROL, KeyEvent.VK_S); }
    public void escape()     { tap(KeyEvent.VK_ESCAPE); }
    public void enter()      { tap(KeyEvent.VK_ENTER); }
    public void tab()        { tap(KeyEvent.VK_TAB); }
    public void delete()     { tap(KeyEvent.VK_DELETE); }
    public void backspace()  { tap(KeyEvent.VK_BACK_SPACE); }
    public void space()      { tap(KeyEvent.VK_SPACE); }

    public void arrowUp()    { tap(KeyEvent.VK_UP); }
    public void arrowDown()  { tap(KeyEvent.VK_DOWN); }
    public void arrowLeft()  { tap(KeyEvent.VK_LEFT); }
    public void arrowRight() { tap(KeyEvent.VK_RIGHT); }

    // ── Internals ──────────────────────────────────────────────────────────────

    /** Determine whether a character needs Shift to type. */
    private static boolean isShiftChar(char c) {
        return "~!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0;
    }

    /** Map a character to a KeyEvent key code. */
    private static int charToKeyCode(char c) {
        char uc = Character.toUpperCase(c);
        if (uc >= 'A' && uc <= 'Z') return KeyEvent.VK_A + (uc - 'A');
        if (c >= '0' && c <= '9') return KeyEvent.VK_0 + (c - '0');
        return switch (c) {
            case ' '  -> KeyEvent.VK_SPACE;
            case '!'  -> KeyEvent.VK_1;
            case '@'  -> KeyEvent.VK_2;
            case '#'  -> KeyEvent.VK_3;
            case '$'  -> KeyEvent.VK_4;
            case '%'  -> KeyEvent.VK_5;
            case '^'  -> KeyEvent.VK_6;
            case '&'  -> KeyEvent.VK_7;
            case '*'  -> KeyEvent.VK_8;
            case '('  -> KeyEvent.VK_9;
            case ')'  -> KeyEvent.VK_0;
            case '_'  -> KeyEvent.VK_MINUS;
            case '+'  -> KeyEvent.VK_EQUALS;
            case '{'  -> KeyEvent.VK_OPEN_BRACKET;
            case '}'  -> KeyEvent.VK_CLOSE_BRACKET;
            case ':'  -> KeyEvent.VK_SEMICOLON;
            case '"'  -> KeyEvent.VK_QUOTE;
            case '<'  -> KeyEvent.VK_COMMA;
            case '>'  -> KeyEvent.VK_PERIOD;
            case '?'  -> KeyEvent.VK_SLASH;
            case '|'  -> KeyEvent.VK_BACK_SLASH;
            case '~'  -> KeyEvent.VK_BACK_QUOTE;
            case '-'  -> KeyEvent.VK_MINUS;
            case '='  -> KeyEvent.VK_EQUALS;
            case ','  -> KeyEvent.VK_COMMA;
            case '.'  -> KeyEvent.VK_PERIOD;
            case '/'  -> KeyEvent.VK_SLASH;
            case ';'  -> KeyEvent.VK_SEMICOLON;
            case '\'' -> KeyEvent.VK_QUOTE;
            case '['  -> KeyEvent.VK_OPEN_BRACKET;
            case ']'  -> KeyEvent.VK_CLOSE_BRACKET;
            case '\\' -> KeyEvent.VK_BACK_SLASH;
            case '`'  -> KeyEvent.VK_BACK_QUOTE;
            default   -> KeyEvent.VK_UNDEFINED;
        };
    }
}
