package kol2.win;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import kol2.Halt;

import java.util.ArrayList;
import java.util.List;

/** 用系统窗口接口找 WeGame / 游戏窗口。不打开进程内存。 */
public final class JnaDesktop implements Desktop {
    /** 补上 jna-platform 的 User32 里没有的两个调用。 */
    interface User32Extra extends User32 {
        User32Extra INSTANCE = Native.load("user32", User32Extra.class);

        /** 客户区坐标换成屏幕坐标。 */
        boolean ClientToScreen(WinDef.HWND hwnd, WinDef.POINT point);

        /** 让本进程按物理像素读窗口，避免缩放后点偏。 */
        boolean SetProcessDPIAware();
    }

    public JnaDesktop() {
        User32Extra.INSTANCE.SetProcessDPIAware();
    }

    @Override
    public List<WindowRef> findVisible(String titleContains) {
        String needle = titleContains == null ? "" : titleContains;
        List<WindowRef> found = new ArrayList<>();
        User32Extra.INSTANCE.EnumWindows((hwnd, data) -> {
            try {
                if (!User32Extra.INSTANCE.IsWindowVisible(hwnd)) {
                    return true;
                }
                char[] buffer = new char[512];
                User32Extra.INSTANCE.GetWindowText(hwnd, buffer, buffer.length);
                String title = Native.toString(buffer).trim();
                if (!title.isEmpty() && title.contains(needle)) {
                    found.add(new WindowRef(title, hwnd));
                }
            } catch (RuntimeException ignored) {
                // 单个窗口读失败就跳过，避免枚举中断。
            }
            return true;
        }, null);
        return found;
    }

    @Override
    public boolean foreground(WindowRef window) {
        return User32Extra.INSTANCE.SetForegroundWindow(hwnd(window));
    }

    @Override
    public void restore(WindowRef window) {
        User32Extra.INSTANCE.ShowWindow(hwnd(window), WinUser.SW_RESTORE);
    }

    @Override
    public ClientArea client(WindowRef window) {
        WinDef.HWND hwnd = hwnd(window);
        WinDef.RECT rect = new WinDef.RECT();
        if (!User32Extra.INSTANCE.GetClientRect(hwnd, rect)) {
            throw new Halt(2, "读不到窗口客户区，已停止。");
        }
        WinDef.POINT origin = new WinDef.POINT(0, 0);
        if (!User32Extra.INSTANCE.ClientToScreen(hwnd, origin)) {
            throw new Halt(2, "无法换算窗口坐标，已停止。");
        }
        long style = User32Extra.INSTANCE.GetWindowLongPtr(hwnd, WinUser.GWL_STYLE).longValue();
        boolean bordered = (style & WinUser.WS_CAPTION) == WinUser.WS_CAPTION;
        return new ClientArea(bordered, rect.right - rect.left, rect.bottom - rect.top, origin.x, origin.y);
    }

    private static WinDef.HWND hwnd(WindowRef window) {
        if (window.handle instanceof WinDef.HWND handle) {
            return handle;
        }
        throw new Halt(2, "窗口句柄无效，已停止。");
    }
}
