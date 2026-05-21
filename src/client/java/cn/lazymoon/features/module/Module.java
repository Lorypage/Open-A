package cn.lazymoon.features.module;

import cn.lazymoon.Client;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.value.Value;
import cn.lazymoon.ingameui.clickgui.PanelClickGui;
import cn.lazymoon.ingameui.notification.NotificationManager;
import cn.lazymoon.ingameui.notification.NotificationType;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.animations.impl.DecelerateAnimation;
import cn.lazymoon.utils.interfaces.Toggleable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@Getter
@Setter
public class Module implements InstanceAccess, Toggleable {
    /** Module Field */

    //String
    public String name;
    public String description;
    public String suffix = "";
    //Category(enum) cn.lazymoon.features.module.Category
    public Category category;
    //Int
    public int key;
    //Boolean
    public boolean state;
    public boolean hidden;

    /**
     * List
     * value list
     */
    public List<Value<?>> values;

    private final DecelerateAnimation animation = new DecelerateAnimation(200, 1);
    private final DecelerateAnimation animation2 = new DecelerateAnimation(200, 1);

    private long lastSecurityCheck = 0;
    private static final long SECURITY_CHECK_COOLDOWN = 3000; // 3秒冷却

    /** Constructor */
    public Module() {
        ModuleInfo moduleInfo = this.getClass().getAnnotation(ModuleInfo.class);
        Objects.requireNonNull(moduleInfo, "ModuleInfo annotation is missing on " + getClass().getName());

        this.name = moduleInfo.name();
        this.description = moduleInfo.description();

        this.key = moduleInfo.key();

        this.category = moduleInfo.category();

        this.hidden = moduleInfo.hidden();

        this.values = new ArrayList<>();
    }

    /**
     * Toggles the module's state.
     */
    @Override
    public void toggle() {
        this.setState(!this.state);
    }

    /**
     * Method called when the module is state.
     */
    @Override
    public void onEnable() {
    }
    /**
     * Method called when the module is disabled.
     */
    @Override
    public void onDisable() {}

    /**
     * Sets the module's state.
     *
     * @param state true to enable, false to disable.
     */
    public void setState(boolean state) {
        if (mc.currentScreen instanceof ChatScreen) return;
        if (this.state != state) {
            this.state = state;

            if (this.state) {
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.playSound(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, 1f, 1f);
                }

                Client.INSTANCE.getEventManager().register(this);
                this.onEnable();

                if (!name.equalsIgnoreCase("ClickGui"))
                    NotificationManager.post(NotificationType.SUCCESS,"Module Toggle", name + " Enabled");
            } else {
                if (MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.playSound(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_OFF, 1f, 1f);
                }

                Client.INSTANCE.getEventManager().unregister(this);
                this.onDisable();

                if (!name.equalsIgnoreCase("ClickGui"))
                    NotificationManager.post(NotificationType.FAILED, "Module Toggle",name + " Disabled");
            }
        }
    }

    public void setSuffix(String tag) {
        if (tag != null && !tag.isEmpty()) {
            String tagStyle = Optional.ofNullable(Client.INSTANCE.getModuleManager().getModule(cn.lazymoon.features.module.impl.render.ArrayList.class)).map(m -> m.tags.get()).orElse("").toLowerCase();
            switch (tagStyle) {
                case "simple":
                    suffix = " " + tag;
                    break;
                case "dash":
                    suffix = " - " + tag;
                    break;
                case "bracket":
                    suffix = " [" + tag + "]";
                    break;
                default:
                    suffix = "";
            }
        } else {
            suffix = "";
        }
    }
}
