package cn.lazymoon.config.impl;

import cn.lazymoon.config.Config;
import cn.lazymoon.ingameui.auth.AuthHistoryManager;
import com.google.gson.JsonObject;

public class AccountHistoryConfig extends Config {

    @Override
    public JsonObject saveConfig() {
        return AuthHistoryManager.saveToJson();
    }

    @Override
    public void loadConfig(JsonObject object) {
        AuthHistoryManager.loadFromJson(object);
    }
}