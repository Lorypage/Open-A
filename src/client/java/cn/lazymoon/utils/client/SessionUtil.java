package cn.lazymoon.utils.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.lang.reflect.Field;

public final class SessionUtil {

    private SessionUtil() {}

    public static void applySession(Session session) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Field field = MinecraftClient.class.getDeclaredField("session");
            field.setAccessible(true);
            field.set(client, session);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to replace Minecraft session", t);
        }
    }
}