package cn.lazymoon.sxmurxy.builders.impl;

import cn.lazymoon.sxmurxy.builders.AbstractBuilder;
import cn.lazymoon.sxmurxy.builders.states.PositionState;
import cn.lazymoon.sxmurxy.builders.states.QuadColorState;
import cn.lazymoon.sxmurxy.builders.states.QuadRadiusState;
import cn.lazymoon.sxmurxy.builders.states.SizeState;
import cn.lazymoon.sxmurxy.renderers.impl.BuiltBlur;
import org.joml.Matrix4f;

public final class BlurBuilder extends AbstractBuilder<BuiltBlur> {

    private Matrix4f matrix4f;
    private PositionState positionState;
    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float smoothness;
    private float blurRadius;

    public BlurBuilder matrix4f(Matrix4f matrix4f) {
        this.matrix4f = matrix4f;
        return this;
    }

    public BlurBuilder position(PositionState position) {
        this.positionState = position;
        return this;
    }

    public BlurBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public BlurBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public BlurBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public BlurBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public BlurBuilder blurRadius(float blurRadius) {
        this.blurRadius = blurRadius;
        return this;
    }

    @Override
    protected BuiltBlur _build() {
        return new BuiltBlur(this.matrix4f, this.positionState, this.size, this.radius, this.color, this.smoothness, this.blurRadius);
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.TRANSPARENT;
        this.smoothness = 1.0f;
        this.blurRadius = 0.0f;
    }

}