package cn.lazymoon.config.impl;

import cn.lazymoon.Client;
import cn.lazymoon.config.Config;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.Value;
import cn.lazymoon.features.value.impl.*;
import com.google.gson.JsonObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModuleConfig extends Config {
    @Override
    public JsonObject saveConfig() {
        List<Module> modules = new ArrayList<>(Client.INSTANCE.getModuleManager().getAllModules());
        modules.sort(Comparator.comparing(Module::getName));
        JsonObject object = new JsonObject();

        for (Module module : modules) {
            JsonObject moduleObject = new JsonObject();

            moduleObject.addProperty("state", module.isState());
            moduleObject.addProperty("key", module.getKey());
            moduleObject.addProperty("hide", module.isHidden());

            JsonObject valuesObject = new JsonObject();

            for (Value<?> value : module.getValues()) {
                if (value instanceof NumberValue nv) {
                    valuesObject.addProperty(value.getName(), nv.get());
                } else if (value instanceof BoolValue bv) {
                    valuesObject.addProperty(value.getName(), bv.get());
                } else if (value instanceof ModeValue mv) {
                    valuesObject.addProperty(value.getName(), mv.get());
                } else if (value instanceof ColorValue cv) {
                    valuesObject.addProperty(value.getName(), cv.get().getRGB());
                    valuesObject.addProperty(value.getName() + "_alpha", cv.get().getAlpha());
                } else if (value instanceof StringValue tv) {
                    String encoded = java.util.Base64.getEncoder().encodeToString(tv.get().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    valuesObject.addProperty(value.getName(), encoded);
                } else if (value instanceof MultiBoolValue mb) {
                    JsonObject mbObject = new JsonObject();
                    mb.getValues().forEach(mbValue -> mbObject.addProperty(mbValue.getName(), mbValue.getValue()));
                    valuesObject.add(value.getName(), mbObject);
                }
            }

            moduleObject.add("values", valuesObject);
            object.add(module.getName(), moduleObject);
        }
        return object;
    }

    @Override
    public void loadConfig(JsonObject object) {
        List<Module> modules = new ArrayList<>(Client.INSTANCE.getModuleManager().getAllModules());
        modules.sort(Comparator.comparing(Module::getName));

        for (Module module : modules) {
            if (!object.has(module.getName())) continue;

            JsonObject moduleObject = object.get(module.getName()).getAsJsonObject();

            if (moduleObject.has("state")) {
                module.setState(moduleObject.get("state").getAsBoolean());
            }

            if (moduleObject.has("key")) {
                module.setKey(moduleObject.get("key").getAsInt());
            }

            if (moduleObject.has("hide")) {
                module.setHidden(moduleObject.get("hide").getAsBoolean());
            }

            if (!moduleObject.has("values")) continue;
            JsonObject valuesObject = moduleObject.get("values").getAsJsonObject();

            for (Value<?> value : module.getValues()) {
                if (!valuesObject.has(value.getName())) continue;

                if (value instanceof NumberValue nv) {
                    nv.setValue(valuesObject.get(value.getName()).getAsNumber().doubleValue());
                } else if (value instanceof BoolValue bv) {
                    bv.setValue(valuesObject.get(value.getName()).getAsBoolean());
                } else if (value instanceof ModeValue mv) {
                    mv.setValue(valuesObject.get(value.getName()).getAsString());
                } else if (value instanceof ColorValue cv) {
                    int rgb = valuesObject.get(value.getName()).getAsInt();
                    int alpha = valuesObject.has(value.getName() + "_alpha") ?
                            valuesObject.get(value.getName() + "_alpha").getAsInt() : 255;
                    Color color = new Color(rgb, true);
                    cv.set(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                } else if (value instanceof StringValue tv) {
                    try {
                        String decoded = new String(java.util.Base64.getDecoder().decode(valuesObject.get(value.getName()).getAsString()), java.nio.charset.StandardCharsets.UTF_8);
                        tv.setValue(decoded);
                    } catch (Exception e) {
                        tv.setValue(valuesObject.get(value.getName()).getAsString());
                    }
                } else if (value instanceof MultiBoolValue mb) {
                    JsonObject mbObject = valuesObject.get(value.getName()).getAsJsonObject();
                    mb.getValues().forEach(mbValue -> {
                        try {
                            mbValue.setValue(mbObject.get(mbValue.getName()).getAsBoolean());
                        } catch (Exception ignored) {
                        }
                    });
                }
            }
        }
    }
}