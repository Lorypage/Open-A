package cn.lazymoon.features.module.impl.render;

import cn.lazymoon.Client;
import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.features.annotations.ModuleInfo;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.features.value.impl.BoolValue;
import cn.lazymoon.features.value.impl.ModeValue;
import cn.lazymoon.ingameui.ui.ModuleWidget;
import cn.lazymoon.sxmurxy.builders.Builder;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.instance.BlurTaskInstance;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltBlur;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.color.ColorPanel;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.font.FontManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.Comparator;

import static cn.lazymoon.utils.render.RenderHelper.context;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVG.nvgScale;
import static org.lwjgl.nanovg.NanoVG.nvgTranslate;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-27
 */
@Getter
@Setter
@ModuleInfo(name = "Scoreboard",description = "Shou you Scoreboard on you game screen",key = 0, category = Category.Render, hidden = false)
public class Scoreboard extends ModuleWidget {
    private final BoolValue showScore = new BoolValue("Show Score", false);
    private ScoreboardObjective objective;

    private record SidebarRow(Text name, Text score, int scoreWidth) {}

    @Override
    public void render(RenderNvgEvent event) {
        float posY = (float) (renderY);
        float posX = (float) (renderX);
        DrawContext drawContext = event.drawContext();
        if (mc.player != null && mc.world != null) {
            if (this.objective != null) {
                final NumberFormat numberFormat = this.objective.getNumberFormatOr(StyledNumberFormat.RED);

                // 收集并排好序的侧边栏行
                final SidebarRow[] sidebarEntries = this.objective.getScoreboard().getScoreboardEntries(objective)
                        .stream()
                        .filter(entry -> !entry.hidden())
                        .sorted(Comparator.comparing(ScoreboardEntry::value).reversed()
                                .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER))
                        .limit(15L)
                        .map(entry -> {
                            final Team team = this.objective.getScoreboard().getScoreHolderTeam(entry.owner());
                            final Text rawName = entry.name();
                            final Text decoratedName = Team.decorateName(team, rawName);
                            final Text formattedScore = entry.formatted(numberFormat);
                            final int scoreTextWidth = (int) FontManager.semibold.getStringWidth(formattedScore.getString(),18);
                            return new SidebarRow(decoratedName, formattedScore, scoreTextWidth);
                        })
                        .toArray(SidebarRow[]::new);


                // 标题与宽度计算
                final Text title = this.objective.getDisplayName();
                final int titleWidth = (int) FontManager.semibold.getStringWidth(title.getString(), 18);
                final int colonWidth = (int) FontManager.semibold.getStringWidth(": ", 18);
                int maxRowWidth = titleWidth;

                for (SidebarRow row : sidebarEntries) {
                    final int nameWidth = (int) FontManager.semibold.getStringWidth(row.name.getString(), 18);
                    final int rowWidth = row.scoreWidth > 0 ? nameWidth + colonWidth + row.scoreWidth : nameWidth;
                    maxRowWidth = Math.max(maxRowWidth, rowWidth);
                }

                // 布局参数
                final int rowCount = sidebarEntries.length;
                final int rowHeight = 9;
                final int totalRowsHeight = rowCount * rowHeight;
                final int centerY = (int) (posY + totalRowsHeight + rowHeight);
                final int leftX = (int) posX + 2;
                final int rightX = leftX + maxRowWidth;
                final int contentTopY = (int) posY;
                float smoothness = 5f;

                if (Client.INSTANCE.getModuleManager().getModule(PostProcessing.class).isState()) {
                    BuiltBlur blur = Builder.blur()
                            .size(new SizeState(maxRowWidth + 4 + 12 + smoothness, (rowCount + 1) * 9 + 4 + 12 + smoothness))
                            .radius(new QuadRadiusState(15))
                            .blurRadius(20)
                            .smoothness(smoothness)
                            .color(QuadColorState.TRANSPARENT)
                            .position(new PositionState(leftX - 2 - 6 - smoothness / 2, contentTopY - 2 - 6 - smoothness / 2))
                            .matrix4f(event.matrix4f())
                            .build();
                    BlurTaskInstance.addTask(blur);
                }

                if (PostProcessing.bloom.get()) {
                    RenderHelper.drawRoundRectBloomApple(leftX - 2 - 6, contentTopY - 2 - 6, maxRowWidth + 4 + 12, (rowCount + 1) * 9 + 4 + 12, 15, 2, new Color(255, 255, 255, 10));
                }
                RenderHelper.drawGradientAppleRoundedRectLR(leftX - 2 - 6, contentTopY - 2 - 6, maxRowWidth + 4 + 12, (rowCount + 1) * 9 + 4 + 12, new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),15);

                // 标题居中绘制
                    FontManager.semibold.drawString(18, title.getString(), leftX + (float) maxRowWidth / 2 - (float) titleWidth / 2, contentTopY + 1, new Color(255,255,255),false);


                // 各行绘制
                for (int index = 0; index < rowCount; index++) {
                    final SidebarRow row = sidebarEntries[index];
                    final int rowY = centerY - (rowCount - index) * rowHeight + 1;

                        FontManager.semibold.drawString(18, row.name.getString(), leftX, rowY, new Color(255,255,255,150),false);

                    if (this.showScore.getValue()) {
                            FontManager.semibold.drawString(18, row.score.getString(), rightX - row.scoreWidth, rowY, new Color(255,255,255,150),false);
                    }
                }

                if (mc.currentScreen instanceof ChatScreen) {
                    setWidth(maxRowWidth + 4);
                    setHeight((rowCount + 1) * 9 + 4);
                }

                this.objective = null;
            } else if (mc.currentScreen instanceof ChatScreen) {
                setWidth(80);
                setHeight(120);

                if (Client.INSTANCE.getModuleManager().getModule(PostProcessing.class).isState()) {
                    float smoothness = 5f;
                    BuiltBlur blur = Builder.blur()
                            .size(new SizeState(getWidth() + 12 + smoothness, getHeight() + 12 + smoothness))
                            .radius(new QuadRadiusState(15))
                            .blurRadius(20)
                            .smoothness(smoothness)
                            .color(QuadColorState.TRANSPARENT)
                            .position(new PositionState(posX - 6 - smoothness / 2, posY - 6 - smoothness / 2))
                            .matrix4f(event.matrix4f())
                            .build();
                    BlurTaskInstance.addTask(blur);
                }

                if (PostProcessing.bloom.get()) {
                    RenderHelper.drawRoundRectBloomApple(posX - 6, posY - 6, getWidth() + 12, getHeight() + 12, 15, 2, new Color(255, 255, 255, 10));
                }
                RenderHelper.drawGradientAppleRoundedRectLR(posX - 6, posY - 6, getWidth() + 12,getHeight() + 12, new Color(255, 255, 255, 40), new Color(255, 255, 255, 10),15);
                    FontManager.semibold.drawString(18, "isEmpty", (posX + getWidth() / 2 - FontManager.semibold.getStringWidth("isEmpty", 18) / 2f), (int) (posY + getHeight() / 2 - 20 / 2f), Theme.getCurrentTheme().getColors().first, false);
            }
        }
    }

    @Override
    public boolean shouldRender() {
        return isState();
    }
}