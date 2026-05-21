package cn.lazymoon.ingameui.clickgui.component;

import cn.lazymoon.Client;
import cn.lazymoon.features.value.Value;
import cn.lazymoon.features.value.impl.*;
import cn.lazymoon.ingameui.clickgui.utils.Component;
import cn.lazymoon.ingameui.clickgui.values.*;
import cn.lazymoon.theme.Theme;
import cn.lazymoon.utils.animations.Animation;
import cn.lazymoon.utils.animations.Direction;
import cn.lazymoon.utils.animations.impl.DecelerateAnimation;
import cn.lazymoon.utils.render.RenderHelper;
import cn.lazymoon.utils.render.RenderUtils;
import cn.lazymoon.utils.render.font.FontManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import cn.lazymoon.features.module.Module;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

import java.awt.*;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;

/**
 * @Author:Guyuemang
 * @Time:03-28
 */
@Getter
public class ModuleComponent extends Component {
    private final Module module;
    @Setter
    private int scroll = 0;
    @Setter
    private int column;
    private final ObjectArrayList<Component> components = new ObjectArrayList<>();
    private final Animation enabled = new DecelerateAnimation(250,1);
    private boolean binding = false;
    private static ModuleComponent currentlyBinding = null;

    public ModuleComponent(Module module) {
        this.module = module;
        for (Value value : module.getValues()) {
            if (value instanceof BoolValue boolValue) {
                components.add(new BooleanValueComponent(boolValue));
            }else if (value instanceof NumberValue numberValue){
                components.add(new NumberValueComponent(numberValue));
            }else if (value instanceof ModeValue modeValue) {
                components.add(new ModeValueComponent(modeValue));
            }else if (value instanceof MultiBoolValue multiBoolValue) {
                components.add(new MultiValueComponent(multiBoolValue));
            }else if (value instanceof ColorValue colorValue){
                components.add(new ColorValueComponent(colorValue));
            }
        }
        enabled.setDirection(module.isState() ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTicks) {
        enabled.setDirection(module.isState() ? Direction.FORWARDS : Direction.BACKWARDS);

        Animation hiddenAnimation = module.getAnimation2();
        hiddenAnimation.setDirection(module.isHidden() ? Direction.FORWARDS : Direction.BACKWARDS);

        RenderHelper.drawRoundedRect(getX(),getY() + 17,160,getHeight() - 19,new Color(255, 255, 255,10), 10);
        FontManager.semibold.drawGlowString(18, module.getName(), getX() + 3, getY() + 6,module.isState() ? Theme.getCurrentTheme().getColors().first : new Color(255, 255, 255), module.isState() ? Theme.getCurrentTheme().getColors().first : new Color(255, 255, 255), false,3);
        FontManager.semibold.drawGlowString(12, module.getDescription(), getX() + 6, getY() + 22, new Color(255, 255, 255,100), new Color(255, 255, 255,100), false,1);

        FontManager.semibold.drawString(14, "Enable", getX() + 6, getY() + 35, new Color(255,255,255), false);
        RenderHelper.drawRoundedRect(getX() + 32.5f, getY() + 31, 23, 13, module.isState() ? new Color(255, 255, 255,50) : new Color(255, 255, 255,20), 6);
        RenderHelper.drawCircle(getX() + 32.5f + 6.5f + enabled.getOutput().floatValue() * 10, getY() + 31 + 6.5f, 5, module.isState() ? new Color(255, 255, 255,50) : new Color(255, 255, 255,20));

        FontManager.semibold.drawString(14, "Hidden", getX() + 65, getY() + 35, new Color(255,255,255), false);
        RenderHelper.drawRoundedRect(getX() + 92.5f, getY() + 31, 23, 13, module.isHidden() ? new Color(255, 255, 255,50) : new Color(255, 255, 255,10), 6);
        RenderHelper.drawCircle(getX() + 92.5f + 6.5f + hiddenAnimation.getOutput().floatValue() * 10, getY() + 31 + 6.5f, 5, module.isHidden() ? new Color(255, 255, 255,50) : new Color(255, 255, 255,20));

        String keyText;
        if (binding) {
            keyText = "Bind";
        } else {
            int key = module.getKey();
            if (key == 0) {
                keyText = "None";
            } else {
                keyText = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
            }
        }
        float textWidth = FontManager.semibold.getStringWidth(keyText, 14);
        float bgWidth = textWidth + 5;
        float bgX = getX() + 160 - bgWidth - 6;
        float bgY = getY() + 31;
        RenderHelper.drawRoundedRect(bgX, bgY, bgWidth, 13, new Color(255, 255, 255,40), 6);
        FontManager.semibold.drawString(14, keyText, bgX + (bgWidth - textWidth) / 2, bgY + 3.5f, new Color(255, 255, 255), false);

        float componentY = getY() + 30;
        ObjectArrayList<Component> filtered = components.stream()
                .filter(Component::isVisible)
                .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll);
        for (Component component : filtered) {
            component.setX(getX());
            component.setY(componentY);
            component.render(guiGraphics,mouseX, mouseY,partialTicks);
            componentY += component.getHeight();
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    public int getMaxScroll() {
        return (int) (((getY() + Client.INSTANCE.getPanelClickGui().getH()) + getHeight()) * 2);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (binding && !isHoveringKeyArea(mouseX, mouseY)) {
            binding = false;
            currentlyBinding = null;
        }
        if (isHoveringKeyArea(mouseX, mouseY) && mouseButton == 0) {
            if (binding) {
                binding = false;
                currentlyBinding = null;
            } else {
                if (currentlyBinding != null) {
                    currentlyBinding.binding = false;
                }
                binding = true;
                currentlyBinding = this;
            }
            return true;
        }
        if (RenderUtils.isHovering(getX() + 32.5f, getY() + 31, 23, 13,mouseX,mouseY) && mouseButton == 0){
            module.toggle();
        }
        if (RenderUtils.isHovering(getX() + 92.5f, getY() + 31, 23, 13,mouseX,mouseY) && mouseButton == 0){
            module.setHidden(!module.hidden);
        }
        for (Component component : components) {
            component.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        for (Component component : components) {
            component.mouseReleased(mouseX, mouseY, state);
        }
        return super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW_KEY_ESCAPE) {
                binding = false;
                currentlyBinding = null;
                return true;
            }
            if (keyCode == GLFW_KEY_SPACE) {
                module.setKey(0);
            } else {
                module.setKey(keyCode);
            }
            binding = false;
            currentlyBinding = null;
            return true;
        }
        for (Component component : components) {
            component.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isHoveringKeyArea(double mouseX, double mouseY) {
        float textWidth = FontManager.semibold.getStringWidth(binding ? "Bind" : (module.getKey() == 0 ? "None" : InputUtil.fromKeyCode(module.getKey(), 0).getLocalizedText().getString()), 14);
        float bgWidth = textWidth + 5;
        float bgX = getX() + 160 - bgWidth - 6;
        float bgY = getY() + 31;

        return RenderUtils.isHovering(bgX, bgY, bgWidth, 13, mouseX, mouseY);
    }
}