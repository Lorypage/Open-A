package cn.lazymoon.nanovg.gl;

import java.util.Stack;

import static org.lwjgl.opengl.GL30.*;

public class States {

    private static final int glVersion;
    private static final Stack<State> states = new Stack<>();

    static {
        int[] major = new int[1];
        int[] minor = new int[1];
        glGetIntegerv(GL_MAJOR_VERSION, major);
        glGetIntegerv(GL_MINOR_VERSION, minor);
        glVersion = major[0] * 100 + minor[0] * 10;
    }

    public static void push() {
        states.push(new State(glVersion).push());
    }

    public static void pop() {
        if (states.isEmpty()) throw new IllegalArgumentException("No state to restore.");
        states.pop().pop();
    }
}
