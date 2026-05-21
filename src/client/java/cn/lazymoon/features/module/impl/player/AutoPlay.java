package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.event.impl.world.SendCommandEvent;
import cn.lazymoon.event.impl.world.SendMessageEvent;
import cn.lazymoon.event.impl.world.WorldEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.impl.combat.KillAura;
import cn.lazymoon.features.module.impl.world.ContainerAura;
import cn.lazymoon.features.module.impl.world.ContainerStealer;
import cn.lazymoon.features.module.impl.world.Scaffold;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.ingameui.notification.NotificationManager;
import cn.lazymoon.ingameui.notification.NotificationType;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.time.TimerUtils;
import lombok.NonNull;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ModuleInfo(name = "AutoPlay",description = "Help you automatically join the game",key = 0, category = Category.Player,hidden = false)
public class AutoPlay extends cn.lazymoon.features.module.Module {

    public static BoolValue autolang = new BoolValue("AutoLang",true);
    public static NumberValue winDelay = new NumberValue("WinDelay",5000,0,10000,50);
    public static NumberValue loseDelay = new NumberValue("LoseDelay",0,0,10000,50);
    public static String playCommand = "";
    public static boolean startNextGame;
    public static boolean win;
    public static boolean lose;
    public static boolean canSend;
    public static TimerUtils timer = new TimerUtils();
    private boolean notified = false;

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.packet instanceof TitleS2CPacket(Text text)) {
            if (text == null) return;

            String title = text.getString();

            if (title.startsWith("§6§l") && (title.endsWith("!") || title.endsWith("！"))) {
                startNextGame = true;
                win = true;
                timer.reset();
            } else if (title.startsWith("§c§l") && (title.endsWith("!") || title.endsWith("！"))) {
                startNextGame = true;
                lose = true;
                timer.reset();
            }
        } else if (event.packet instanceof ClickSlotC2SPacket packet) {
            String itemName = packet.getStack().getName().getString();
            int itemID = Item.getRawId(packet.getStack().getItem());
            if (itemID == 1042) {
                if (itemName.contains("空岛战争") || itemName.contains("SkyWars")) {
                    if (itemName.contains("双人") || itemName.contains("Doubles")) {
                        if (itemName.contains("普通") || itemName.contains("Normal")) {
                            playCommand = "/play teams_normal";
                        } else if (itemName.contains("疯狂") || itemName.contains("Insane")) {
                            playCommand = "/play teams_insane";
                        }
                    } else if (itemName.contains("单挑") || itemName.contains("Solo")) {
                        if (itemName.contains("普通") || itemName.contains("Normal")) {
                            playCommand = "/play solo_normal";
                        } else if (itemName.contains("疯狂") || itemName.contains("Insane")) {
                            playCommand = "/play solo_insane";
                        }
                    }
                    canSend = true;
                }
            } else if (itemID == 1027) {
                if ((itemName.contains("起床战争") || itemName.contains("Bed Wars")) && !itemName.contains("Duel")) {
                    if (itemName.contains("4v4")) {
                        playCommand = "/play bedwars_four_four";
                    } else if (itemName.contains("3v3")) {
                        playCommand = "/play bedwars_four_three";
                    } else if (itemName.contains("双人模式") || itemName.contains("Doubles")) {
                        playCommand = "/play bedwars_eight_two";
                    } else if (itemName.contains("单挑") && !itemName.contains("决斗") || itemName.contains("Solo")) {
                        playCommand = "/play bedwars_eight_one";
                    } else if (itemName.contains("决斗") && itemName.contains("单挑") || itemName.contains("1v1")) {
                        playCommand = "/play bedwars_two_one_duels";
                    }
                    canSend = true;
                }
            } else if (itemID == 223) {
                if (itemName.contains("疾速起床决斗") || itemName.contains("Bed Rush")) {
                    if (itemName.contains("1v1") || itemName.contains("单挑")) {
                        playCommand = "/play bedwars_two_one_duels_rush";
                    }
                    canSend = true;
                }
            } else if (itemID == 959) {
                if (itemName.contains("Sumo Duel") || itemName.contains("相扑决斗")) {
                    playCommand = "/play duels_sumo_duel";
                    canSend = true;
                }
            } else if (itemID == 980) {
                if (itemName.contains("经典") || itemName.contains("Classic")) {
                    if (itemName.contains("决斗") || itemName.contains("Duel")) {
                        playCommand = "/play duels_classic_duel";
                    }
                    if (itemName.contains("双人") || itemName.contains("Doubles")) {
                        playCommand = "/play duels_classic_doubles";
                    }
                    canSend = true;
                }
            }
        }
    }

    @EventTarget
    public void onSendMessage(SendMessageEvent event) {
        if (event.message.startsWith("/play")) {
            playCommand = event.message;
        }
    }

    @EventTarget
    public void onSendCommand(SendCommandEvent event) {
        if (event.command.startsWith("play")) {
            playCommand = "/" + event.command;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (startNextGame && (win || lose)) {
            if (Client.INSTANCE.getModuleManager().getModule(AutoGG.class).isState() && AutoGG.spoken || !Client.INSTANCE.getModuleManager().getModule(AutoGG.class).isState()) {
                sendToGame(playCommand);
                startNextGame = false;
                win = false;
                lose = false;
            }
        }
    }

    private void sendToGame(String string) {
        float delay = (win) ? winDelay.get().floatValue() / 1000 : (lose) ? loseDelay.get().floatValue() / 1000 : 0;
            Client.INSTANCE.getModuleManager().getModule(KillAura.class).setState(false);
            Client.INSTANCE.getModuleManager().getModule(InvManager.class).setState(false);
            Client.INSTANCE.getModuleManager().getModule(ContainerStealer.class).setState(false);
            Client.INSTANCE.getModuleManager().getModule(ContainerAura.class).setState(false);
            Client.INSTANCE.getModuleManager().getModule(Scaffold.class).setState(false);
        if (!notified && win) {
            NotificationManager.post(NotificationType.SUCCESS,"AutoPlay", "A new game will begin" + (delay > 0 ? " in " + delay + "s" : "") + "!", delay);
            notified = true;
        }
        schedule(() -> {
            if (canSend)
                ClientUtils.send(string);
            else {

            }
        }, (long) delay, TimeUnit.SECONDS);
    }
    private static final ScheduledExecutorService RUNNABLE_POOL = Executors.newScheduledThreadPool(3, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "Multithreading Thread " + counter.incrementAndGet());
        }
    });

    public static ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit unit) {
        return RUNNABLE_POOL.schedule(r, delay, unit);
    }

    @EventTarget
    public void onWorld(WorldEvent e) {
        notified = false;
    }
}
