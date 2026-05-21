package cn.lazymoon.sxmurxy.renderers.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.instance.TextureShaderKeyInstance;
import cn.lazymoon.sxmurxy.renderers.IRenderer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import java.util.Objects;

public record BuiltTexture(Matrix4f matrix4f, PositionState positionState, SizeState size, QuadRadiusState radius, QuadColorState color, float smoothness, float u, float v, float texWidth, float texHeight, int textureId) implements IRenderer {

    @SuppressWarnings("resource")
    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        RenderSystem.setShaderTexture(0, this.textureId);

        float width = this.size.width(), height = this.size.height();
        ShaderProgram shader = RenderSystem.setShader(TextureShaderKeyInstance.TEXTURE_SHADER_KEY);
        assert shader != null;
        Objects.requireNonNull(shader.getUniform("Size")).set(width, height);
        Objects.requireNonNull(shader.getUniform("Radius")).set(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4());
        Objects.requireNonNull(shader.getUniform("Smoothness")).set(this.smoothness);

        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix, x, y, z).texture(this.u, this.v).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).texture(this.u, this.v + this.texHeight).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).texture(this.u + this.texWidth, this.v + this.texHeight).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).texture(this.u + this.texWidth, this.v).color(this.color.color4());

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.setShaderTexture(0, 0);
    }

}