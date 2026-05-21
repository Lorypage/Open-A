package cn.lazymoon.ingameui.auth;

import cn.lazymoon.config.ConfigManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthCredentialStore {

    private static final Map<String, String> BY_UUID = new ConcurrentHashMap<>();
    private static final Map<String, String> BY_USERNAME = new ConcurrentHashMap<>();

    private static boolean loaded = false;

    private AuthCredentialStore() {
    }
    public static synchronized void remove(String uuid, String username) {
        ensureLoaded();

        if (uuid != null && !uuid.isBlank()) {
            BY_UUID.remove(normalizeUuid(uuid));
        }

        if (username != null && !username.isBlank()) {
            BY_USERNAME.remove(normalizeUsername(username));
        }

        save();
    }

    public static synchronized void clear() {
        ensureLoaded();
        BY_UUID.clear();
        BY_USERNAME.clear();
        save();
    }
    private static File file() {
        try {
            File base = ConfigManager.getDir();
            if (base != null) {
                return new File(base, "auth-credentials.json");
            }
        } catch (Throwable ignored) {
        }
        return new File("Arcane/auth-credentials.json");
    }

    private static synchronized void ensureLoaded() {
        if (!loaded) {
            load();
            loaded = true;
        }
    }

    public static synchronized void load() {
        BY_UUID.clear();
        BY_USERNAME.clear();

        File file = file();
        if (!file.exists()) {
            return;
        }

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            if (content == null || content.isBlank()) {
                return;
            }

            JsonObject root = JsonParser.parseString(content).getAsJsonObject();

            if (root.has("byUuid") && root.get("byUuid").isJsonObject()) {
                JsonObject uuids = root.getAsJsonObject("byUuid");
                for (String key : uuids.keySet()) {
                    BY_UUID.put(normalizeUuid(key), uuids.get(key).getAsString());
                }
            }

            if (root.has("byUsername") && root.get("byUsername").isJsonObject()) {
                JsonObject usernames = root.getAsJsonObject("byUsername");
                for (String key : usernames.keySet()) {
                    BY_USERNAME.put(normalizeUsername(key), usernames.get(key).getAsString());
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static synchronized void save() {
        try {
            File file = file();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            JsonObject root = new JsonObject();
            JsonObject byUuid = new JsonObject();
            JsonObject byUsername = new JsonObject();

            for (Map.Entry<String, String> entry : BY_UUID.entrySet()) {
                byUuid.addProperty(entry.getKey(), entry.getValue());
            }

            for (Map.Entry<String, String> entry : BY_USERNAME.entrySet()) {
                byUsername.addProperty(entry.getKey(), entry.getValue());
            }

            root.add("byUuid", byUuid);
            root.add("byUsername", byUsername);

            Files.writeString(file.toPath(), root.toString(), StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
    }

    public static void put(UUID uuid, String username, String refreshToken) {
        ensureLoaded();

        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        if (uuid != null) {
            BY_UUID.put(normalizeUuid(uuid.toString()), refreshToken);
        }

        if (username != null && !username.isBlank()) {
            BY_USERNAME.put(normalizeUsername(username), refreshToken);
        }

        save();
    }

    public static String getByUuid(String uuid) {
        ensureLoaded();

        if (uuid == null || uuid.isBlank()) {
            return null;
        }

        return BY_UUID.get(normalizeUuid(uuid));
    }

    public static String getByUsername(String username) {
        ensureLoaded();

        if (username == null || username.isBlank()) {
            return null;
        }

        return BY_USERNAME.get(normalizeUsername(username));
    }

    private static String normalizeUuid(String uuid) {
        return uuid.replace("-", "").toLowerCase();
    }

    private static String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }
}