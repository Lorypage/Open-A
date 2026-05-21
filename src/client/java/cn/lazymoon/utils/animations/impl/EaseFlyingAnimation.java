package cn.lazymoon.utils.animations.impl;

import cn.lazymoon.utils.animations.Animation;

public class EaseFlyingAnimation extends Animation {

    public boolean inCir = false;

    public EaseFlyingAnimation(int duration,int endpoint) {
        super(duration,endpoint);
    }

    public void setInCir(boolean inCir) {
        this.inCir = inCir;
    }

    @Override
    public double getEquation(double value) {
        return inCir ? 1.0 - Math.sqrt(1.0 - Math.pow(value, 2.0)) : Math.sqrt(1.0 - Math.pow(value - 1.0, 2.0));
    }
    
}
