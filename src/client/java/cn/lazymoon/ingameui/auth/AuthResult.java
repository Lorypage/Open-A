package cn.lazymoon.ingameui.auth;

import net.minecraft.client.session.Session;

import java.util.UUID;

public final class AuthResult {

    private final boolean success;
    private final String message;
    private final String username;
    private final UUID uuid;
    private final String accessToken;
    private final String refreshToken;
    private final Session session;

    private AuthResult(
            boolean success,
            String message,
            String username,
            UUID uuid,
            String accessToken,
            String refreshToken,
            Session session
    ) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.session = session;
    }

    public static AuthResult success(
            String message,
            String username,
            UUID uuid,
            String accessToken,
            String refreshToken,
            Session session
    ) {
        return new AuthResult(true, message, username, uuid, accessToken, refreshToken, session);
    }

    public static AuthResult failure(String message) {
        return new AuthResult(false, message, null, null, null, null, null);
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public String username() {
        return username;
    }

    public UUID uuid() {
        return uuid;
    }

    public String accessToken() {
        return accessToken;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public Session session() {
        return session;
    }

    @Override
    public String toString() {
        return "AuthResult{success=" + success + ", message='" + message + "', username='" + username + "', uuid=" + uuid + "}";
    }
}