package kol2.win;

import java.util.List;

/** 查找官方客户端窗口并读取客户区。不做内存读取。 */
public interface Desktop {
    /** 客户区尺寸，以及客户区左上角在屏幕上的位置。 */
    final class ClientArea {
        public final boolean bordered;
        public final int width;
        public final int height;
        public final int originX;
        public final int originY;

        public ClientArea(boolean bordered, int width, int height, int originX, int originY) {
            this.bordered = bordered;
            this.width = width;
            this.height = height;
            this.originX = originX;
            this.originY = originY;
        }
    }

    /** 按标题包含关系查找可见窗口。 */
    List<WindowRef> findVisible(String titleContains);

    /** 尝试把窗口放到前台。失败时返回 false。 */
    boolean foreground(WindowRef window);

    /** 若窗口最小化则恢复，不改变它的分辨率。 */
    void restore(WindowRef window);

    /** 读取客户区。读不到时抛出停止。 */
    ClientArea client(WindowRef window);
}
