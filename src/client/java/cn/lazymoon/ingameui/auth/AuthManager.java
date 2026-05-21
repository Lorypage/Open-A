package cn.lazymoon.ingameui.auth;

import cn.lazymoon.Client;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public final class AuthManager {

    private AuthManager() {
    }

    public static AccountManagerOfflineAccount.MicrosoftLoginStart loginMicrosoftStart() {
        return AccountManagerOfflineAccount.loginMicrosoftStart();
    }
    public static CompletableFuture<AuthResult> loginWithSessionToken(String token) {
        return AccountManagerOfflineAccount.loginWithSessionToken(token)
                .thenApply(result -> applySessionIfNeeded(result, "Token"));
    }
    public static CompletableFuture<AuthResult> loginMicrosoft() {
        AccountManagerOfflineAccount.MicrosoftLoginStart start = loginMicrosoftStart();
        if (start == null || start.future() == null) {
            return CompletableFuture.completedFuture(AuthResult.failure("Microsoft login start failed"));
        }
        return start.future().thenApply(result -> applySessionIfNeeded(result, "Microsoft"));
    }

    public static CompletableFuture<AuthResult> loginWithRefreshToken(String token) {
        return AccountManagerOfflineAccount.loginWithRefreshToken(token)
                .thenApply(result -> applySessionIfNeeded(result, "Token"));
    }

    public static AuthResult loginOffline(String name) {
        AuthResult result = AccountManagerOfflineAccount.loginOffline(name);
        return applySessionIfNeeded(result, "Offline");
    }

    public static AuthResult applySessionIfNeeded(AuthResult result) {
        if (result == null || !result.success() || result.session() == null) {
            return result;
        }

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return result;

            Session session = result.session();

            for (Method m : client.getClass().getMethods()) {
                if (m.getName().equals("setSession")
                        && m.getParameterCount() == 1
                        && Session.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    m.invoke(client, session);
                    return result;
                }
            }

            for (Field f : client.getClass().getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase("session")
                        && Session.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(client, session);
                    return result;
                }
            }
        } catch (Throwable t) {
            System.out.println("applySessionIfNeeded failed: " + t.getMessage());
        }

        return result;
    }

    public static AuthResult applySessionIfNeeded(AuthResult result, String type) {
        AuthResult applied = applySessionIfNeeded(result);

        if (applied != null && applied.success() && applied.username() != null) {
            AuthHistoryManager.addHistory(type, applied.username(), applied.uuid());

            if (applied.refreshToken() != null && !applied.refreshToken().isBlank()) {
                AuthCredentialStore.put(applied.uuid(), applied.username(), applied.refreshToken());
            }

            try {
                if (Client.INSTANCE != null && Client.INSTANCE.getConfigManager() != null) {
                    Client.INSTANCE.getConfigManager().saveAccountHistoryConfig();
                }
            } catch (Throwable ignored) {
            }
        }

        return applied;
    }
}