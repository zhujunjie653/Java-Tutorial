package kol2;

import kol2.win.Desktop;
import kol2.win.KeyCodes;
import kol2.win.WindowRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionFlowTest {
    @TempDir
    Path temp;

    @Test
    void refusesNonWindowsWithoutTouchingProcesses() {
        Fakes.FakeRuntime runtime = new Fakes.FakeRuntime();
        runtime.windows = false;
        int code = new Kol2HostApp().run(new String[0], runtime);
        assertEquals(2, code);
        assertEquals(0, runtime.processes.starts);
        assertTrue(runtime.actions.events.isEmpty());
    }

    @Test
    void trialPlaysOneOffenseAndDoesNotRematch() throws Exception {
        Fakes.FakeRuntime runtime = ready(true, true, false);
        int shoot = KeyCodes.vk("J");
        runtime.probe = new Fakes.EndAfterNewShoot(runtime.actions, shoot);
        int code = new Kol2HostApp().run(new String[]{"--config", config(temp).toString()}, runtime);
        assertEquals(0, code);
        String log = logText();
        assertTrue(log.contains("模式=trial"));
        assertTrue(log.contains("完成 1 场"));
        assertFalse(log.contains("step=rematch"));
        assertEquals(1, runtime.actions.taps(shoot));
        assertEquals(1, runtime.actions.taps(KeyCodes.vk("Q")));
        assertEquals(1, runtime.actions.taps(KeyCodes.vk("K")));
        assertTrue(indexOf(runtime, "down:" + KeyCodes.vk("SHIFT"))
                < indexOf(runtime, "tap:" + KeyCodes.vk("Q")));
        assertEquals(4, runtime.actions.events.stream().filter(event -> event.equals("click:1290,740")).count());
        assertTrue(log.contains("2560x1440"));
        assertFalse(log.contains("dummy"));
    }

    @Test
    void wrongClientSizeDoesNotClickTheLobby() throws Exception {
        Fakes.FakeRuntime runtime = ready(true, true, false);
        runtime.desktop.windows.clear();
        runtime.desktop.areas.clear();
        runtime.desktop.add("WeGame", new Desktop.ClientArea(true, 1000, 800, 0, 0));
        runtime.desktop.add("NBA2KOL2", new Desktop.ClientArea(true, 1920, 1080, 10, 20));
        int code = new Kol2HostApp().run(args(), runtime);
        assertEquals(2, code);
        String log = logText();
        assertTrue(log.contains("1920x1080"));
        assertFalse(log.contains("step=menu-pve"));
        assertEquals(1, runtime.actions.events.stream().filter(event -> event.startsWith("click:")).count());
    }

    @Test
    void borderlessWindowStops() throws Exception {
        Fakes.FakeRuntime runtime = ready(true, true, false);
        replaceGame(runtime, new Desktop.ClientArea(false, 2560, 1440, 10, 20));
        int code = new Kol2HostApp().run(args(), runtime);
        assertEquals(2, code);
        assertTrue(logText().contains("标题栏"));
        assertFalse(logText().contains("step=menu-pve"));
    }

    @Test
    void unconfirmedLaunchDoesNotClick() throws Exception {
        Fakes.FakeRuntime runtime = ready(false, false, false);
        int code = new Kol2HostApp().run(args(), runtime);
        assertEquals(2, code);
        assertTrue(runtime.actions.events.stream().noneMatch(event -> event.startsWith("click:")));
        assertTrue(logText().contains("ui.launchConfirmed=false"));
        assertEquals(0, runtime.actions.typeCalls);
    }

    @Test
    void loginSubmitsOnceAndStopsOnCaptchaAfter() throws Exception {
        Path file = config(temp);
        String text = Files.readString(file)
                .replace("login.coordsConfirmed=false", "login.coordsConfirmed=true")
                .replace("account=", "account=user12")
                .replace("password=", "password=dummy-pass")
                .replace("ui.launchConfirmed=true", "ui.launchConfirmed=false");
        Files.writeString(file, text);
        Fakes.FakeRuntime runtime = stage(false);
        WindowRef login = runtime.desktop.windows.get(0);
        login.title = "WeGame登录";
        runtime.actions.onTap = vk -> {
            if (vk == KeyCodes.vk("ENTER")) {
                login.title = "WeGame安全验证";
            }
        };
        int code = new Kol2HostApp().run(args(), runtime);
        assertEquals(2, code);
        assertEquals(2, runtime.actions.typeCalls);
        assertEquals(1, runtime.actions.taps(KeyCodes.vk("ENTER")));
        String log = logText();
        assertTrue(log.contains("不会再次提交密码"));
        assertFalse(log.contains("dummy-pass"));
        assertFalse(log.contains("user12"));
    }

    @Test
    void alreadyLoggedInDoesNotTypePassword() throws Exception {
        Path file = config(temp);
        Files.writeString(file, Files.readString(file)
                .replace("account=", "account=user12")
                .replace("password=", "password=dummy-pass"));
        Fakes.FakeRuntime runtime = stage(false);
        int code = new Kol2HostApp().run(args(), runtime);
        assertEquals(2, code);
        assertEquals(0, runtime.actions.typeCalls);
        assertTrue(logText().contains("不输入密码"));
        assertFalse(logText().contains("dummy-pass"));
    }

    @Test
    void dailyStopsBeforeSecondMatchWhenRematchIsUnconfirmed() throws Exception {
        Fakes.FakeRuntime runtime = ready(true, true, false);
        runtime.probe = new Fakes.EndAfterNewShoot(runtime.actions, KeyCodes.vk("J"));
        int code = new Kol2HostApp().run(new String[]{"daily", "--config", config(temp).toString()}, runtime);
        assertEquals(2, code);
        String log = logText();
        assertTrue(log.contains("模式=daily"));
        assertTrue(log.contains("postgame.confirmed=false"));
        assertEquals(1, runtime.actions.taps(KeyCodes.vk("J")));
        assertFalse(log.contains("完成 5 场"));
    }

    @Test
    void dailyContinuesAfterLossUntilFive() throws Exception {
        Path file = config(temp);
        Files.writeString(file, Files.readString(file).replace("postgame.confirmed=false", "postgame.confirmed=true"));
        Fakes.FakeRuntime runtime = ready(true, true, true);
        int shoot = KeyCodes.vk("J");
        Fakes.EndAfterNewShoot probe = new Fakes.EndAfterNewShoot(runtime.actions, shoot);
        runtime.probe = probe;
        runtime.actions.onTap = vk -> {
            if (vk == shoot && runtime.actions.taps(shoot) == 1) {
                runtime.signals.mark(kol2.run.UserMark.LOSS);
                probe.note(1);
            }
        };
        int code = new Kol2HostApp().run(new String[]{"daily", "--config", file.toString()}, runtime);
        assertEquals(0, code);
        String log = logText();
        assertTrue(log.contains("result=负"));
        assertTrue(log.contains("完成 5 场"));
        assertEquals(5, runtime.actions.taps(shoot));
        assertEquals(4, count(log, "step=rematch result=ok"));
    }

    @Test
    void wegameStartRetriesThenStops() throws Exception {
        Fakes.FakeRuntime runtime = stage(false);
        runtime.desktop.windows.clear();
        runtime.processes.failAlways = true;
        int code = new Kol2HostApp().run(args(), runtime);
        assertEquals(2, code);
        assertEquals(2, runtime.processes.starts);
        assertTrue(logText().contains("超过重试上限"));
        assertTrue(runtime.actions.events.stream().noneMatch(event -> event.startsWith("click:")));
    }

    private Fakes.FakeRuntime ready(boolean launch, boolean menu, boolean postgame) throws Exception {
        Path file = config(temp);
        String text = Files.readString(file)
                .replace("ui.launchConfirmed=false", "ui.launchConfirmed=" + launch)
                .replace("ui.menuConfirmed=false", "ui.menuConfirmed=" + menu)
                .replace("postgame.confirmed=false", "postgame.confirmed=" + postgame);
        Files.writeString(file, text);
        return stage(true);
    }

    private Fakes.FakeRuntime stage(boolean withGame) throws Exception {
        config(temp);
        Fakes.FakeRuntime runtime = new Fakes.FakeRuntime();
        runtime.desktop.add("WeGame", new Desktop.ClientArea(true, 1000, 800, 0, 0));
        if (withGame) {
            runtime.desktop.add("NBA2KOL2", new Desktop.ClientArea(true, 2560, 1440, 10, 20));
        }
        return runtime;
    }

    private void replaceGame(Fakes.FakeRuntime runtime, Desktop.ClientArea area) {
        runtime.desktop.windows.removeIf(window -> window.title.contains("NBA"));
        runtime.desktop.add("NBA2KOL2", area);
    }

    private Path config(Path root) throws Exception {
        Path dir = root.resolve("WeGame");
        Files.createDirectories(dir);
        Path exe = dir.resolve("wegame.exe");
        if (!Files.exists(exe)) {
            Files.writeString(exe, "");
        }
        Path logs = root.resolve("logs");
        Path lock = root.resolve("runtime/instance.lock");
        Path file = root.resolve("application.properties");
        if (!Files.exists(file)) {
            String body = ConfigCasesTestBase.properties(
                    dir.toString().replace('\\', '/'),
                    logs.toString().replace('\\', '/'),
                    lock.toString().replace('\\', '/'));
            Files.writeString(file, body, StandardCharsets.UTF_8);
        }
        return file;
    }

    private String[] args() {
        return new String[]{"--config", temp.resolve("application.properties").toString()};
    }

    private String logText() throws Exception {
        return Files.readString(temp.resolve("logs/kol2-host.log"));
    }

    private static int indexOf(Fakes.FakeRuntime runtime, String event) {
        return runtime.actions.events.indexOf(event);
    }

    private static int count(String text, String token) {
        int count = 0;
        int from = 0;
        while (true) {
            int found = text.indexOf(token, from);
            if (found < 0) {
                return count;
            }
            count++;
            from = found + token.length();
        }
    }
}
