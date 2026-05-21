package cn.lazymoon.sxmurxy.instance;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import cn.lazymoon.sxmurxy.providers.ResourceProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.VertexFormats;
public class SimpleFrameBufferInstance {

    public static final ShaderProgramKey BLUR_SHADER_KEY = new ShaderProgramKey(ResourceProvider.getShaderIdentifier("blur"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    public static final Supplier<SimpleFramebuffer> TEMP_FBO_SUPPLIER = Suppliers.memoize(() -> new SimpleFramebuffer(2560, 1440, false));
    public static final Framebuffer MAIN_FBO = MinecraftClient.getInstance().getFramebuffer();
    public static SimpleFramebuffer simpleFramebuffer = TEMP_FBO_SUPPLIER.get();

    public static void updateSimpleFrameBufferInstance() {
        simpleFramebuffer = TEMP_FBO_SUPPLIER.get();

        if (simpleFramebuffer.textureWidth != MAIN_FBO.textureWidth || simpleFramebuffer.textureHeight != MAIN_FBO.textureHeight) {
            simpleFramebuffer.resize(MAIN_FBO.textureWidth, MAIN_FBO.textureHeight);
        }
    }

    public static void renderSimpleFrameBufferInstance() {
        SimpleFrameBufferInstance.simpleFramebuffer.beginWrite(false);
        SimpleFrameBufferInstance.MAIN_FBO.draw(SimpleFrameBufferInstance.simpleFramebuffer.textureWidth, SimpleFrameBufferInstance.simpleFramebuffer.textureHeight);
        SimpleFrameBufferInstance.MAIN_FBO.beginWrite(false);
    }

    public static void setShaderTexturePre() {
        RenderSystem.setShaderTexture(0, SimpleFrameBufferInstance.simpleFramebuffer.getColorAttachment());
    }

    public static void setShaderTexturePost() {
        RenderSystem.setShaderTexture(0, 0);
    }

}
