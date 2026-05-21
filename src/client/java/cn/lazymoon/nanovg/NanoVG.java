package cn.lazymoon.nanovg;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.nanovg.NanoVGGL3;

import java.util.function.Consumer;

import static org.lwjgl.nanovg.NanoVG.nvgBeginFrame;
import static org.lwjgl.nanovg.NanoVG.nvgEndFrame;
import static org.lwjgl.nanovg.NanoVG.nvgRestore;
import static org.lwjgl.nanovg.NanoVG.nvgSave;
import static org.lwjgl.nanovg.NanoVG.nvgScale;
import static org.lwjgl.nanovg.NanoVG.nvgShapeAntiAlias;

/**
 * @Author:Gu_YueMang
 * @CreateTime:2026-03-26
 */
@Getter
@Setter
public class NanoVG {
    public static final NanoVG INSTANCE = new NanoVG();

    private long vg = 0L;
    private boolean initialized = false;
    private boolean inFrame = false;

    public void initNanoVG() {
        if (initialized) return;

        System.out.println("Initializing NanoVG");
        vg = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS | NanoVGGL3.NVG_STENCIL_STROKES);
        if (vg == 0L) {
            throw new RuntimeException("Unable to initialize NanoVG");
        }

        nvgShapeAntiAlias(vg, true);
        initialized = true;
    }

    public long getContext() {
        if (!initialized) {
            initNanoVG();
        }
        return vg;
    }

    /**
     * 统一的 NanoVG 绘制入口
     * 注意：不要在外面又套 RenderHelper.beginRender()/endRender()
     * 和这个方法一起用，二选一。
     */
    public void draw(Consumer<Long> drawingLogic) {
        if (!initialized) initNanoVG();

        if (inFrame) {
            drawingLogic.accept(vg);
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        MinecraftClient mc = MinecraftClient.getInstance();
        int fbWidth = mc.getWindow().getFramebufferWidth();
        int fbHeight = mc.getWindow().getFramebufferHeight();
        float scaleFactor = (float) mc.getWindow().getScaleFactor();

        nvgBeginFrame(vg, fbWidth, fbHeight, 1.0f);
        nvgSave(vg);
        nvgScale(vg, scaleFactor, scaleFactor);

        inFrame = true;
        try {
            drawingLogic.accept(vg);
        } finally {
            nvgRestore(vg);
            nvgEndFrame(vg);
            inFrame = false;

            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }
}