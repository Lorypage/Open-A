package cn.lazymoon.config.impl;

import cn.lazymoon.Client;
import cn.lazymoon.config.Config;
import cn.lazymoon.ingameui.ui.ModuleWidget;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Comparator;
import java.util.List;

public class DragConfig extends Config {

    @Override
    public JsonObject saveConfig() {
        List<ModuleWidget> modules = (List<ModuleWidget>) Client.INSTANCE.getModuleManager().getAllWidgets();
        modules.sort(Comparator.comparing(ModuleWidget::getName));
        JsonObject object = new JsonObject();
        for (ModuleWidget dragging : modules) {
            JsonObject dragObject = new JsonObject();
            JsonObject draggingPosObject = new JsonObject();
            draggingPosObject.addProperty("x", dragging.getX());
            draggingPosObject.addProperty("y", dragging.getY());
            dragObject.add("values", draggingPosObject);
            object.add(dragging.getName(), dragObject);
        }
        return object;
    }

    @Override
    public void loadConfig(JsonObject object) {
        List<ModuleWidget> modules = (List<ModuleWidget>) Client.INSTANCE.getModuleManager().getAllWidgets();
        modules.sort(Comparator.comparing(ModuleWidget::getName));
        for (ModuleWidget dragging : modules) {
            if (object.has(dragging.getName())) {
                JsonElement element = object.get(dragging.getName());
                if (element.isJsonObject()) {
                    JsonObject dragObject = element.getAsJsonObject();
                    if (dragObject.has("values")) {
                        JsonObject draggingPosObject = dragObject.get("values").getAsJsonObject();
                        if (draggingPosObject.has("x")) {
                            dragging.setX(draggingPosObject.get("x").getAsFloat());
                        }
                        if (draggingPosObject.has("y")) {
                            dragging.setY(draggingPosObject.get("y").getAsFloat());
                        }
                    }
                }
            }
        }
    }

}
