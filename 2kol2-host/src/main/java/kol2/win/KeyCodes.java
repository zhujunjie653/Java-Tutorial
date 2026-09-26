package kol2.win;

import kol2.Halt;

import java.awt.event.KeyEvent;
import java.util.Locale;

/** 把配置里的键名变成虚拟键。只接受字母、数字和少量功能键。 */
public final class KeyCodes {
    private KeyCodes() {
    }

    /** 解析键名。无法识别时拒绝启动。 */
    public static int vk(String name) {
        String token = name.trim().toUpperCase(Locale.ROOT);
        return switch (token) {
            case "SHIFT" -> KeyEvent.VK_SHIFT;
            case "SPACE" -> KeyEvent.VK_SPACE;
            case "CONTROL", "CTRL" -> KeyEvent.VK_CONTROL;
            case "TAB" -> KeyEvent.VK_TAB;
            case "ENTER" -> KeyEvent.VK_ENTER;
            case "ESC", "ESCAPE" -> KeyEvent.VK_ESCAPE;
            default -> letterOrDigit(name, token);
        };
    }

    private static int letterOrDigit(String original, String token) {
        if (token.length() == 1) {
            char c = token.charAt(0);
            if (c >= 'A' && c <= 'Z') {
                return KeyEvent.VK_A + (c - 'A');
            }
            if (c >= '0' && c <= '9') {
                return KeyEvent.VK_0 + (c - '0');
            }
        }
        throw new Halt(1, "无法识别的按键: " + original);
    }
}
