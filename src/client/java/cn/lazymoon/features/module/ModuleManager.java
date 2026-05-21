package cn.lazymoon.features.module;

import cn.lazymoon.Client;
import cn.lazymoon.event.api.EventTarget;
import cn.lazymoon.event.impl.input.KeyEvent;
import cn.lazymoon.event.impl.render.RenderNvgEvent;
import cn.lazymoon.features.annotations.AutoEnable;
import cn.lazymoon.features.module.impl.combat.*;
import cn.lazymoon.features.module.impl.exploit.*;
import cn.lazymoon.features.module.impl.movement.*;
import cn.lazymoon.features.module.impl.player.*;
import cn.lazymoon.features.module.impl.render.*;
import cn.lazymoon.features.module.impl.render.targethud.TargetHUD;
import cn.lazymoon.features.module.impl.visual.*;
import cn.lazymoon.features.module.impl.world.*;
import cn.lazymoon.features.value.Value;
import cn.lazymoon.ingameui.clickgui.PanelClickGui;
import cn.lazymoon.ingameui.ui.ModuleWidget;
import cn.lazymoon.utils.InstanceAccess;

import java.lang.reflect.Field;
import java.util.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
public class ModuleManager implements InstanceAccess {
    private final Map<Class<? extends Module>, Module> modules = new LinkedHashMap<>();

    /**
     * Initializes the ModuleManager by registering all available modules,
     * automatically enabling modules marked with autoEnabled annotation,
     * and registering event listeners.
     */
    public void Initialize(){
        // Register this ModuleManager instance to listen for events
        Client.INSTANCE.getEventManager().register(this);
        //Combat
        addModule(new AntiCrystal());
        addModule(new AntiFireball());
        addModule(new AntiKnockback());
        addModule(new AutoClicker());
        addModule(new MobAssist());
        addModule(new Lagrange());
        addModule(new AutoWeapon());
        addModule(new KillAura());
        addModule(new SuperKnockBack());
        addModule(new ThrowableAura());
        //Exploit
        addModule(new Disabler());
        addModule(new AntiBot());
        addModule(new GhostHand());
        addModule(new NameProtect());
        addModule(new NoDelay());
        //Movement
        addModule(new AntiVoid());
        addModule(new Eagle());
        addModule(new GuiMove());
        addModule(new NoFall());
        addModule(new NoSlow());
        addModule(new Sprint());
        //Player
        addModule(new AntiAFK());
        addModule(new AutoGG());
        addModule(new AutoPlay());
        addModule(new AutoTool());
        addModule(new Blink());
        addModule(new FastPlace());
        addModule(new InvManager());
        addModule(new MidPearl());
        addModule(new Teams());
        //Visual
        addModule(new Animations());
        addModule(new ContainerESP());
        addModule(new Camera());
        addModule(new AntiBlind());
        addModule(new BedPlates());
        addModule(new BedESP());
        addModule(new ESP());
        addModule(new OldHitting());
        //Render
        addModule(new ClickGui());
        addModule(new EffectCountdownHUD());
        addModule(new InterFace());
        addModule(new PostProcessing());
        addModule(new Scoreboard());
        addModule(new WaterMark());
        addModule(new Notification());
        addModule(new cn.lazymoon.features.module.impl.render.ArrayList());
        addModule(new TargetHUD());
        addModule(new EffectHUD());
        //World
        addModule(new ContainerAura());
        addModule(new ContainerStealer());
        addModule(new Scaffold());
        addModule(new BedBreaker());
        addModule(new PlayerReminder());
        addModule(new Eliminate());

        modules.values().stream().filter(module -> module.getClass().isAnnotationPresent(AutoEnable.class)).forEach(module -> module.setState(true));
    }

    /**
     * Register module
     * @param module module
     */
    public void addModule(Module module) {
        for (final Field field : module.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                final Object obj = field.get(module);
                if (obj instanceof Value<?>) module.getValues().add((Value<?>) obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        modules.put(module.getClass(), module);
    }

    /**
     * Checks if a module of the specified class is currently enabled.
     *
     * @param c the class of the module to check
     * @return true if the module exists and is enabled, false otherwise
     */
    public boolean isState(Class<? extends Module> c) {
        Module m = getModule(c);
        return m != null && m.isState();
    }

    /**
     * Retrieves a module instance by its class.
     *
     * @param cls the class of the module to retrieve
     * @param <T> the type of the module
     * @return the module instance, or null if not found
     */
    public <T extends Module> T getModule(Class<T> cls) {
        return cls.cast(modules.get(cls));
    }


    public Collection<Module> getAllModules() {
        return Collections.unmodifiableCollection(modules.values());
    }
    /**
     * Retrieves a module by its display name (case-insensitive).
     *
     * @param moduleName the display name of the module to find
     * @return the matching module, or null if not found
     */
    public Module getModule(String moduleName) {
        return modules.values().stream()
                .filter(module -> module.getName().equalsIgnoreCase(moduleName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves all modules that belong to a specific category.
     *
     * @param category The category to filter modules by.
     * @return A list of modules within the specified category.
     */
    public ArrayList<Module> getModulesInCategory(Category category) {
        ArrayList<Module> modulesInCategory = new ArrayList<>();
        for (Module module : modules.values()) {
            if (module.getCategory() == category) {
                modulesInCategory.add(module);
            }
        }
        return modulesInCategory;
    }

    @EventTarget
    private void onKeyPress(KeyEvent event) {
        if (mc.currentScreen instanceof PanelClickGui) return;
        modules.values().stream().filter(module -> module.getKey() == event.getKey() && event.getKey() != -1).forEach(Module::toggle);
    }

    public <T extends ModuleWidget> T getModuleW(Class<T> cls) {
        return cls.cast(modules.get(cls));
    }

    public Collection<ModuleWidget> getAllWidgets() {
        return modules.values().stream()
                .filter(ModuleWidget.class::isInstance)
                .map(ModuleWidget.class::cast)
                .collect(Collectors.toList());
    }

    @EventTarget
    public void onRender2D(RenderNvgEvent event) {
        for (Module module : modules.values()) {
            if (module instanceof ModuleWidget && module.isState()) {
                ModuleWidget widget = (ModuleWidget) module;
                if (widget.shouldRender()) {
                    widget.setDrawContext(event.drawContext());
                    widget.updatePos();
                    widget.render(event);
                }
            }
        }
    }
}
