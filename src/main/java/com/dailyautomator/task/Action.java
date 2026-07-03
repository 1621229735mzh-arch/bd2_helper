package com.dailyautomator.task;

import java.util.function.Consumer;

public interface Action {
    String getType();
    void execute(ActionContext ctx) throws InterruptedException;
}
