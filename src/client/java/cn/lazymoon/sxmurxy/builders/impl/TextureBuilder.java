package cn.lazymoon.sxmurxy.builders.impl;

import cn.lazymoon.sxmurxy.builders.AbstractBuilder;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltTexture;
import net.minecraft.client.texture.AbstractTexture;
import org.joml.Matrix4f;

public final class TextureBuilder extends AbstractBuilder<BuiltTexture> {

    private Matrix4f matrix4f;
    private PositionState positionState;
    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float smoothness;
    private float u, v;
    private float texWidth, texHeight;
    private int textureId;

    public TextureBuilder matrix4f(Matrix4f matrix4f) {
        this.matrix4f = matrix4f;
        return this;
    }

    public TextureBuilder position(PositionState position) {
        this.positionState = position;
        return this;
    }

    public TextureBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public TextureBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public TextureBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public TextureBuilder smoothness(float smoothness) {
        this.smoothness = Math.max(1f, smoothness);
        return this;
    }

    public TextureBuilder texture(float u, float v, float texWidth, float texHeight, AbstractTexture texture) {
        return texture(u, v, texWidth, texHeight, texture.getGlId());
    }

    public TextureBuilder texture(float u, float v, float texWidth, float texHeight, int textureId) {
        this.u = u;
        this.v = v;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.textureId = textureId;
        return this;
    }

    @Override
    protected BuiltTexture _build() {
        return new BuiltTexture(
                this.matrix4f,
                this.positionState,
                this.size,
                this.radius,
                this.color,
                this.smoothness,
                this.u, this.v,
                this.texWidth, this.texHeight,
                this.textureId
        );
    }

    @Override
    protected void reset() {
        this.positionState = PositionState.NONE;
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.TRANSPARENT;
        this.smoothness = 1.0f;
        this.u = 0.0f;
        this.v = 0.0f;
        this.texWidth = 0.0f;
        this.texHeight = 0.0f;
        this.textureId = 0;
    }

}