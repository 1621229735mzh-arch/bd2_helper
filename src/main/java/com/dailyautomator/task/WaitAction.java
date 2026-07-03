package com.dailyautomator.task;

public class WaitAction implements Action {

    private final int durationMs;

    public WaitAction(int durationMs) {
        this.durationMs = durationMs;
    }

    @Override
    public String getType() { return "wait"; }
    public int getDurationMs() { return durationMs; }

    @Override
    public void execute(ActionContext ctx) throws InterruptedException {
        int sec = durationMs / 1000;
        ctx.log.accept(sec > 0 ? "Wait " + sec + "s..." : "Wait " + durationMs + "ms...");
        Thread.sleep(durationMs);
    }

    @Override
    public String toString() {
        if (durationMs >= 1000) return "Wait(" + (durationMs / 1000) + "s)";
        return "Wait(" + durationMs + "ms)";
    }
}
