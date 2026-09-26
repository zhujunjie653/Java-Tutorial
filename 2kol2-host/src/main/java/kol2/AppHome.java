package kol2;

import java.nio.file.Path;

/** 程序所在目录。打包后是 exe 旁边，否则是当前工作目录。 */
public final class AppHome {
    private AppHome() {
    }

    /** 配置、日志相对路径以此为基准。 */
    public static Path home() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path parent = Path.of(appPath).getParent();
            if (parent != null) {
                return parent;
            }
        }
        return Path.of(System.getProperty("user.dir"));
    }

    /** 是否由 jpackage 生成的 exe 启动。 */
    public static boolean packaged() {
        String appPath = System.getProperty("jpackage.app-path");
        return appPath != null && !appPath.isBlank();
    }

    /** 绝对路径原样返回，相对路径接到程序目录下。 */
    public static Path resolve(Path path) {
        if (path.isAbsolute()) {
            return path;
        }
        return home().resolve(path).normalize();
    }

    /** 本机配置。打包后放在 exe 旁边。 */
    public static Path configFile() {
        if (packaged()) {
            return home().resolve("application.properties");
        }
        return home().resolve("config").resolve("application.properties");
    }

    /** 示例配置。打包后也放在 exe 旁边，不含用户填过的账号。 */
    public static Path exampleFile() {
        if (packaged()) {
            return home().resolve("application.example.properties");
        }
        return home().resolve("config").resolve("application.example.properties");
    }
}
