package kol2.ui;

import kol2.AppHome;
import kol2.run.HostLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopClientTest {
    @TempDir
    Path temp;

    @Test
    void packagedConfigSitsNextToExe() {
        String previous = System.getProperty("jpackage.app-path");
        System.setProperty("jpackage.app-path", temp.resolve("2KOL2Host.exe").toString());
        try {
            assertEquals(temp.resolve("application.properties"), AppHome.configFile());
            assertEquals(temp.resolve("application.example.properties"), AppHome.exampleFile());
            assertTrue(AppHome.packaged());
        } finally {
            if (previous == null) {
                System.clearProperty("jpackage.app-path");
            } else {
                System.setProperty("jpackage.app-path", previous);
            }
        }
    }

    @Test
    void firstLaunchCopiesExampleAndDoesNotOverwrite() throws Exception {
        Path example = temp.resolve("application.example.properties");
        Path target = temp.resolve("application.properties");
        Files.writeString(example, "account=\npassword=\nmarker=example\n", StandardCharsets.UTF_8);
        ConfigStore.ensure(example, target);
        assertEquals("example", value(target, "marker"));
        Files.writeString(target, "marker=user\n", StandardCharsets.UTF_8);
        ConfigStore.ensure(example, target);
        assertEquals("user", value(target, "marker"));
    }

    @Test
    void classpathExampleUsedWhenFileMissing() throws Exception {
        Path target = temp.resolve("from-jar.properties");
        ConfigStore.ensure(null, target);
        String text = Files.readString(target);
        assertTrue(text.contains("wegame.dir="));
        assertTrue(text.contains("password="));
        assertFalse(text.contains("password=1"));
        try (var in = ConfigStore.class.getResourceAsStream("/config/application.example.properties")) {
            assertNotNull(in);
        }
    }

    @Test
    void formKeepsSwitchesOffAndRoundTripsPasswordOnlyInFile() throws Exception {
        Properties example = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("config", "application.example.properties"))) {
            example.load(reader);
        }
        ConfigForm form = new ConfigForm();
        form.read(example);
        assertFalse(form.loginConfirmed);
        assertFalse(form.launchConfirmed);
        assertFalse(form.menuConfirmed);
        assertFalse(form.postgameConfirmed);
        assertEquals("2560", form.width);
        assertEquals("1440", form.height);
        assertEquals("", form.account);
        assertEquals("", form.password);
        assertEquals("ConfigForm", form.toString());
        assertFalse(HostFrame.allowDaily(false));
        assertTrue(HostFrame.allowDaily(true));

        form.wegameDir = "D:\\Program Files (x86)\\WeGame";
        form.account = "user12";
        form.password = "dummy-pass";
        form.width = "2560";
        form.height = "1440";
        Path saved = temp.resolve("application.properties");
        ConfigStore.save(saved, Path.of("config", "application.example.properties"), form);
        String fileText = Files.readString(saved);
        assertTrue(fileText.contains("D:/Program Files (x86)/WeGame") || fileText.contains("D\\:"));
        assertFalse(form.toString().contains("dummy-pass"));

        Properties loaded = ConfigStore.load(saved);
        assertEquals("user12", loaded.getProperty("account"));
        assertEquals("dummy-pass", loaded.getProperty("password"));
        assertEquals("false", loaded.getProperty("ui.launchConfirmed"));

        AtomicReference<String> shown = new AtomicReference<>();
        HostLog log = HostLog.open(temp.resolve("logs"), "user12", "dummy-pass".toCharArray());
        log.setListener(shown::set);
        log.info("sample", "ok", "泄漏 user12 和 dummy-pass");
        log.close();
        assertFalse(shown.get().contains("dummy-pass"));
        assertFalse(shown.get().contains("user12"));
        assertTrue(shown.get().contains("账号已隐藏"));
    }

    @Test
    void windowsScriptUsesFixedToolsAndSkipsUserConfig() throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of("package-windows.cmd"));
        assertFalse(bytes.length >= 3
                && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB
                && bytes[2] == (byte) 0xBF);
        for (byte value : bytes) {
            assertTrue((value & 0xFF) < 128, "script must stay ASCII for cmd code page 936");
        }
        String script = new String(bytes, StandardCharsets.US_ASCII);
        assertTrue(script.contains("\r\n"));
        assertFalse(script.replace("\r\n", "").contains("\n"));
        assertFalse(script.contains("-q"));
        assertFalse(script.contains("chcp"));
        assertTrue(script.contains("C:\\Program Files\\Apache\\apache-maven-3.9.16\\bin\\mvn.cmd"));
        assertTrue(script.contains("C:\\Program Files\\Microsoft"));
        assertTrue(script.contains("jdk-21*"));
        assertTrue(script.contains("jpackage.exe"));
        assertTrue(script.contains("--type app-image"));
        assertTrue(script.contains("application.example.properties"));
        assertFalse(script.contains("config\\application.properties"));
        assertTrue(script.contains("-Dmaven.repo.local=D:\\java\\m2"));
        assertTrue(script.contains("D:\\java\\2KOL2Host"));
        assertTrue(script.contains("D:\\java\\m2"));
        assertFalse(script.contains("C:\\Users"));
        assertFalse(script.contains("%TEMP%"));
        assertTrue(script.contains("2KOL2"));
    }

    private static String value(Path file, String key) throws Exception {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        return properties.getProperty(key);
    }
}
