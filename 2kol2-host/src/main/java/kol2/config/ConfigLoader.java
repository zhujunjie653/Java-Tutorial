package kol2.config;

import kol2.AppHome;
import kol2.Halt;
import kol2.win.AsciiTyping;
import kol2.win.KeyCodes;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** 从本机文件读取配置。缺项或路径不存在时拒绝启动。 */
public final class ConfigLoader {
    private ConfigLoader() {
    }

    /** 读取并校验配置。不会把账号或密码写进异常信息。 */
    public static AppConfig load(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            throw new Halt(1, "缺少配置文件 " + file + "。请复制 config/application.example.properties 为本地配置再填写。不要提交该文件。");
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException ex) {
            throw new Halt(1, "配置文件读不了。请检查文件是否被占用。");
        }
        String account = properties.getProperty("account", "").trim();
        String passwordText = properties.getProperty("password", "");
        properties.setProperty("password", "");
        properties.setProperty("account", "");
        if (account.contains("\n") || account.contains("\r") || account.length() > 32) {
            throw new Halt(1, "账号格式不正确，已拒绝启动。");
        }
        if (!account.isEmpty()) {
            try {
                AsciiTyping.plan(account);
            } catch (Halt ex) {
                throw new Halt(1, "账号含有无法键入的字符，已拒绝启动。");
            }
        }
        if (passwordText.length() > 64) {
            throw new Halt(1, "密码过长，已拒绝启动，未提交登录。");
        }
        if (!passwordText.isEmpty()) {
            try {
                AsciiTyping.plan(passwordText);
            } catch (Halt ex) {
                throw new Halt(1, "密码含有无法键入的字符，已拒绝启动，未提交登录。");
            }
        }

        Path wegameDir = Path.of(required(properties, "wegame.dir"));
        if (!Files.isDirectory(wegameDir)) {
            throw new Halt(1, "WeGame 目录不存在: " + wegameDir
                    + "。请检查 wegame.dir。反斜杠要写成双反斜杠，或改用正斜杠。");
        }
        String exeName = required(properties, "wegame.exe");
        if (!exeName.matches("[A-Za-z0-9._-]+\\.exe")) {
            throw new Halt(1, "wegame.exe 只能是文件名，例如 wegame.exe。");
        }
        Path exe = wegameDir.resolve(exeName);
        if (!Files.isRegularFile(exe)) {
            throw new Halt(1, "找不到 WeGame 程序: " + exe);
        }
        String mode = required(properties, "display.mode");
        if (!"window".equalsIgnoreCase(mode.trim())) {
            throw new Halt(1, "display.mode 只能是 window。全屏不会启动。");
        }
        int width = integer(properties, "display.width", 1, 7680);
        int height = integer(properties, "display.height", 1, 4320);
        int refresh = integer(properties, "display.refreshHz", 1, 480);
        String quality = required(properties, "display.quality");
        if (quality.length() > 20) {
            throw new Halt(1, "display.quality 过长。");
        }
        String template = properties.getProperty("template.matchEnd", "").trim();
        return new AppConfig(
                wegameDir,
                exeName,
                required(properties, "wegame.windowTitle"),
                required(properties, "game.windowTitle"),
                account,
                passwordText.toCharArray(),
                flag(properties, "login.coordsConfirmed"),
                required(properties, "login.titleContains"),
                width,
                height,
                refresh,
                quality,
                integer(properties, "daily.matches", 1, 20),
                integer(properties, "daily.startHour", 0, 23),
                integer(properties, "retry.max", 1, 5),
                integer(properties, "ui.menuDelayMs", 0, 60_000),
                integer(properties, "match.dribbleMs", 100, 20_000),
                integer(properties, "match.afterCutMs", 0, 10_000),
                integer(properties, "match.afterPassMs", 0, 10_000),
                integer(properties, "match.idleMs", 0, 10_000),
                integer(properties, "match.loadWaitSec", 0, 600) * 1000L,
                integer(properties, "match.launchTimeoutSec", 1, 900) * 1000L,
                integer(properties, "match.maxMinutes", 1, 120),
                flag(properties, "ui.launchConfirmed"),
                flag(properties, "ui.menuConfirmed"),
                flag(properties, "postgame.confirmed"),
                new KeyBindings(
                        KeyCodes.vk(required(properties, "keys.moveForward")),
                        KeyCodes.vk(required(properties, "keys.sprint")),
                        KeyCodes.vk(required(properties, "keys.callCut")),
                        KeyCodes.vk(required(properties, "keys.pass")),
                        KeyCodes.vk(required(properties, "keys.shoot")),
                        optionalKey(properties, "keys.takeControl")),
                ratio(properties, "click.game.x"),
                ratio(properties, "click.game.y"),
                ratio(properties, "click.login.account.x"),
                ratio(properties, "click.login.account.y"),
                ratio(properties, "click.login.password.x"),
                ratio(properties, "click.login.password.y"),
                ratio(properties, "click.pve.x"),
                ratio(properties, "click.pve.y"),
                ratio(properties, "click.dynasty.x"),
                ratio(properties, "click.dynasty.y"),
                ratio(properties, "click.difficulty.x"),
                ratio(properties, "click.difficulty.y"),
                ratio(properties, "click.enter.x"),
                ratio(properties, "click.enter.y"),
                ratio(properties, "click.rematch.x"),
                ratio(properties, "click.rematch.y"),
                AppHome.resolve(Path.of(required(properties, "log.dir"))),
                AppHome.resolve(Path.of(required(properties, "instance.lock"))),
                template.isEmpty() ? null : AppHome.resolve(Path.of(template)));
    }

    private static int optionalKey(Properties properties, String key) {
        String raw = properties.getProperty(key, "").trim();
        if (raw.isEmpty()) {
            return 0;
        }
        return KeyCodes.vk(raw);
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new Halt(1, "配置缺少 " + key + "。");
        }
        return value.trim();
    }

    private static boolean flag(Properties properties, String key) {
        String raw = required(properties, key);
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        throw new Halt(1, key + " 只能是 true 或 false。");
    }

    private static int integer(Properties properties, String key, int min, int max) {
        String raw = required(properties, key);
        int value;
        try {
            value = Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new Halt(1, key + " 不是整数。");
        }
        if (value < min || value > max) {
            throw new Halt(1, key + " 超出允许范围。");
        }
        return value;
    }

    private static double ratio(Properties properties, String key) {
        String raw = required(properties, key);
        double value;
        try {
            value = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            throw new Halt(1, key + " 不是数字。");
        }
        if (value <= 0 || value >= 1) {
            throw new Halt(1, key + " 必须在 0 和 1 之间。");
        }
        return value;
    }
}
