package cn.lazymoon.utils.client;

import cn.lazymoon.Client;
import cn.lazymoon.utils.InstanceAccess;
import cn.lazymoon.utils.player.PlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
public class ClientUtils implements InstanceAccess {
    public static void addMessage(String message) {
        if (mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(Text.of(message));
        } else {
            printToLog(message);
        }
    }

    public static boolean isInLobby() {
        if (mc.world == null) return true;
        Iterable<Entity> entities = mc.world.getEntities();
        for (Entity entity : entities) {
            if (entity != null && entity.getName().getString().contains("§e§lCLICK TO PLAY")) {
                return true;
            }
        }
        return mc.player.getInventory().getStack(8) != null && mc.player.getInventory().getStack(8).getItem() == Items.NETHER_STAR && mc.player.getInventory().getStack(0) != null && mc.player.getInventory().getStack(0).getItem() == Items.COMPASS;
    }

    public static void displayChat(String message) {
        if (mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(Text.of("§7[§b" + Client.getClientName() + "§7]" + Formatting.RESET + " " + message));
        } else {
            printToLog(message);
        }
    }


    public static void displayChat(String message,boolean bypassNameProtect) {
        if (mc.inGameHud != null) {
            if (!bypassNameProtect)mc.inGameHud.getChatHud().addMessage(Text.of("§7[§b" + Client.getClientName() + "§7]" + Formatting.RESET + " " + message));
            else {
                MutableText msg = Text.literal("§7[§b" + Client.getClientName() + "§7]" + Formatting.RESET + " ")
                        .append(Text.literal(message)
                                .setStyle(Style.EMPTY.withInsertion("np-bypass")));

                mc.inGameHud.getChatHud().addMessage(msg);
            }
        } else {
            printToLog(message);
        }
    }

    public static void debug(String message) {
        if (mc.inGameHud != null) {
            mc.inGameHud.getChatHud().addMessage(Text.of("§7[§b" +"A" + "§7] " + message));
        } else {
            printToLog(message);
        }
    }

    public static void printToLog(String msg) {
        Client.logger.info("[{}] {}", Client.getClientName(), msg);
    }

    public static void send(String message) {
        if (mc.player != null) {
            mc.player.networkHandler.sendChatMessage(message);
        }
    }

    public static boolean isInLobbyOrSpectator() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = Objects.requireNonNull(mc.player).getInventory().getStack(i);
            if (stack != null && stack.getItem() instanceof BedItem) {
                return true;
            }
        }
        return mc.player != null && (PlayerUtils.findSlot(Items.NETHER_STAR) != -1
                || PlayerUtils.findSlot(Items.PAPER) != -1);
    }

    public static boolean isNull() {
        return mc.player == null || mc.world == null;
    }

    public static String formatTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return dateTime.format(formatter);
    }
}
