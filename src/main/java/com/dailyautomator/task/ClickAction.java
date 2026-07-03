package com.dailyautomator.task;

import com.dailyautomator.core.CoordUtils;
import com.dailyautomator.core.MouseController;
import java.awt.Point;

public class ClickAction implements Action {

    private final int x;
    private final int y;

    public ClickAction(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String getType() { return "click"; }
    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public void execute(ActionContext ctx) throws InterruptedException {
        Point screen = CoordUtils.offsetToScreen(ctx.hwnd, x, y);
        ctx.log.accept("Click: offset (" + x + "," + y + ") -> screen ("
            + screen.x + "," + screen.y + ")");
        ctx.mouse.setMode(MouseController.Mode.FOREGROUND);
        ctx.mouse.leftClick(screen.x, screen.y);
    }

    @Override
    public String toString() {
        return "Click(" + x + "," + y + ")";
    }
}
