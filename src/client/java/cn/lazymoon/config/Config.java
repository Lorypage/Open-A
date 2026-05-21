package cn.lazymoon.config;

import com.google.gson.JsonObject;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public abstract class Config {
    public abstract JsonObject saveConfig();

    public abstract void loadConfig(JsonObject object);
}
