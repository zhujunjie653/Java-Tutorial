package kol2.win;

import kol2.Halt;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 按美式键盘的符号位置组织按键。
 * 中文输入法打开时可能键错，程序不会去切输入法。
 */
public final class AsciiTyping {
    /** 是否按住 Shift，以及要按的虚拟键。 */
    public record Chord(boolean shift, int vk) {
    }

    private AsciiTyping() {
    }

    /** 把可打印 ASCII 编成按键。含无法键入的字符时拒绝，信息里不带原字符。 */
    public static List<Chord> plan(String text) {
        List<Chord> chords = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            chords.add(chordAt(text.charAt(i)));
        }
        return chords;
    }

    private static Chord chordAt(char c) {
        if (c >= 'a' && c <= 'z') {
            return new Chord(false, KeyEvent.VK_A + (c - 'a'));
        }
        if (c >= 'A' && c <= 'Z') {
            return new Chord(true, KeyEvent.VK_A + (c - 'A'));
        }
        if (c >= '0' && c <= '9') {
            return digit(c);
        }
        return switch (c) {
            case ' ' -> new Chord(false, KeyEvent.VK_SPACE);
            case '-' -> new Chord(false, KeyEvent.VK_MINUS);
            case '_' -> new Chord(true, KeyEvent.VK_MINUS);
            case '=' -> new Chord(false, KeyEvent.VK_EQUALS);
            case '+' -> new Chord(true, KeyEvent.VK_EQUALS);
            case '[' -> new Chord(false, KeyEvent.VK_OPEN_BRACKET);
            case '{' -> new Chord(true, KeyEvent.VK_OPEN_BRACKET);
            case ']' -> new Chord(false, KeyEvent.VK_CLOSE_BRACKET);
            case '}' -> new Chord(true, KeyEvent.VK_CLOSE_BRACKET);
            case '\\' -> new Chord(false, KeyEvent.VK_BACK_SLASH);
            case '|' -> new Chord(true, KeyEvent.VK_BACK_SLASH);
            case ';' -> new Chord(false, KeyEvent.VK_SEMICOLON);
            case ':' -> new Chord(true, KeyEvent.VK_SEMICOLON);
            case '\'' -> new Chord(false, KeyEvent.VK_QUOTE);
            case '"' -> new Chord(true, KeyEvent.VK_QUOTE);
            case ',' -> new Chord(false, KeyEvent.VK_COMMA);
            case '<' -> new Chord(true, KeyEvent.VK_COMMA);
            case '.' -> new Chord(false, KeyEvent.VK_PERIOD);
            case '>' -> new Chord(true, KeyEvent.VK_PERIOD);
            case '/' -> new Chord(false, KeyEvent.VK_SLASH);
            case '?' -> new Chord(true, KeyEvent.VK_SLASH);
            case '`' -> new Chord(false, KeyEvent.VK_BACK_QUOTE);
            case '~' -> new Chord(true, KeyEvent.VK_BACK_QUOTE);
            case '!' -> new Chord(true, KeyEvent.VK_1);
            case '@' -> new Chord(true, KeyEvent.VK_2);
            case '#' -> new Chord(true, KeyEvent.VK_3);
            case '$' -> new Chord(true, KeyEvent.VK_4);
            case '%' -> new Chord(true, KeyEvent.VK_5);
            case '^' -> new Chord(true, KeyEvent.VK_6);
            case '&' -> new Chord(true, KeyEvent.VK_7);
            case '*' -> new Chord(true, KeyEvent.VK_8);
            case '(' -> new Chord(true, KeyEvent.VK_9);
            case ')' -> new Chord(true, KeyEvent.VK_0);
            default -> throw new Halt(1, "有字符无法键入。");
        };
    }

    private static Chord digit(char c) {
        boolean shifted = false;
        int vk = KeyEvent.VK_0 + (c - '0');
        return new Chord(shifted, vk);
    }
}
