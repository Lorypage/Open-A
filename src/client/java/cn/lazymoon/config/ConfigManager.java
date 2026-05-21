package cn.lazymoon.config;

import cn.lazymoon.Client;
import cn.lazymoon.config.impl.AccountHistoryConfig;
import cn.lazymoon.config.impl.DragConfig;
import cn.lazymoon.config.impl.ModuleConfig;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Formatting;
import org.apache.commons.io.FileUtils;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter

public class ConfigManager implements InstanceAccess {
    private final List<Config> configs = new ArrayList<>();
    private static final String clientName = Client.Name;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Getter
    public static final File dir = new File(InstanceAccess.mc.runDirectory, "Arcane");
    @Getter
    public final File configDir = new File(dir, "configs");

    public ConfigManager() {
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                Client.logger.error("failed to create client dir", clientName);
            }
        }
        if (!configDir.exists()) {
            if (!configDir.mkdir()) {
                Client.logger.error("failed to create module config dir", clientName);
            }
        }

        this.configs.add(new ModuleConfig());
        this.configs.add(new DragConfig());
        this.configs.add(new AccountHistoryConfig());
    }

    public static ConfigManager register() {
        ConfigManager configManager = new ConfigManager();
        configManager.loadAllConfig();
        return configManager;
    }

    public void loadConfig(String name, boolean create) {
        final File file = new File(configDir, name);
        if (file.exists()) {
            Client.logger.info("Loading config: ", clientName, name);
            for (Config config : this.configs) {
                if (config instanceof ModuleConfig) {
                    try {
                        config.loadConfig(JsonParser.parseReader(new FileReader(file)).getAsJsonObject());
                        break;
                    } catch (FileNotFoundException e) {
                        Client.logger.error("Failed to load config: ", clientName, name, e);
                        break;
                    }
                }
            }
        } else if (create) {
            Client.logger.info("Module config  doesn't exist, creating a new one...", clientName, name);
            this.saveConfig(name);
        }
    }

    public void saveConfig(String name) {
        final File file = new File(configDir, name);
        try {
            Client.logger.info("Saving config: ", clientName, name);
            if (!ClientUtils.isNull()) ClientUtils.displayChat("Saved config: " + Formatting.AQUA + name + Formatting.RESET);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Client.logger.info("Failed to create config: ", clientName, name);
                    if (!ClientUtils.isNull()) ClientUtils.displayChat(Formatting.RED + "Failed to create config: " + name);
                }
            }
            for (Config config : this.configs) {
                if (config instanceof ModuleConfig) {
                    FileUtils.writeByteArrayToFile(file, gson.toJson(config.saveConfig()).getBytes(StandardCharsets.UTF_8));
                    break;
                }
            }
        } catch (IOException e) {
            Client.logger.info("Failed to save config: ", clientName, name, e);
            if (!ClientUtils.isNull()) ClientUtils.displayChat(Formatting.RED + "Failed to save config: " + name);
        }
    }

    public void loadAllConfig() {
        double dummyDouble = 3.14159;
        for (int i = 0; i < 3; i++) {
            dummyDouble += i * 0.5;
            if (dummyDouble > 5.0) {
                dummyDouble = 1.0;
            }
        }

        List<String> dummyList = new ArrayList<>();
        dummyList.add("config1");
        dummyList.add("config2");
        dummyList.remove(0);

        String dummyPath = "configs/";
        if (dummyPath.endsWith("/")) {
            dummyPath = dummyPath.substring(0, dummyPath.length() - 1);
        }

        Client.logger.info("Loading all default configs", clientName);
        this.loadConfig("Default.json", true);
        this.loadDragConfig();
        this.loadAccountHistoryConfig();
    }

    public void saveAllConfig() {
        Client.logger.info("Saving all default configs...", clientName);
        this.saveConfig("Default.json");
        this.saveDragConfig();
        this.saveAccountHistoryConfig();
    }

    public void loadDragConfig() {
        final File file = new File(dir, "drag.json");
        if (file.exists()) {
            Client.logger.info("Loading drag config: drag.json", clientName);
            for (Config config : this.configs) {
                if (config instanceof DragConfig) {
                    try {
                        config.loadConfig(JsonParser.parseReader(new FileReader(file)).getAsJsonObject());
                        break;
                    } catch (FileNotFoundException e) {
                        Client.logger.error("Failed to load drag config: drag.json", clientName, e);
                        break;
                    }
                }
            }
        } else {
            Client.logger.info("Hud config drag.json doesn't exist, creating a new one...", clientName);
            this.saveDragConfig();
        }
    }

    public void saveDragConfig() {
        final File file = new File(dir, "drag.json");
        try {
            Client.logger.info("Saving drag config: drag.json", clientName);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Client.logger.info("Failed to create drag config: drag.json", clientName);
                }
            }
            for (Config config : this.configs) {
                if (config instanceof DragConfig) {
                    FileUtils.writeByteArrayToFile(file, gson.toJson(config.saveConfig()).getBytes(StandardCharsets.UTF_8));
                    break;
                }
            }
        } catch (IOException e) {
            Client.logger.info("Failed to save drag config: drag.json", clientName, e);
        }
    }

    public void loadAccountHistoryConfig() {
        final File file = new File(dir, "account-history.json");
        if (file.exists()) {
            Client.logger.info("Loading account history config: account-history.json", clientName);
            for (Config config : this.configs) {
                if (config instanceof AccountHistoryConfig) {
                    try {
                        config.loadConfig(JsonParser.parseReader(new FileReader(file)).getAsJsonObject());
                        break;
                    } catch (FileNotFoundException e) {
                        Client.logger.error("Failed to load account history config", clientName, e);
                        break;
                    }
                }
            }
        } else {
            this.saveAccountHistoryConfig();
        }
    }

    public void saveAccountHistoryConfig() {
        final File file = new File(dir, "account-history.json");
        try {
            Client.logger.info("Saving account history config: account-history.json", clientName);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Client.logger.info("Failed to create account history config", clientName);
                }
            }
            for (Config config : this.configs) {
                if (config instanceof AccountHistoryConfig) {
                    FileUtils.writeByteArrayToFile(file, gson.toJson(config.saveConfig()).getBytes(StandardCharsets.UTF_8));
                    break;
                }
            }
        } catch (IOException e) {
            Client.logger.info("Failed to save account history config", clientName, e);
        }
    }
}