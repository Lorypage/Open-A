package cn.lazymoon.utils.key;

import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KeyCodeConverter {
    private static final Map<String, Integer> keyMap = new HashMap<>();
    private static final Map<Integer, String> reverseKeyMap = new HashMap<>();

//    static {
//        for (char c = 'a'; c <= 'z'; c++) {
//            keyMap.put(String.valueOf(c), (int) Character.toUpperCase(c));
//        }
//
//        for (int i = 0; i <= 9; i++) {
//            keyMap.put(String.valueOf(i), GLFW.GLFW_KEY_0 + i);
//        }
//
//        for (int i = 1; i <= 12; i++) {
//            keyMap.put("f" + i, GLFW.GLFW_KEY_F1 + (i - 1));
//        }
//
//        for (int i = 0; i <= 9; i++) {
//            keyMap.put("kp_" + i, GLFW.GLFW_KEY_KP_0 + i);
//        }
//
//        keyMap.put("none", 0);
//        keyMap.put("escape", GLFW.GLFW_KEY_ESCAPE);
//        keyMap.put("enter", GLFW.GLFW_KEY_ENTER);
//        keyMap.put("tab", GLFW.GLFW_KEY_TAB);
//        keyMap.put("backspace", GLFW.GLFW_KEY_BACKSPACE);
//        keyMap.put("space", GLFW.GLFW_KEY_SPACE);
//        keyMap.put("left_shift", GLFW.GLFW_KEY_LEFT_SHIFT);
//        keyMap.put("right_shift", GLFW.GLFW_KEY_RIGHT_SHIFT);
//        keyMap.put("left_control", GLFW.GLFW_KEY_LEFT_CONTROL);
//        keyMap.put("right_control", GLFW.GLFW_KEY_RIGHT_CONTROL);
//        keyMap.put("left_alt", GLFW.GLFW_KEY_LEFT_ALT);
//        keyMap.put("right_alt", GLFW.GLFW_KEY_RIGHT_ALT);
//        keyMap.put("up", GLFW.GLFW_KEY_UP);
//        keyMap.put("down", GLFW.GLFW_KEY_DOWN);
//        keyMap.put("left", GLFW.GLFW_KEY_LEFT);
//        keyMap.put("right", GLFW.GLFW_KEY_RIGHT);
//
//        keyMap.put("comma", GLFW.GLFW_KEY_COMMA);
//        keyMap.put("period", GLFW.GLFW_KEY_PERIOD);
//        keyMap.put("slash", GLFW.GLFW_KEY_SLASH);
//        keyMap.put("semicolon", GLFW.GLFW_KEY_SEMICOLON);
//        keyMap.put("apostrophe", GLFW.GLFW_KEY_APOSTROPHE);
//        keyMap.put("minus", GLFW.GLFW_KEY_MINUS);
//        keyMap.put("equal", GLFW.GLFW_KEY_EQUAL);
//        keyMap.put("left_bracket", GLFW.GLFW_KEY_LEFT_BRACKET);
//        keyMap.put("right_bracket", GLFW.GLFW_KEY_RIGHT_BRACKET);
//        keyMap.put("backslash", GLFW.GLFW_KEY_BACKSLASH);
//        keyMap.put("grave_accent", GLFW.GLFW_KEY_GRAVE_ACCENT);
//        keyMap.put("kp_enter", GLFW.GLFW_KEY_KP_ENTER);
//        keyMap.put("kp_add", GLFW.GLFW_KEY_KP_ADD);
//        keyMap.put("kp_subtract", GLFW.GLFW_KEY_KP_SUBTRACT);
//        keyMap.put("kp_multiply", GLFW.GLFW_KEY_KP_MULTIPLY);
//        keyMap.put("kp_divide", GLFW.GLFW_KEY_KP_DIVIDE);
//
//        keyMap.put("insert", GLFW.GLFW_KEY_INSERT);
//        keyMap.put("delete", GLFW.GLFW_KEY_DELETE);
//        keyMap.put("home", GLFW.GLFW_KEY_HOME);
//        keyMap.put("end", GLFW.GLFW_KEY_END);
//        keyMap.put("page_up", GLFW.GLFW_KEY_PAGE_UP);
//        keyMap.put("page_down", GLFW.GLFW_KEY_PAGE_DOWN);
//
//        keyMap.put("caps_lock", GLFW.GLFW_KEY_CAPS_LOCK);
//        keyMap.put("scroll_lock", GLFW.GLFW_KEY_SCROLL_LOCK);
//        keyMap.put("num_lock", GLFW.GLFW_KEY_NUM_LOCK);
//
//        keyMap.put("print_screen", GLFW.GLFW_KEY_PRINT_SCREEN);
//        keyMap.put("pause", GLFW.GLFW_KEY_PAUSE);
//        keyMap.put("menu", GLFW.GLFW_KEY_MENU);
//
//        keyMap.put("kp_decimal", GLFW.GLFW_KEY_KP_DECIMAL);
//        keyMap.put("kp_equal", GLFW.GLFW_KEY_KP_EQUAL);
//
//        keyMap.put("left_super", GLFW.GLFW_KEY_LEFT_SUPER);
//        keyMap.put("right_super", GLFW.GLFW_KEY_RIGHT_SUPER);
//
//        keyMap.forEach((k, v) -> reverseKeyMap.put(v, k));
//    }

    static {
        try {
            for (Field field : GLFW.class.getDeclaredFields()) {
                String name = field.getName();
                if (!name.startsWith("GLFW_KEY_")) continue;

                int value = field.getInt(null);
                if (value <= 0) continue;

                String key = name.substring("GLFW_KEY_".length()).toLowerCase();
                keyMap.put(key, value);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to init KeyCodeConverter", e);
        }

        keyMap.put("none", 0);
        keyMap.forEach((k, v) -> reverseKeyMap.put(v, k));
    }

    public static int stringToKeycode(String keyStr) throws IllegalArgumentException {
        keyStr = keyStr.toLowerCase();
        return keyMap.getOrDefault(keyStr, 0);
    }

    public static int getKeyCodeFromName(String name) {
        if (name == null || name.isEmpty()) return GLFW.GLFW_KEY_UNKNOWN;

        // 单字母 A-Z
        if (name.length() == 1) {
            char c = name.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return GLFW.GLFW_KEY_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return GLFW.GLFW_KEY_0 + (c - '0');
            }
        }

        // 特殊键名
        switch (name.toUpperCase()) {
            case "SPACE": return GLFW.GLFW_KEY_SPACE;
            case "LSHIFT": return GLFW.GLFW_KEY_LEFT_SHIFT;
            case "RSHIFT": return GLFW.GLFW_KEY_RIGHT_SHIFT;
            case "LCTRL": return GLFW.GLFW_KEY_LEFT_CONTROL;
            case "RCTRL": return GLFW.GLFW_KEY_RIGHT_CONTROL;
            case "LALT": return GLFW.GLFW_KEY_LEFT_ALT;
            case "RALT": return GLFW.GLFW_KEY_RIGHT_ALT;
            case "TAB": return GLFW.GLFW_KEY_TAB;
            case "ESC": return GLFW.GLFW_KEY_ESCAPE;
            case "ENTER": return GLFW.GLFW_KEY_ENTER;
            case "BACKSPACE": return GLFW.GLFW_KEY_BACKSPACE;
            case "UP": return GLFW.GLFW_KEY_UP;
            case "DOWN": return GLFW.GLFW_KEY_DOWN;
            case "LEFT": return GLFW.GLFW_KEY_LEFT;
            case "RIGHT": return GLFW.GLFW_KEY_RIGHT;
            case "F1": return GLFW.GLFW_KEY_F1;
            case "F2": return GLFW.GLFW_KEY_F2;
            case "F3": return GLFW.GLFW_KEY_F3;
            case "F4": return GLFW.GLFW_KEY_F4;
            case "F5": return GLFW.GLFW_KEY_F5;
            case "F6": return GLFW.GLFW_KEY_F6;
            case "F7": return GLFW.GLFW_KEY_F7;
            case "F8": return GLFW.GLFW_KEY_F8;
            case "F9": return GLFW.GLFW_KEY_F9;
            case "F10": return GLFW.GLFW_KEY_F10;
            case "F11": return GLFW.GLFW_KEY_F11;
            case "F12": return GLFW.GLFW_KEY_F12;
            default:
                try {
                    return Integer.parseInt(name);
                } catch (NumberFormatException e) {
                    return GLFW.GLFW_KEY_UNKNOWN;
                }
        }
    }

    public static String keycodeToString(int keycode) throws IllegalArgumentException {
        String result = reverseKeyMap.get(keycode);
        if (result == null) {
            return "unknown";
        }
        return result;
    }

    public static Set<String> getKeyNames() {
        return keyMap.keySet();
    }
}
