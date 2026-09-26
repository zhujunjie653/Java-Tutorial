package kol2.win;

import kol2.Halt;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 通过系统键鼠接口操作前台窗口。 */
public final class RobotActions implements Actions {
    private final Robot robot;
    private final Object keyLock = new Object();
    private final Set<Integer> held = new HashSet<>();

    public RobotActions() {
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new Halt(2, "无法使用键鼠。请在有图形界面的 Windows 上运行。");
        }
        robot.setAutoDelay(20);
    }

    @Override
    public void click(int screenX, int screenY) {
        robot.mouseMove(screenX, screenY);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    @Override
    public void keyDown(int vk) {
        synchronized (keyLock) {
            if (held.add(vk)) {
                robot.keyPress(vk);
            }
        }
    }

    @Override
    public void keyUp(int vk) {
        synchronized (keyLock) {
            if (held.remove(vk)) {
                robot.keyRelease(vk);
            }
        }
    }

    @Override
    public boolean isDown(int vk) {
        synchronized (keyLock) {
            return held.contains(vk);
        }
    }

    @Override
    public void releaseAll() {
        synchronized (keyLock) {
            List<Integer> keys = new ArrayList<>(held);
            for (int vk : keys) {
                robot.keyRelease(vk);
            }
            held.clear();
        }
    }

    @Override
    public void tap(int vk) {
        keyDown(vk);
        keyUp(vk);
    }

    @Override
    public void typeAscii(String text) {
        for (AsciiTyping.Chord chord : AsciiTyping.plan(text)) {
            synchronized (keyLock) {
                if (chord.shift()) {
                    robot.keyPress(KeyEvent.VK_SHIFT);
                }
                robot.keyPress(chord.vk());
                robot.keyRelease(chord.vk());
                if (chord.shift()) {
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                }
            }
        }
    }

    @Override
    public BufferedImage capture(int screenX, int screenY, int width, int height) {
        if (width <= 0 || height <= 0) {
            return null;
        }
        try {
            return robot.createScreenCapture(new Rectangle(screenX, screenY, width, height));
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
