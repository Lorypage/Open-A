package cn.lazymoon;

import cn.lazymoon.command.CommandManager;
import cn.lazymoon.component.blink.BlinkComponent;
import cn.lazymoon.component.rotation.RotationComponent;
import cn.lazymoon.config.ConfigManager;
import cn.lazymoon.event.api.EventManager;
import cn.lazymoon.features.module.ModuleManager;
import cn.lazymoon.features.module.impl.combat.utils.PacketLockUtils;
import cn.lazymoon.ingameui.auth.AuthCredentialStore;
import cn.lazymoon.ingameui.clickgui.PanelClickGui;
import cn.lazymoon.nanovg.NanoVG;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.client.ClientData;
import cn.lazymoon.utils.player.PlayerUtils;
import com.google.gson.Gson;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@Getter
public class Client implements InstanceAccess {
    /** Client Info */
    public static Client INSTANCE = new Client();

    public static final String Name = "Arcane";
    public static final String Versions = "S1.0";

    public static Logger logger = LogManager.getLogger(Name);
    //int field
    int startTime;

    //boolean field
    private static boolean nanoVGInit = false;

    /** Manager Info */
    private EventManager eventManager;

    private ModuleManager moduleManager;

    private ConfigManager configManager;

    private CommandManager commandManager;

    private RotationComponent rotationComponent;

    private PanelClickGui panelClickGui;

    private BlinkComponent blinkComponent;

    public void load() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        startTime = (int) System.currentTimeMillis();

        InitializingManagers();

        AuthCredentialStore.load();
    }
    public void shutdown(){
        configManager.saveAllConfig();
    }

    public void InitializingNanoVG() {
        if (!nanoVGInit) {
            NanoVG.INSTANCE.initNanoVG();
            nanoVGInit = true;
        }
    }

    public void InitializingManagers() {
        this.eventManager = new EventManager();

        this.moduleManager = new ModuleManager();
        this.moduleManager.Initialize();

        Theme.init();

        configManager = new ConfigManager();
        configManager.loadAllConfig();

        commandManager = new CommandManager();

        rotationComponent = new RotationComponent();

        panelClickGui = new PanelClickGui();

        this.blinkComponent = new BlinkComponent();

        eventManager.register(blinkComponent);
        eventManager.register(new ClientData());
        eventManager.register(new PlayerUtils());
        eventManager.register(new PacketLockUtils());
    }

    public static String getClientName(){
        return "A";
    }
}
