package cn.lazymoon.ingameui.clickgui.panel;

import cn.lazymoon.Client;
import cn.lazymoon.features.module.Category;
import cn.lazymoon.ingameui.clickgui.component.ModuleComponent;
import cn.lazymoon.ingameui.clickgui.utils.Component;
import cn.lazymoon.ingameui.clickgui.utils.IComponent;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.DecelerateAnimation;
import cn.lazymoon.utils.animations.impl.SmoothStepAnimation;
import cn.lazymoon.utils.math.MathUtils;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import cn.lazymoon.features.module.Module;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.opengl.GL11;

@Getter
@Setter
public class CategoryPanel implements IComponent {
    private float x, y;
    private float maxScroll = Float.MAX_VALUE, rawScroll, scroll;
    private Animation scrollAnimation = new SmoothStepAnimation(0, 0, Direction.BACKWARDS);
    public final Category category;
    private boolean selected;
    private final Animation animation = new DecelerateAnimation(250, 1);
    private final ObjectArrayList<ModuleComponent> moduleComponents = new ObjectArrayList<>();

    public CategoryPanel(Category category) {
        this.category = category;

        for (Module module : Client.INSTANCE.getModuleManager().getAllModules()){
            if (module.getCategory().equals(this.category)){
                moduleComponents.add(new ModuleComponent(module));
                System.out.println(moduleComponents);
            }
        }
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        x = Client.INSTANCE.getPanelClickGui().getX();
        y = Client.INSTANCE.getPanelClickGui().getY();
        animation.setDirection(selected ? Direction.FORWARDS : Direction.BACKWARDS);

        if (isSelected()) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);

            float col0Offset = 0, col1Offset = 0;
            maxScroll = 0;
            RenderHelper.scissorStart(x, y + 44,
                    Client.INSTANCE.getPanelClickGui().getW(),
                    Client.INSTANCE.getPanelClickGui().getH() - 44);
            for (int i = 0; i < moduleComponents.size(); i++) {
                ModuleComponent module = moduleComponents.get(i);
                int column = i % 2;
                float componentOffset = getComponentOffset(i, column, col0Offset, col1Offset);

                module.render(guiGraphics, mouseX, mouseY, partialTicks);

                double scroll = getScroll();
                module.setScroll((int) MathUtils.roundToHalf(scroll));

                maxScroll = Math.max(maxScroll, module.getMaxScroll());

                switch (column) {
                    case 0:
                        col0Offset += 50 + componentOffset;
                        break;
                    case 1:
                        col1Offset += 50 + componentOffset;
                        break;
                }
            }
            RenderHelper.scissorEnd();

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
        IComponent.super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private float getComponentOffset(int i, int column, float col0Offset, float col1Offset) {
        ModuleComponent component = moduleComponents.get(i);
        component.setColumn(column);

        int baseX = (int) getX();

        if (column == 0) {
            component.setX(baseX + 162);
        } else {
            component.setX(baseX + 162 + 168);
        }

        component.setHeight(50);

        float currentColOffset = (column == 0) ? col0Offset : col1Offset;
        component.setY(y + 48 + currentColOffset + scroll);

        float componentOffset = 0;
        for (Component component2 : component.getComponents()) {
            if (component2.isVisible())
                componentOffset += component2.getHeight();
        }
        component.setHeight(component.getHeight() + componentOffset);
        return componentOffset;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isSelected() && RenderUtils.isHovering(x + 150, y + 44,
                Client.INSTANCE.getPanelClickGui().getW() - 150,
                Client.INSTANCE.getPanelClickGui().getH() - 44, mouseX, mouseY)) {
            rawScroll += (float) (verticalAmount * 40);
            rawScroll = Math.max(Math.min(0, rawScroll), -maxScroll);
            scrollAnimation = new SmoothStepAnimation(250, rawScroll - scroll, Direction.BACKWARDS);
        }
        return IComponent.super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public float getScroll() {
        scroll = (float) (rawScroll - scrollAnimation.getOutput());
        return scroll;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (isSelected()) {
            moduleComponents.forEach(moduleComponent -> moduleComponent.mouseClicked(mouseX,mouseY,mouseButton));
        }
        return IComponent.super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if (isSelected()) {
            moduleComponents.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX,mouseY,state));
        }
        return IComponent.super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isSelected()) {
            moduleComponents.forEach(moduleComponent -> moduleComponent.keyPressed(keyCode, scanCode, modifiers));
        }
        return IComponent.super.keyPressed(keyCode, scanCode, modifiers);
    }
}