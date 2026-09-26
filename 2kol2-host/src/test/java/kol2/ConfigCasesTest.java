package kol2;

import kol2.config.AppConfig;
import kol2.config.ConfigLoader;
import kol2.run.ConsoleCommands;
import kol2.run.Control;
import kol2.run.DailyStart;
import kol2.run.HostLog;
import kol2.run.SignalBox;
import kol2.run.SingleInstance;
import kol2.run.UserMark;
import kol2.win.AsciiTyping;
import kol2.win.DisplayCheck;
import kol2.win.ImageDiff;
import kol2.win.KeyCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigCasesTest {
    @TempDir
    Path temp;

    @Test
    void missingFileSaysWhatIsMissing() {
        Halt halt = assertThrows(Halt.class, () -> ConfigLoader.load(temp.resolve("no.properties")));
        assertEquals(1, halt.exitCode);
        assertTrue(halt.getMessage().contains("缺少配置文件"));
    }

    @Test
    void missingWegameDirectoryIsRejected() {
        Path file = writeBase(temp, "wegame.dir=" + temp.resolve("missing").toString().replace('\\', '/'));
        Halt halt = assertThrows(Halt.class, () -> ConfigLoader.load(file));
        assertEquals(1, halt.exitCode);
        assertTrue(halt.getMessage().contains("目录不存在"));
    }

    @Test
    void fullscreenModeIsRejected() throws Exception {
        Path file = writeReady(temp);
        String text = Files.readString(file);
        Files.writeString(file, text.replace("display.mode=window", "display.mode=fullscreen"));
        Halt halt = assertThrows(Halt.class, () -> ConfigLoader.load(file));
        assertTrue(halt.getMessage().contains("window"));
    }

    @Test
    void passwordCharacterIsNotEchoed() throws Exception {
        Path file = writeReady(temp);
        String text = Files.readString(file);
        Files.writeString(file, text.replace("password=", "password=你"));
        Halt halt = assertThrows(Halt.class, () -> ConfigLoader.load(file));
        assertFalse(halt.getMessage().contains("你"));
        assertTrue(halt.getMessage().contains("未提交登录"));
    }

    @Test
    void exampleFileLoadsWhenPathExists() throws Exception {
        Path root = temp.resolve("game");
        Files.createDirectories(root);
        Files.writeString(root.resolve("wegame.exe"), "");
        String example = Files.readString(Path.of("config", "application.example.properties"));
        String rewritten = example.replace(
                "wegame.dir=D:/Program Files (x86)/WeGame",
                "wegame.dir=" + root.toString().replace('\\', '/'));
        rewritten = rewritten.replace("log.dir=logs", "log.dir=" + temp.resolve("logs").toString().replace('\\', '/'));
        rewritten = rewritten.replace(
                "instance.lock=runtime/instance.lock",
                "instance.lock=" + temp.resolve("runtime/instance.lock").toString().replace('\\', '/'));
        Path file = temp.resolve("application.properties");
        Files.writeString(file, rewritten);
        AppConfig config = ConfigLoader.load(file);
        assertEquals(2560, config.clientWidth);
        assertEquals(1440, config.clientHeight);
        assertEquals(120, config.refreshHz);
        assertEquals("高", config.quality);
        assertEquals(5, config.dailyMatches);
        assertEquals(9, config.dailyStartHour);
        assertFalse(config.loginCoordsConfirmed);
        assertFalse(config.launchConfirmed);
        assertFalse(config.menuConfirmed);
        assertFalse(config.postgameConfirmed);
        assertEquals("", config.account);
        assertEquals(0, config.password.length);
        assertEquals(KeyCodes.vk("W"), config.keys.moveForward);
        assertEquals(KeyCodes.vk("SHIFT"), config.keys.sprint);
        assertEquals(KeyCodes.vk("Q"), config.keys.callCut);
        assertEquals(KeyCodes.vk("K"), config.keys.pass);
        assertEquals(KeyCodes.vk("J"), config.keys.shoot);
        assertEquals(0, config.keys.takeControl);
        assertFalse(example.contains("password=1"));
        assertTrue(example.contains("password="));
    }

    @Test
    void defaultArgsAreTrial() {
        Args args = Args.parse(new String[0]);
        assertEquals(RunMode.TRIAL, args.mode);
        assertEquals(RunMode.DAILY, Args.parse(new String[]{"daily"}).mode);
        assertEquals(1, assertThrows(Halt.class, () -> Args.parse(new String[]{"rank"})).exitCode);
    }

    @Test
    void displayCheckStopsOnFullscreenOrWrongSize() {
        assertNull(DisplayCheck.problem(true, 2560, 1440, 2560, 1440));
        assertTrue(DisplayCheck.problem(false, 2560, 1440, 2560, 1440).contains("标题栏"));
        assertTrue(DisplayCheck.problem(true, 1920, 1080, 2560, 1440).contains("1920x1080"));
    }

    @Test
    void loginDecisionDoesNotSubmitTwiceOnRisk() {
        assertEquals(kol2.flow.LoginDecision.Action.ALREADY_IN,
                kol2.flow.LoginDecision.decide(List.of("WeGame"), "登录"));
        assertEquals(kol2.flow.LoginDecision.Action.SUBMIT_ONCE,
                kol2.flow.LoginDecision.decide(List.of("WeGame登录"), "登录"));
        assertEquals(kol2.flow.LoginDecision.Action.STOP_RISK,
                kol2.flow.LoginDecision.decide(List.of("WeGame安全验证"), "登录"));
        assertEquals(kol2.flow.LoginDecision.Action.UNKNOWN,
                kol2.flow.LoginDecision.decide(List.of("记事本"), "登录"));
    }

    @Test
    void asciiPlanUsesShiftForUppercaseAndRejectsNonAscii() {
        List<AsciiTyping.Chord> chords = AsciiTyping.plan("A1");
        assertTrue(chords.get(0).shift());
        assertFalse(chords.get(1).shift());
        assertThrows(Halt.class, () -> AsciiTyping.plan("密"));
    }

    @Test
    void logRedactsAccountAndPassword(@TempDir Path dir) throws Exception {
        HostLog log = HostLog.open(dir, "user12", "dummy-pass".toCharArray());
        log.info("sample", "ok", "泄漏 user12 和 dummy-pass");
        log.close();
        String text = Files.readString(dir.resolve("kol2-host.log"));
        assertFalse(text.contains("user12"));
        assertFalse(text.contains("dummy-pass"));
        assertTrue(text.contains("账号已隐藏"));
        assertTrue(text.contains("***"));
        assertTrue(text.contains("step=sample"));
    }

    @Test
    void secondInstanceIsRejected(@TempDir Path dir) {
        Path lock = dir.resolve("instance.lock");
        SingleInstance first = SingleInstance.acquire(lock);
        Halt halt = assertThrows(Halt.class, () -> SingleInstance.acquire(lock));
        assertTrue(halt.getMessage().contains("已有一个"));
        first.close();
        SingleInstance second = SingleInstance.acquire(lock);
        second.close();
    }

    @Test
    void pauseThenStopReleasesAndUnblocks() throws Exception {
        Control control = new Control();
        Fakes.RecordingActions actions = new Fakes.RecordingActions();
        actions.keyDown(KeyCodes.vk("J"));
        control.pause();
        Thread waiter = new Thread(() -> {
            try {
                new kol2.flow.HostServices(
                        null, control, null, null, actions, null, new Fakes.FakeClock(0),
                        new SignalBox(), () -> false, null).gate();
            } catch (Halt ignored) {
                // 停止即退出等待。
            }
        });
        waiter.start();
        long until = System.currentTimeMillis() + 1000;
        while (!actions.held.isEmpty() && System.currentTimeMillis() < until) {
            Thread.sleep(10);
        }
        assertTrue(actions.held.isEmpty());
        control.stop();
        waiter.join(1000);
        assertFalse(waiter.isAlive());
    }

    @Test
    void dailyWaitsUntilLocalNine() {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime eight = ZonedDateTime.of(2026, 9, 26, 8, 30, 0, 0, zone);
        Fakes.FakeClock clock = new Fakes.FakeClock(eight.toInstant().toEpochMilli());
        DailyStart.waitUntil(9, clock, new Control());
        ZonedDateTime after = ZonedDateTime.ofInstant(Instant.ofEpochMilli(clock.now), zone);
        assertEquals(9, after.getHour());
        assertEquals(0, after.getMinute());
        long stayed = clock.now;
        DailyStart.waitUntil(9, clock, new Control());
        assertEquals(stayed, clock.now);
    }

    @Test
    void consoleCommandsCoverPauseStopAndResult() {
        assertEquals(ConsoleCommands.Command.Kind.PAUSE, ConsoleCommands.parse("p").orElseThrow().kind());
        assertEquals(ConsoleCommands.Command.Kind.STOP, ConsoleCommands.parse("q").orElseThrow().kind());
        assertEquals(UserMark.LOSS, ConsoleCommands.parse("loss").orElseThrow().mark());
        assertTrue(ConsoleCommands.parse("你好").isEmpty());
    }

    @Test
    void identicalImagesMatch() {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        assertEquals(0, ImageDiff.mad(image, image), 0.001);
        BufferedImage other = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        other.setRGB(0, 0, 0xffffff);
        assertTrue(ImageDiff.mad(image, other) > 0);
    }

    private static Path writeBase(Path temp, String wegameLine) {
        try {
            Path file = temp.resolve("application.properties");
            Files.writeString(file, base().replace("wegame.dir=D:/WeGame", wegameLine), StandardCharsets.UTF_8);
            return file;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Path writeReady(Path temp) throws Exception {
        Path root = temp.resolve("WeGame");
        Files.createDirectories(root);
        Files.writeString(root.resolve("wegame.exe"), "");
        return writeBase(temp, "wegame.dir=" + root.toString().replace('\\', '/'));
    }

    private static String base() {
        return """
                wegame.dir=D:/WeGame
                wegame.exe=wegame.exe
                wegame.windowTitle=WeGame
                game.windowTitle=NBA
                account=
                password=
                login.coordsConfirmed=false
                login.titleContains=登录
                display.mode=window
                display.width=2560
                display.height=1440
                display.refreshHz=120
                display.quality=高
                daily.matches=5
                daily.startHour=9
                retry.max=2
                ui.launchConfirmed=false
                ui.menuConfirmed=false
                postgame.confirmed=false
                ui.menuDelayMs=0
                match.loadWaitSec=0
                match.launchTimeoutSec=5
                match.maxMinutes=10
                match.dribbleMs=100
                match.afterCutMs=0
                match.afterPassMs=0
                match.idleMs=0
                keys.moveForward=W
                keys.sprint=SHIFT
                keys.callCut=Q
                keys.pass=K
                keys.shoot=J
                keys.takeControl=
                click.game.x=0.5
                click.game.y=0.5
                click.login.account.x=0.5
                click.login.account.y=0.5
                click.login.password.x=0.5
                click.login.password.y=0.5
                click.pve.x=0.5
                click.pve.y=0.5
                click.dynasty.x=0.5
                click.dynasty.y=0.5
                click.difficulty.x=0.5
                click.difficulty.y=0.5
                click.enter.x=0.5
                click.enter.y=0.5
                click.rematch.x=0.5
                click.rematch.y=0.5
                log.dir=logs
                instance.lock=runtime/instance.lock
                template.matchEnd=
                """;
    }
}
