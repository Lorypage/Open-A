package cn.lazymoon.ingameui.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.session.Session;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AccountManagerOfflineAccount {

    public static final String TYPE = "arcane:offline_v1";
    private static final String CLIENT_ID = "54fd49e4-2103-4044-9603-2b028c814ec3";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final String name;
    private final UUID uuid;
    private final UUID skin;

    public AccountManagerOfflineAccount(String name, UUID skin) {
        String clean = normalizeName(name);
        this.name = clean;
        this.uuid = uuid(clean);
        this.skin = skin;
    }

    public String type() {
        return TYPE;
    }

    public String typeTipKey() {
        return "arcane.accounts.tip.type.offline";
    }

    public String name() {
        return name;
    }

    public UUID uuid() {
        return uuid;
    }

    public UUID skin() {
        return skin != null ? skin : uuid;
    }

    public boolean canLogin() {
        return false;
    }

    public boolean insecure() {
        return false;
    }

    public Session toSession() {
        return createSession(this.name, this.uuid, "offline-token");
    }

    public AuthResult login() {
        return loginOffline(this.name);
    }

    // =========================
    // Login entry points
    // =========================

    public static AuthResult loginOffline(String name) {
        try {
            String clean = normalizeName(name);
            UUID uuid = uuid(clean);
            Session session = createSession(clean, uuid, "offline-token");
            return AuthResult.success("Offline login successful", clean, uuid, null, null, session);
        } catch (Throwable t) {
            return AuthResult.failure("Offline login failed: " + rootMessage(t));
        }
    }

    public static CompletableFuture<AuthResult> loginWithSessionToken(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String clean = token == null ? "" : token.trim();
                if (clean.isBlank()) {
                    return AuthResult.failure("Session token is empty");
                }

                MinecraftProfile profile = mcaToProfile(clean);
                Session session = createSession(profile.name, profile.uuid, clean);

                return AuthResult.success(
                        "Token login successful",
                        profile.name,
                        profile.uuid,
                        clean,
                        null,
                        session
                );
            } catch (Throwable t) {
                return AuthResult.failure("Token login failed: " + rootMessage(t));
            }
        });
    }
    /**
     * Requests a Microsoft device code and returns code / URL / future.
     * The UI can show the code and wait for the future to complete.
     */
    public static MicrosoftLoginStart loginMicrosoftStart() {
        try {
            DeviceCodeInfo code = requestDeviceCode();

            String url = code.verificationUriComplete != null && !code.verificationUriComplete.isBlank()
                    ? code.verificationUriComplete
                    : code.verificationUri;

            // 第一次登录：自动打开浏览器
            openBrowser(url);

            CompletableFuture<AuthResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    MsaTokens msa = pollDeviceCode(code);
                    MinecraftLoginResult mc = loginMinecraftWithMsa(msa.accessToken);
                    Session session = createSession(mc.name, mc.uuid, mc.minecraftAccessToken);

                    return AuthResult.success(
                            "Microsoft login successful",
                            mc.name,
                            mc.uuid,
                            msa.accessToken,
                            msa.refreshToken,
                            session
                    );
                } catch (Throwable t) {
                    return AuthResult.failure("Microsoft login failed: " + rootMessage(t));
                }
            });

            return new MicrosoftLoginStart(
                    code.userCode,
                    code.verificationUri,
                    code.verificationUriComplete,
                    future
            );
        } catch (Throwable t) {
            CompletableFuture<AuthResult> future = CompletableFuture.completedFuture(
                    AuthResult.failure("Microsoft login failed: " + rootMessage(t))
            );
            return new MicrosoftLoginStart(null, null, null, future);
        }
    }

    private static void openBrowser(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Browser URL is empty.");
        }

        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(java.net.URI.create(url));
                    return;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", url).start();
                return;
            }
            if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
                return;
            }
            new ProcessBuilder("xdg-open", url).start();
        } catch (Throwable ignored) {
            throw new IllegalStateException("Unable to open browser automatically: " + url);
        }
    }
    /**
     * Compatibility helper: directly returns the final Microsoft login result.
     */
    public static CompletableFuture<AuthResult> loginMicrosoft() {
        MicrosoftLoginStart start = loginMicrosoftStart();
        return start.future();
    }

    /**
     * Refresh token login: logs in directly using refresh_token.
     */
    public static CompletableFuture<AuthResult> loginWithRefreshToken(String refreshToken) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String clean = refreshToken == null ? "" : refreshToken.trim();
                if (clean.isBlank()) {
                    return AuthResult.failure("Refresh token is empty");
                }

                MsaTokens msa = exchangeRefreshToken(clean);
                MinecraftLoginResult mc = loginMinecraftWithMsa(msa.accessToken);
                Session session = createSession(mc.name, mc.uuid, mc.minecraftAccessToken);

                return AuthResult.success(
                        "Token login successful",
                        mc.name,
                        mc.uuid,
                        msa.accessToken,
                        msa.refreshToken,
                        session
                );
            } catch (Throwable t) {
                return AuthResult.failure("Token login failed: " + rootMessage(t));
            }
        });
    }

    // =========================
    // Validation / utilities
    // =========================

    public static UUID uuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static String warnKey(String name) {
        if (name == null || name.isBlank()) {
            return "arcane.offline.nick.blank";
        }

        int len = name.length();
        if (len < 3) {
            return "arcane.offline.nick.short";
        }
        if (len > 16) {
            return "arcane.offline.nick.long";
        }

        for (int i = 0; i < len; i++) {
            char c = name.charAt(i);
            if (c != '_' && !Character.isLetterOrDigit(c)) {
                return "arcane.offline.nick.chars";
            }
        }

        return null;
    }

    public static boolean isValidName(String name) {
        return warnKey(name) == null;
    }

    private static String normalizeName(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Offline account name is blank.");
        }
        return clean;
    }

    private static String rootMessage(Throwable t) {
        if (t == null) return "unknown";
        Throwable c = t;
        while (c.getCause() != null) {
            c = c.getCause();
        }
        String msg = c.getMessage();
        return (msg == null || msg.isBlank()) ? c.toString() : msg;
    }

    // =========================
    // Microsoft OAuth / Xbox / MC
    // =========================

    private static DeviceCodeInfo requestDeviceCode() throws IOException, InterruptedException {
        String payload = "client_id=" + CLIENT_ID
                + "&scope=" + URLEncoder.encode("XboxLive.signin XboxLive.offline_access", StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"))
                .header("User-Agent", "Arcane")
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Device code request failed: " + response.statusCode() + " / " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return DeviceCodeInfo.fromJson(json);
    }

    private static MsaTokens pollDeviceCode(DeviceCodeInfo code) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + code.expiresIn * 1000L;
        int interval = Math.max(1, code.interval);

        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(interval * 1000L);

            try {
                return exchangeDeviceCode(code.deviceCode);
            } catch (AuthorizationPendingException e) {
                continue;
            } catch (SlowDownException e) {
                interval++;
            }
        }

        throw new IllegalStateException("Device code expired. Please log in again.");
    }

    private static MsaTokens exchangeDeviceCode(String deviceCode) throws IOException, InterruptedException {
        String payload = "client_id=" + CLIENT_ID
                + "&grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:device_code", StandardCharsets.UTF_8)
                + "&device_code=" + URLEncoder.encode(deviceCode, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                .header("User-Agent", "Arcane")
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return MsaTokens.fromJson(json);
        }

        handleOAuthError(response.body());
        throw new IllegalStateException("Device code authorization failed: " + response.statusCode());
    }

    private static MsaTokens exchangeRefreshToken(String refreshToken) throws IOException, InterruptedException {
        String payload = "client_id=" + CLIENT_ID
                + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                + "&grant_type=refresh_token"
                + "&scope=" + URLEncoder.encode("XboxLive.signin XboxLive.offline_access", StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://login.live.com/oauth20_token.srf"))
                .header("User-Agent", "Arcane")
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return MsaTokens.fromJson(json);
        }

        throw new IllegalStateException("Refresh token exchange failed: " + response.statusCode() + " / " + response.body());
    }

    private static final class MinecraftLoginResult {
        private final UUID uuid;
        private final String name;
        private final String minecraftAccessToken;

        private MinecraftLoginResult(UUID uuid, String name, String minecraftAccessToken) {
            this.uuid = uuid;
            this.name = name;
            this.minecraftAccessToken = minecraftAccessToken;
        }
    }

    private static MinecraftLoginResult loginMinecraftWithMsa(String accessToken) throws IOException, InterruptedException {
        XblToken xbl = msaToXbl(accessToken);
        XstsToken xsts = xblToXsts(xbl.token, xbl.hash);
        String mcAccess = xstsToMca(xsts.token, xsts.hash);
        MinecraftProfile profile = mcaToProfile(mcAccess);
        return new MinecraftLoginResult(profile.uuid, profile.name, mcAccess);
    }

    private static XblToken msaToXbl(String accessToken) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + accessToken);
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "http://auth.xboxlive.com");
        body.addProperty("TokenType", "JWT");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                .header("User-Agent", "Arcane")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("MSA -> XBL failed: " + response.statusCode() + " / " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String token = json.get("Token").getAsString();
        String hash = json.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui")
                .get(0)
                .getAsJsonObject()
                .get("uhs")
                .getAsString();

        return new XblToken(token, hash);
    }

    private static XstsToken xblToXsts(String xblToken, String expectedHash) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        JsonObject properties = new JsonObject();
        properties.add("UserTokens", new com.google.gson.JsonArray());
        properties.getAsJsonArray("UserTokens").add(xblToken);
        properties.addProperty("SandboxId", "RETAIL");
        body.add("Properties", properties);
        body.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        body.addProperty("TokenType", "JWT");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .header("User-Agent", "Arcane")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            if (response.statusCode() == 401) {
                throw new IllegalStateException("XSTS authorization failed: account unavailable or restricted.");
            }
            throw new IllegalStateException("XBL -> XSTS failed: " + response.statusCode() + " / " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String token = json.get("Token").getAsString();
        String hash = json.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui")
                .get(0)
                .getAsJsonObject()
                .get("uhs")
                .getAsString();

        if (expectedHash != null && !expectedHash.equals(hash)) {
            throw new IllegalStateException("XBL/XSTS hash mismatch.");
        }

        return new XstsToken(token, hash);
    }

    private static String xstsToMca(String xstsToken, String hash) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("identityToken", "XBL3.0 x=" + hash + ";" + xstsToken);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                .header("User-Agent", "Arcane")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("XSTS -> MCA failed: " + response.statusCode() + " / " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        return json.get("access_token").getAsString();
    }

    private static MinecraftProfile mcaToProfile(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("User-Agent", "Arcane")
                .header("Authorization", "Bearer " + accessToken)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            throw new IllegalStateException("This Microsoft account does not own a Java Edition profile.");
        }
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to fetch MC profile: " + response.statusCode() + " / " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String name = json.get("name").getAsString();
        String raw = json.get("id").getAsString();

        UUID uuid = UUID.fromString(raw.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        ));

        return new MinecraftProfile(uuid, name);
    }

    private static void handleOAuthError(String body) {
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String error = json.has("error") ? json.get("error").getAsString() : "";
            if ("authorization_pending".equalsIgnoreCase(error)) {
                throw new AuthorizationPendingException();
            }
            if ("slow_down".equalsIgnoreCase(error)) {
                throw new SlowDownException();
            }
            if ("access_denied".equalsIgnoreCase(error)) {
                throw new IllegalStateException("The user canceled Microsoft login.");
            }
            if ("expired_token".equalsIgnoreCase(error)) {
                throw new IllegalStateException("The device code has expired.");
            }
            throw new IllegalStateException("OAuth error: " + body);
        } catch (AuthorizationPendingException | SlowDownException e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalStateException("OAuth response parse failed: " + body, t);
        }
    }

    // =========================
    // Session adapter
    // =========================

    private static Session createSession(String username, UUID uuid, String accessToken) {
        try {
            String uuidString = uuid.toString();

            for (Constructor<?> ctor : Session.class.getDeclaredConstructors()) {
                Class<?>[] p = ctor.getParameterTypes();

                // 1) String uuid + 6-arg version
                if (p.length == 6
                        && p[0] == String.class
                        && p[1] == String.class
                        && p[2] == String.class
                        && Optional.class.isAssignableFrom(p[3])
                        && Optional.class.isAssignableFrom(p[4])
                        && p[5].isEnum()) {
                    ctor.setAccessible(true);
                    Object accountType = pickAccountType(p[5]);
                    return (Session) ctor.newInstance(
                            username,
                            uuidString,
                            accessToken,
                            Optional.empty(),
                            Optional.empty(),
                            accountType
                    );
                }

                // 2) UUID uuid + 6-arg version
                if (p.length == 6
                        && p[0] == String.class
                        && p[1] == UUID.class
                        && p[2] == String.class
                        && Optional.class.isAssignableFrom(p[3])
                        && Optional.class.isAssignableFrom(p[4])
                        && p[5].isEnum()) {
                    ctor.setAccessible(true);
                    Object accountType = pickAccountType(p[5]);
                    return (Session) ctor.newInstance(
                            username,
                            uuid,
                            accessToken,
                            Optional.empty(),
                            Optional.empty(),
                            accountType
                    );
                }

                // 3) String uuid + 5-arg version
                if (p.length == 5
                        && p[0] == String.class
                        && p[1] == String.class
                        && p[2] == String.class
                        && Optional.class.isAssignableFrom(p[3])
                        && Optional.class.isAssignableFrom(p[4])) {
                    ctor.setAccessible(true);
                    return (Session) ctor.newInstance(
                            username,
                            uuidString,
                            accessToken,
                            Optional.empty(),
                            Optional.empty()
                    );
                }

                // 4) UUID uuid + 5-arg version
                if (p.length == 5
                        && p[0] == String.class
                        && p[1] == UUID.class
                        && p[2] == String.class
                        && Optional.class.isAssignableFrom(p[3])
                        && Optional.class.isAssignableFrom(p[4])) {
                    ctor.setAccessible(true);
                    return (Session) ctor.newInstance(
                            username,
                            uuid,
                            accessToken,
                            Optional.empty(),
                            Optional.empty()
                    );
                }
            }

            throw new IllegalStateException("No matching Session constructor found.");
        } catch (Throwable t) {
            System.out.println("createSession failed: " + t.getMessage());
            return null;
        }
    }

    private static Object pickAccountType(Class<?> enumType) {
        Object[] values = enumType.getEnumConstants();
        if (values == null || values.length == 0) {
            return null;
        }

        for (Object v : values) {
            String n = ((Enum<?>) v).name().toUpperCase();
            if (n.contains("MOJANG") || n.contains("MICROSOFT") || n.contains("MSA")) {
                return v;
            }
        }

        return values[0];
    }

    // =========================
    // Save / load
    // =========================

    public void write(DataOutput out) throws IOException {
        out.writeUTF(this.name);
        if (this.skin != null) {
            out.writeBoolean(true);
            out.writeLong(this.skin.getMostSignificantBits());
            out.writeLong(this.skin.getLeastSignificantBits());
        } else {
            out.writeBoolean(false);
        }
    }

    public static AccountManagerOfflineAccount readV1(DataInput in) throws IOException {
        String name = in.readUTF();
        return new AccountManagerOfflineAccount(name, null);
    }

    public static AccountManagerOfflineAccount readV2(DataInput in) throws IOException {
        String name = in.readUTF();
        UUID skin = in.readBoolean() ? new UUID(in.readLong(), in.readLong()) : null;
        return new AccountManagerOfflineAccount(name, skin);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AccountManagerOfflineAccount that)) return false;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.name);
    }

    @Override
    public String toString() {
        return "AccountManagerOfflineAccount{name='" + name + "', uuid=" + uuid + "}";
    }

    // =========================
    // DTO
    // =========================

    public static final class MicrosoftLoginStart {
        private final String userCode;
        private final String verificationUri;
        private final String verificationUriComplete;
        private final CompletableFuture<AuthResult> future;

        public MicrosoftLoginStart(
                String userCode,
                String verificationUri,
                String verificationUriComplete,
                CompletableFuture<AuthResult> future
        ) {
            this.userCode = userCode;
            this.verificationUri = verificationUri;
            this.verificationUriComplete = verificationUriComplete;
            this.future = future;
        }

        public String userCode() {
            return userCode;
        }

        public String verificationUri() {
            return verificationUri;
        }

        public String verificationUriComplete() {
            return verificationUriComplete;
        }

        public CompletableFuture<AuthResult> future() {
            return future;
        }
    }

    private static final class DeviceCodeInfo {
        private final String deviceCode;
        private final String userCode;
        private final String verificationUri;
        private final String verificationUriComplete;
        private final int interval;
        private final int expiresIn;

        private DeviceCodeInfo(
                String deviceCode,
                String userCode,
                String verificationUri,
                String verificationUriComplete,
                int interval,
                int expiresIn
        ) {
            this.deviceCode = deviceCode;
            this.userCode = userCode;
            this.verificationUri = verificationUri;
            this.verificationUriComplete = verificationUriComplete;
            this.interval = interval;
            this.expiresIn = expiresIn;
        }

        static DeviceCodeInfo fromJson(JsonObject json) {
            return new DeviceCodeInfo(
                    json.get("device_code").getAsString(),
                    json.has("user_code") ? json.get("user_code").getAsString() : "",
                    json.has("verification_uri") ? json.get("verification_uri").getAsString() : "",
                    json.has("verification_uri_complete") ? json.get("verification_uri_complete").getAsString() : null,
                    json.has("interval") ? json.get("interval").getAsInt() : 5,
                    json.has("expires_in") ? json.get("expires_in").getAsInt() : 900
            );
        }
    }

    private static final class MsaTokens {
        private final String accessToken;
        private final String refreshToken;

        private MsaTokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        static MsaTokens fromJson(JsonObject json) {
            String access = json.get("access_token").getAsString();
            String refresh = json.has("refresh_token") ? json.get("refresh_token").getAsString() : null;
            return new MsaTokens(access, refresh);
        }
    }

    private static final class XblToken {
        private final String token;
        private final String hash;

        private XblToken(String token, String hash) {
            this.token = token;
            this.hash = hash;
        }
    }

    private static final class XstsToken {
        private final String token;
        private final String hash;

        private XstsToken(String token, String hash) {
            this.token = token;
            this.hash = hash;
        }
    }

    private static final class MinecraftProfile {
        private final UUID uuid;
        private final String name;

        private MinecraftProfile(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    private static final class AuthorizationPendingException extends RuntimeException {
    }

    private static final class SlowDownException extends RuntimeException {
    }
}