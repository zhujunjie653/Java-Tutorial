package kol2.ui;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** 读写 exe 旁边的本地配置。不写日志。 */
public final class ConfigStore {
    private ConfigStore() {
    }

    /**
     * 本地配置不存在时，从示例复制一份。
     * 已有文件不会被覆盖。
     */
    public static void ensure(Path example, Path target) throws IOException {
        if (Files.isRegularFile(target)) {
            return;
        }
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (example != null && Files.isRegularFile(example)) {
            Files.copy(example, target);
            return;
        }
        try (InputStream in = ConfigStore.class.getResourceAsStream("/config/application.example.properties")) {
            if (in == null) {
                throw new IOException("找不到配置示例，无法生成 " + target);
            }
            Files.copy(in, target);
        }
    }

    /** 读配置。文件不存在时返回空表。 */
    public static Properties load(Path file) throws IOException {
        Properties properties = new Properties();
        if (file == null || !Files.isRegularFile(file)) {
            return properties;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    /** 在原有配置上覆盖界面字段后写回。 */
    public static void save(Path file, Path example, ConfigForm form) throws IOException {
        Properties properties = new Properties();
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } else if (example != null && Files.isRegularFile(example)) {
            try (Reader reader = Files.newBufferedReader(example, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        form.apply(properties);
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            properties.store(writer, "local");
        }
    }
}
