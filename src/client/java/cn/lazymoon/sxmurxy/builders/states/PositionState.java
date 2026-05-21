package cn.lazymoon.sxmurxy.builders.states;

public record PositionState(float x, float y) {

    public static final PositionState NONE = new PositionState(0.0f, 0.0f);

    public PositionState(double x, double y) {
        this((float) x, (float) y);
    }

}
