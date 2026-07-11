package com.dailyautomator.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskDefinition {
    private String name;
   private final List<Action> actions;

    private boolean breakpoint = false;

    public TaskDefinition(String name) {
        this.name = name;
        this.actions = new ArrayList<>();
    }

    public TaskDefinition(String name, List<Action> actions) {
        this.name = name;
        this.actions = new ArrayList<>(actions);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isBreakpoint() { return breakpoint; }
    public void setBreakpoint(boolean b) { this.breakpoint = b; }

    public List<Action> getActions() { return Collections.unmodifiableList(actions); }
    public void addAction(Action action) { actions.add(action); }
    public void removeAction(int index) { if (index >= 0 && index < actions.size()) actions.remove(index); }
    public void moveActionUp(int index) { if (index > 0 && index < actions.size()) Collections.swap(actions, index, index - 1); }
    public void moveActionDown(int index) { if (index >= 0 && index < actions.size() - 1) Collections.swap(actions, index, index + 1); }
    public int actionCount() { return actions.size(); }
    public void setAction(int index, Action action) { if (index >= 0 && index < actions.size()) actions.set(index, action); }

    @Override
    public String toString() {
        return "Task[" + name + "] (" + actions.size() + " steps)";
    }
}
