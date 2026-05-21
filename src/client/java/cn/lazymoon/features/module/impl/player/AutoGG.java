package cn.lazymoon.features.module.impl.player;

import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.player.PacketEvent;
import cn.lazymoon.event.impl.player.UpdateEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.features.value.impl.NumberValue;
import cn.lazymoon.features.value.impl.StringValue;
import cn.lazymoon.utils.client.ClientUtils;
import cn.lazymoon.utils.time.TimerUtils;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.text.Text;

@ModuleInfo(name = "AutoGG",description = "Help you automatically end the game",key = 0, category = Category.Player,hidden = false)
public class AutoGG extends Module {

    public static NumberValue delay = new NumberValue("Delay",100,0,3000, 100);

    public static boolean spoken = false;
    public boolean needSpeakWinWords = false;
    public boolean needSpeakLoseWords = false;
    private final TimerUtils timer = new TimerUtils();

    @SuppressWarnings("unused")
    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (ClientUtils.isNull() || mc.getNetworkHandler() == null)
            return;

        if (needSpeakWinWords && timer.hasTimeElapsed(delay.get())) {
            mc.getNetworkHandler().sendChatMessage("/ac " + "Nobe");
            needSpeakWinWords = false;
            spoken = true;
        }

        if (needSpeakLoseWords && timer.hasTimeElapsed(delay.get())) {
            mc.getNetworkHandler().sendChatMessage("/ac " + "L");
            needSpeakLoseWords = false;
            spoken = true;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!(event.packet instanceof TitleS2CPacket(Text text))) return;
        if (text == null) return;

        String title = text.getString();

        if (title.startsWith("§6§l") && (title.endsWith("!") || title.endsWith("！"))) {
            needSpeakWinWords = true;
            timer.reset();
        } else if (title.startsWith("§c§l") && (title.endsWith("!") || title.endsWith("！"))) {
            needSpeakLoseWords = true;
            timer.reset();
        }
    }
}
