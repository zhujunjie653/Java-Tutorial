package kol2.win;

import java.awt.image.BufferedImage;

/** 键鼠操作。暂停和停止由调用方先放开按键。 */
public interface Actions {
    /** 在屏幕坐标点一下左键。 */
    void click(int screenX, int screenY);

    /** 按下并保持。重复按下同一键不会叠多次。 */
    void keyDown(int vk);

    /** 松开一个键。 */
    void keyUp(int vk);

    /** 该键是否仍被按住。 */
    boolean isDown(int vk);

    /** 松开全部按住的键。 */
    void releaseAll();

    /** 点按一次。 */
    void tap(int vk);

    /** 按美式键盘键入。调用前应已确认字符可键入。 */
    void typeAscii(String text);

    /** 截取屏幕区域。无法截取时返回 null。 */
    BufferedImage capture(int screenX, int screenY, int width, int height);
}
