package kol2.run;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** 本机日志。写入前去掉密码和完整账号。 */
public final class HostLog implements AutoCloseable {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Logger logger;
    private final String account;
    private final char[] password;
    private final Path file;

    private HostLog(Logger logger, String account, char[] password, Path file) {
        this.logger = logger;
        this.account = account == null ? "" : account;
        this.password = password == null ? new char[0] : password;
        this.file = file;
    }

    /** 在目录下打开 kol2-host.log。 */
    public static HostLog open(Path dir, String account, char[] password) {
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve("kol2-host.log");
            Logger logger = Logger.getLogger("kol2.host." + UUID.randomUUID());
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.INFO);
            HostLog log = new HostLog(logger, account, password == null ? new char[0] : Arrays.copyOf(password, password.length), file);
            FileHandler fileHandler = new FileHandler(file.toString(), true);
            fileHandler.setFormatter(formatter(log));
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(formatter(log));
            logger.addHandler(fileHandler);
            logger.addHandler(consoleHandler);
            return log;
        } catch (IOException ex) {
            throw new kol2.Halt(2, "日志目录创建失败: " + dir);
        }
    }

    private static SimpleFormatter formatter(HostLog log) {
        return new SimpleFormatter() {
            @Override
            public synchronized String format(java.util.logging.LogRecord record) {
                return log.redact(record.getMessage()) + System.lineSeparator();
            }
        };
    }

    /** 记录一步的结果。 */
    public void info(String step, String result, String detail) {
        logger.info(line("INFO", step, result, detail));
    }

    /** 记录失败或停止原因。 */
    public void error(String step, String result, String detail) {
        logger.severe(line("ERROR", step, result, detail));
    }

    /** 日志文件路径。 */
    public Path file() {
        return file;
    }

    private String line(String level, String step, String result, String detail) {
        String time = TIME.format(LocalDateTime.now());
        return time + " level=" + level + " step=" + step + " result=" + result + " detail=" + safe(detail);
    }

    private String safe(String detail) {
        return detail == null ? "" : detail;
    }

    String redact(String message) {
        String text = message == null ? "" : message;
        if (account.length() >= 4) {
            text = text.replace(account, "账号已隐藏");
        }
        String secret = new String(password);
        if (!secret.isEmpty()) {
            text = text.replace(secret, "***");
        }
        return text;
    }

    /** 关掉文件并抹掉用于过滤的密码副本。 */
    @Override
    public void close() {
        for (Handler handler : logger.getHandlers()) {
            logger.removeHandler(handler);
            handler.flush();
            handler.close();
        }
        Arrays.fill(password, '\0');
    }
}
