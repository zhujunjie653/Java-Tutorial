package kol2.ui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** 界面上要编辑的配置。不含日志输出。 */
public final class ConfigForm {
    /** 高级项里的键和中文标签，顺序即界面顺序。 */
    public static final String[][] ADVANCED = {
            {"keys.moveForward", "向前"},
            {"keys.sprint", "冲刺"},
            {"keys.callCut", "呼叫内切"},
            {"keys.pass", "传球"},
            {"keys.shoot", "投篮"},
            {"keys.takeControl", "切到持球人（可空）"},
            {"click.game.x", "游戏图标 X"},
            {"click.game.y", "游戏图标 Y"},
            {"click.login.account.x", "登录账号框 X"},
            {"click.login.account.y", "登录账号框 Y"},
            {"click.login.password.x", "登录密码框 X"},
            {"click.login.password.y", "登录密码框 Y"},
            {"click.pve.x", "人机赛 X"},
            {"click.pve.y", "人机赛 Y"},
            {"click.dynasty.x", "王朝模式 X"},
            {"click.dynasty.y", "王朝模式 Y"},
            {"click.difficulty.x", "非常简单 X"},
            {"click.difficulty.y", "非常简单 Y"},
            {"click.enter.x", "进入 X"},
            {"click.enter.y", "进入 Y"},
            {"click.rematch.x", "赛后重开 X"},
            {"click.rematch.y", "赛后重开 Y"}
    };

    public String wegameDir = "";
    public String account = "";
    public String password = "";
    public String width = "2560";
    public String height = "1440";
    public boolean loginConfirmed;
    public boolean launchConfirmed;
    public boolean menuConfirmed;
    public boolean postgameConfirmed;
    public final Map<String, String> advanced = new LinkedHashMap<>();

    public ConfigForm() {
        for (String[] row : ADVANCED) {
            advanced.put(row[0], "");
        }
    }

    /** 从已读入的配置填表。确认开关缺省为关闭。 */
    public void read(Properties properties) {
        wegameDir = text(properties, "wegame.dir");
        account = text(properties, "account");
        password = properties.getProperty("password", "");
        width = text(properties, "display.width");
        if (width.isEmpty()) {
            width = "2560";
        }
        height = text(properties, "display.height");
        if (height.isEmpty()) {
            height = "1440";
        }
        loginConfirmed = flag(properties, "login.coordsConfirmed");
        launchConfirmed = flag(properties, "ui.launchConfirmed");
        menuConfirmed = flag(properties, "ui.menuConfirmed");
        postgameConfirmed = flag(properties, "postgame.confirmed");
        for (String key : advanced.keySet()) {
            advanced.put(key, text(properties, key));
        }
    }

    /** 把表上的值写回配置。路径里的反斜杠改成正斜杠。 */
    public void apply(Properties properties) {
        properties.setProperty("wegame.dir", wegameDir.replace('\\', '/').trim());
        properties.setProperty("account", account.trim());
        properties.setProperty("password", password == null ? "" : password);
        properties.setProperty("display.width", width.trim());
        properties.setProperty("display.height", height.trim());
        properties.setProperty("display.mode", "window");
        properties.setProperty("login.coordsConfirmed", Boolean.toString(loginConfirmed));
        properties.setProperty("ui.launchConfirmed", Boolean.toString(launchConfirmed));
        properties.setProperty("ui.menuConfirmed", Boolean.toString(menuConfirmed));
        properties.setProperty("postgame.confirmed", Boolean.toString(postgameConfirmed));
        for (Map.Entry<String, String> entry : advanced.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().trim());
        }
    }

    /** 避免调试输出带出密码。 */
    @Override
    public String toString() {
        return "ConfigForm";
    }

    private static String text(Properties properties, String key) {
        String value = properties.getProperty(key, "");
        return value == null ? "" : value.trim();
    }

    private static boolean flag(Properties properties, String key) {
        return "true".equalsIgnoreCase(properties.getProperty(key, "false").trim());
    }
}
