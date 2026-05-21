package cn.lazymoon.ingameui.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.*;

public final class AuthHistoryManager {

    public static final class HistoryEntry {
        private final String type;
        private final String username;
        private final String uuid;
        private final long lastLoginTime;

        public HistoryEntry(String type, String username, String uuid, long lastLoginTime) {
            this.type = type == null ? "Unknown" : type;
            this.username = username == null ? "Unknown" : username;
            this.uuid = uuid == null ? "" : uuid;
            this.lastLoginTime = lastLoginTime;
        }

        public String type() {
            return type;
        }

        public String username() {
            return username;
        }

        public String uuid() {
            return uuid;
        }

        public long lastLoginTime() {
            return lastLoginTime;
        }

        public String formatTime() {
            if (lastLoginTime <= 0L) return "-";
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(lastLoginTime));
        }
    }

    private static final int MAX_HISTORY = 20;
    private static final List<HistoryEntry> HISTORY = new ArrayList<>();

    private AuthHistoryManager() {
    }
    public static boolean removeHistory(HistoryEntry target) {
        if (target == null) return false;

        boolean removed = HISTORY.removeIf(entry ->
                entry.type().equalsIgnoreCase(target.type())
                        && entry.username().equalsIgnoreCase(target.username())
                        && Objects.equals(entry.uuid(), target.uuid())
        );

        if (removed) {
            AuthCredentialStore.remove(target.uuid(), target.username());
        }

        return removed;
    }

    public static boolean removeHistory(String type, String username, String uuid) {
        String cleanType = type == null ? "Unknown" : type.trim();
        String cleanName = username == null ? "Unknown" : username.trim();
        String cleanUuid = uuid == null ? "" : uuid.trim();

        boolean removed = HISTORY.removeIf(entry ->
                entry.type().equalsIgnoreCase(cleanType)
                        && entry.username().equalsIgnoreCase(cleanName)
                        && Objects.equals(entry.uuid(), cleanUuid)
        );

        if (removed) {
            AuthCredentialStore.remove(cleanUuid, cleanName);
        }

        return removed;
    }

    public static void clearHistory() {
        HISTORY.clear();
    }
    public static List<HistoryEntry> getHistory() {
        return Collections.unmodifiableList(HISTORY);
    }

    public static void addHistory(String type, String username, UUID uuid) {
        String cleanType = type == null ? "Unknown" : type.trim();
        String cleanName = username == null ? "Unknown" : username.trim();
        String cleanUuid = uuid == null ? "" : uuid.toString();

        HISTORY.removeIf(entry ->
                entry.type().equalsIgnoreCase(cleanType)
                        && entry.username().equalsIgnoreCase(cleanName)
                        && Objects.equals(entry.uuid(), cleanUuid)
        );

        HISTORY.add(0, new HistoryEntry(cleanType, cleanName, cleanUuid, System.currentTimeMillis()));

        while (HISTORY.size() > MAX_HISTORY) {
            HISTORY.remove(HISTORY.size() - 1);
        }
    }

    public static JsonObject saveToJson() {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();

        for (HistoryEntry entry : HISTORY) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", entry.type());
            obj.addProperty("username", entry.username());
            obj.addProperty("uuid", entry.uuid());
            obj.addProperty("lastLoginTime", entry.lastLoginTime());
            array.add(obj);
        }

        root.add("history", array);
        return root;
    }

    public static void loadFromJson(JsonObject root) {
        HISTORY.clear();

        if (root == null || !root.has("history") || !root.get("history").isJsonArray()) {
            return;
        }

        JsonArray array = root.getAsJsonArray("history");
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;

            JsonObject obj = element.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "Unknown";
            String username = obj.has("username") ? obj.get("username").getAsString() : "Unknown";
            String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : "";
            long lastLoginTime = obj.has("lastLoginTime") ? obj.get("lastLoginTime").getAsLong() : 0L;

            HISTORY.add(new HistoryEntry(type, username, uuid, lastLoginTime));
        }

        HISTORY.sort(Comparator.comparingLong(HistoryEntry::lastLoginTime).reversed());

        while (HISTORY.size() > MAX_HISTORY) {
            HISTORY.remove(HISTORY.size() - 1);
        }
    }
}