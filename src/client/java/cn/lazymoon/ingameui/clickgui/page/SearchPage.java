package cn.lazymoon.ingameui.clickgui.page;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.Module;
import cn.lazymoon.ingameui.clickgui.PanelClickGui;
import cn.lazymoon.ingameui.clickgui.component.ModuleComponent;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.SmoothStepAnimation;
import cn.lazymoon.utils.math.MathUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SearchPage implements IGuiPage {
    private final PanelClickGui parent;
    private final ObjectArrayList<ModuleComponent> allComponents = new ObjectArrayList<>();

    private float maxScroll = Float.MAX_VALUE, rawScroll, scroll;
    private Animation scrollAnimation = new SmoothStepAnimation(0, 0, Direction.BACKWARDS);

    public SearchPage(PanelClickGui parent) {
        this.parent = parent;
        for (Module module : Client.INSTANCE.getModuleManager().getAllModules()) {
            allComponents.add(new ModuleComponent(module));
        }
    }

    public List<ModuleComponent> getFiltered() {
        String query = parent.getSearchText() == null ? "" : parent.getSearchText().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return allComponents;
        }

        return allComponents.stream().filter(component -> {
            Module module = component.getModule();
            return module.getName().toLowerCase(Locale.ROOT).contains(query)
                    || module.getDescription().toLowerCase(Locale.ROOT).contains(query)
                    || module.getCategory().name().toLowerCase(Locale.ROOT).contains(query);
        }).collect(Collectors.toList());
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        float x = parent.getX();
        float y = parent.getY();

        float contentX = x + 162;
        float contentY = y + 48;
        float contentW = parent.getW() - 174;
        float contentH = parent.getH() - 60;

        RenderHelper.drawRoundRectBloom(contentX, contentY, contentW, contentH, new Color(255, 255, 255, 10), 12);
        RenderHelper.drawRoundedRect(contentX, contentY, contentW, contentH, new Color(255, 255, 255, 40), 12);

        FontManager.semibold.drawGlowString(18, "Search", contentX + 12, contentY + 10,
                Theme.getCurrentTheme().getColors().first,
                Theme.getCurrentTheme().getColors().first, false, 1);

        String info = parent.getSearchText().isEmpty()
                ? "Type module name / description / category"
                : "Results: " + getFiltered().size();
        FontManager.semibold.drawString(13, info, contentX + 12, contentY + 27, new Color(255, 255, 255, 120), false);

        RenderHelper.scissorStart(contentX, contentY + 38, contentW, contentH - 42);

        float col0Offset = 0, col1Offset = 0;
        maxScroll = 0;

        List<ModuleComponent> filtered = getFiltered();

        for (int i = 0; i < filtered.size(); i++) {
            ModuleComponent module = filtered.get(i);
            int column = i % 2;

            float componentOffset = getComponentOffset(module, column, col0Offset, col1Offset);
            module.render(guiGraphics, mouseX, mouseY, partialTicks);

            double scroll = getScroll();
            module.setScroll((int) MathUtils.roundToHalf(scroll));
            maxScroll = Math.max(maxScroll, module.getMaxScroll());

            if (column == 0) {
                col0Offset += 50 + componentOffset;
            } else {
                col1Offset += 50 + componentOffset;
            }
        }

        if (filtered.isEmpty()) {
            FontManager.semibold.drawGlowString(16, "No modules found", contentX + contentW / 2f - 48, contentY + 75,
                    new Color(255, 255, 255, 180),
                    new Color(255, 255, 255, 180), false, 1);
        }

        RenderHelper.scissorEnd();
    }

    private float getComponentOffset(ModuleComponent component, int column, float col0Offset, float col1Offset) {
        component.setColumn(column);

        if (column == 0) {
            component.setX(parent.getX() + 162);
        } else {
            component.setX(parent.getX() + 162 + 168);
        }

        component.setHeight(50);

        float currentColOffset = column == 0 ? col0Offset : col1Offset;
        component.setY(parent.getY() + 48 + 38 + currentColOffset + scroll);

        float componentOffset = 0;
        for (cn.lazymoon.ingameui.clickgui.utils.Component component2 : component.getComponents()) {
            if (component2.isVisible()) {
                componentOffset += component2.getHeight();
            }
        }
        component.setHeight(component.getHeight() + componentOffset);
        return componentOffset;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (ModuleComponent component : getFiltered()) {
            component.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        for (ModuleComponent component : getFiltered()) {
            component.mouseReleased(mouseX, mouseY, state);
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (ModuleComponent component : getFiltered()) {
            component.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float contentX = parent.getX() + 150;
        float contentY = parent.getY() + 44;
        float contentW = parent.getW() - 150;
        float contentH = parent.getH() - 44;

        if (RenderUtils.isHovering(contentX, contentY, contentW, contentH, mouseX, mouseY)) {
            rawScroll += (float) (verticalAmount * 40);
            rawScroll = Math.max(Math.min(0, rawScroll), -maxScroll);
            scrollAnimation = new SmoothStepAnimation(250, rawScroll - scroll, Direction.BACKWARDS);
        }
        return false;
    }

    public float getScroll() {
        scroll = (float) (rawScroll - scrollAnimation.getOutput());
        return scroll;
    }

    public void resetScroll() {
        rawScroll = 0;
        scroll = 0;
        maxScroll = 0;
    }
}