package kol2.win;

/** 只接受带标题栏、且客户区正好是配置分辨率的窗口。 */
public final class DisplayCheck {
    private DisplayCheck() {
    }

    /**
     * 符合窗口模式和客户区时返回 null。
     * 否则返回给用户看的停止原因。
     */
    public static String problem(boolean bordered, int width, int height, int expectWidth, int expectHeight) {
        if (!bordered) {
            return "当前不是带标题栏的窗口模式（可能是全屏或无边框）。请改成窗口模式，客户区 "
                    + expectWidth + "x" + expectHeight + "。已停止，不会点击。";
        }
        if (width != expectWidth || height != expectHeight) {
            return "窗口客户区为 " + width + "x" + height + "，需要 " + expectWidth + "x" + expectHeight
                    + "。已停止，不会点击。";
        }
        return null;
    }
}
