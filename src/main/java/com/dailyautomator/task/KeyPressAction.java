package com.dailyautomator.task;

import java.awt.event.KeyEvent;

public class KeyPressAction implements Action {

    private final String key;

    public KeyPressAction(String key) {
        this.key = key.toLowerCase();
    }

    @Override
    public String getType() {
        return "keypress";
    }

    public String getKey() {
        return key;
    }

    @Override
    public void execute(ActionContext ctx) throws InterruptedException {
        ctx.log.accept("KeyPress: " + key.toUpperCase());
        int keyCode = switch (key) {
            case "esc" -> KeyEvent.VK_ESCAPE;
            case "h"   -> KeyEvent.VK_H;
            case "f"   -> KeyEvent.VK_F;
            default    -> KeyEvent.VK_UNDEFINED;
        };
        if (keyCode != KeyEvent.VK_UNDEFINED) {
            ctx.keyboard.tap(keyCode);
        } else {
            ctx.log.accept("Unknown key: " + key);
        }
    }

    @Override
    public String toString() {
        return "Key(" + key.toUpperCase() + ")";
    }
}
