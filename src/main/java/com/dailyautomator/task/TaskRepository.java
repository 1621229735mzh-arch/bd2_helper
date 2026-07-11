package com.dailyautomator.task;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;


public class TaskRepository {
    private static final String FILE_NAME = "tasks.json";
    private final Path filePath;
    private final Gson gson;

    public TaskRepository() {
        this.filePath = Paths.get(System.getProperty("user.dir"), FILE_NAME);
        this.gson = buildGson();
    }

    public TaskRepository(Path customPath) {
        this.filePath = customPath;
        this.gson = buildGson();
    }

    private Gson buildGson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeHierarchyAdapter(Action.class, new ActionDeserializer())
            .registerTypeHierarchyAdapter(Action.class, new ActionSerializer())
            .create();
    }

    private List<TaskDefinition> parseTasks(JsonArray arr) {
        List<TaskDefinition> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject obj = arr.get(i).getAsJsonObject();
            String name = obj.get("name").getAsString();
            List<Action> actions = new ArrayList<>();
            JsonArray actionsArr = obj.getAsJsonArray("actions");
            if (actionsArr != null) {
                for (int j = 0; j < actionsArr.size(); j++) {
                    Action a = gson.fromJson(actionsArr.get(j), Action.class);
                    if (a != null) actions.add(a);
                }
            }
            TaskDefinition td = new TaskDefinition(name, actions);
            if (obj.has("breakpoint")) td.setBreakpoint(obj.get("breakpoint").getAsBoolean());
            list.add(td);
        }
        return list;
    }

    public List<TaskDefinition> loadAll() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(Files.readString(filePath, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("tasks");
            return arr != null ? parseTasks(arr) : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public List<TaskDefinition> loadQueue() {
        if (!Files.exists(filePath)) return new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseString(Files.readString(filePath, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("queue");
            return arr != null ? parseTasks(arr) : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void saveAll(List<TaskDefinition> tasks) {
        try {
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (TaskDefinition task : tasks) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", task.getName());
                JsonArray actionsArr = new JsonArray();
                for (Action a : task.getActions()) actionsArr.add(gson.toJsonTree(a));
                obj.add("actions", actionsArr);
                arr.add(obj);
            }
            root.add("tasks", arr);
            Files.writeString(filePath, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save tasks: " + e.getMessage());
        }
    }

    public void saveAll(List<TaskDefinition> tasks, List<TaskDefinition> queue) {
        try {
            JsonObject root = new JsonObject();

            JsonArray tasksArr = new JsonArray();
            for (TaskDefinition task : tasks) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", task.getName());
                JsonArray actionsArr = new JsonArray();
                for (Action a : task.getActions()) actionsArr.add(gson.toJsonTree(a));
                obj.add("actions", actionsArr);
                tasksArr.add(obj);
            }
            root.add("tasks", tasksArr);

            JsonArray queueArr = new JsonArray();
            for (TaskDefinition task : queue) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", task.getName());
                JsonArray actionsArr = new JsonArray();
                for (Action a : task.getActions()) actionsArr.add(gson.toJsonTree(a));
               obj.add("actions", actionsArr);
                obj.addProperty("breakpoint", task.isBreakpoint());
               queueArr.add(obj);
            }
            root.add("queue", queueArr);

            Files.writeString(filePath, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to save tasks: " + e.getMessage());
        }
    }

    private static class ActionSerializer implements JsonSerializer<Action> {
        @Override
        public JsonElement serialize(Action src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getType());
            if (src instanceof ClickAction c) {
                obj.addProperty("x", c.getX());
                obj.addProperty("y", c.getY());
            } else if (src instanceof WaitAction w) {
                obj.addProperty("durationMs", w.getDurationMs());
            } else if (src instanceof KeyPressAction k) {
                obj.addProperty("key", k.getKey());
            }
            return obj;
        }
    }

    private static class ActionDeserializer implements JsonDeserializer<Action> {
        @Override
        public Action deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString();
            switch (type) {
                case "click":
                    return new ClickAction(obj.get("x").getAsInt(), obj.get("y").getAsInt());
                case "wait":
                    return new WaitAction(obj.get("durationMs").getAsInt());
                case "keypress":
                    return new KeyPressAction(obj.get("key").getAsString());
                default:
                    throw new JsonParseException("Unknown type: " + type);
            }
        }
    }
}
