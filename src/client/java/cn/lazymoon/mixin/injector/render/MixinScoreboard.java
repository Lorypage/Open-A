package cn.lazymoon.mixin.injector.render;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.impl.exploit.Disabler;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public abstract class MixinScoreboard {

    @Shadow @Final private Object2ObjectMap<String, Team> teamsByScoreHolder;
    @Shadow public abstract @Nullable Team getScoreHolderTeam(String scoreHolderName);

    @Inject(method = "removeScoreHolderFromTeam", at = @At("HEAD"), cancellable = true)
    public void removeScoreHolderFromTeam(String scoreHolderName, Team team, CallbackInfo ci) {
        var protocol = Client.INSTANCE.getModuleManager().getModule(Disabler.class);
        if (protocol.isState() && Disabler.heypixel.isEnabled("Health Fixed")) {
            if (this.getScoreHolderTeam(scoreHolderName) != team) {
            } else {
                this.teamsByScoreHolder.remove(scoreHolderName);
                team.getPlayerList().remove(scoreHolderName);
            }
            ci.cancel();
        }
    }

}
