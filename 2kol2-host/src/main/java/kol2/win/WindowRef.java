package kol2.win;

/** 一个可见窗口。handle 只交给具体桌面实现使用。 */
public final class WindowRef {
    public String title;
    public final Object handle;

    public WindowRef(String title, Object handle) {
        this.title = title;
        this.handle = handle;
    }
}
