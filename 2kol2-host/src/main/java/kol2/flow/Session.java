package kol2.flow;

import kol2.Halt;
import kol2.RunMode;
import kol2.run.DailyStart;
import kol2.run.UserMark;
import kol2.win.Desktop;
import kol2.win.DisplayCheck;
import kol2.win.WindowRef;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** 试跑或每日模式的主流程。菜单坐标未确认、画面不符时停止，不连点。 */
public final class Session {
    /** 按模式跑完，或在无法继续时停止。 */
    public void run(RunMode mode, HostServices services) {
        try {
            if (mode == RunMode.DAILY) {
                services.log.info("daily-wait", "start", "等到本地时间 " + services.config.dailyStartHour + ":00");
                DailyStart.waitUntil(services.config.dailyStartHour, services.clock, services.control);
                services.log.info("daily-wait", "ok", "已到点");
            } else {
                services.log.info("mode", "trial", "只打 1 场");
            }
            ensureWegame(services);
            login(services);
            launchGame(services);
            waitPlayable(services);
            enterMatch(services);
            int target = mode == RunMode.TRIAL ? 1 : services.config.dailyMatches;
            services.log.info("matches", "target", Integer.toString(target));
            for (int index = 1; index <= target; index++) {
                if (index > 1) {
                    rematch(services, index);
                }
                playOne(services, index);
            }
            services.log.info("matches", "ok", "完成 " + target + " 场");
        } finally {
            services.actions.releaseAll();
            services.config.clearPassword();
        }
    }

    private void ensureWegame(HostServices services) {
        if (!services.desktop.findVisible(services.config.wegameTitle).isEmpty()) {
            services.log.info("wegame-start", "ok", "窗口已在");
            return;
        }
        Halt last = null;
        int max = services.config.retryMax;
        for (int attempt = 1; attempt <= max; attempt++) {
            services.gate();
            try {
                if (!services.processes.running(services.config.wegameExe)) {
                    services.processes.start(
                            services.config.wegameDir.resolve(services.config.wegameExe),
                            services.config.wegameDir);
                }
                waitTitle(services, services.config.wegameTitle, services.config.launchTimeoutMs);
                services.log.info("wegame-start", "ok", "第" + attempt + "次");
                return;
            } catch (Halt halt) {
                if (services.control.isStopped()) {
                    throw halt;
                }
                last = halt;
                services.log.error("wegame-start", "retry", "第" + attempt + "次失败");
            }
        }
        String reason = last == null ? "" : last.getMessage();
        throw new Halt(2, "启动 WeGame 超过重试上限。" + reason);
    }

    private void login(HostServices services) {
        List<WindowRef> windows = services.desktop.findVisible(services.config.wegameTitle);
        if (windows.isEmpty()) {
            services.config.clearPassword();
            throw new Halt(2, "找不到 WeGame 窗口。");
        }
        LoginDecision.Action action = LoginDecision.decide(titles(windows), services.config.loginTitleMark);
        switch (action) {
            case ALREADY_IN -> {
                services.log.info("login", "skip", "视为已登录，不输入密码");
                services.config.clearPassword();
            }
            case STOP_RISK -> {
                services.config.clearPassword();
                throw new Halt(2, "出现验证码、二次验证或风控，已停止。请人工处理后再运行，程序不会再次提交密码。");
            }
            case UNKNOWN -> {
                services.config.clearPassword();
                throw new Halt(2, "无法判断 WeGame 是否已登录，已停止。请人工登录后再运行，程序不会向未知窗口输入密码。");
            }
            case SUBMIT_ONCE -> submitOnce(services, loginWindow(windows, services.config.loginTitleMark));
            default -> throw new Halt(2, "登录状态无法判断，已停止。");
        }
    }

    private void submitOnce(HostServices services, WindowRef window) {
        try {
            if (!services.config.loginCoordsConfirmed) {
                throw new Halt(2, "检测到登录窗口，但 login.coordsConfirmed=false。请确认账号框和密码框位置后再改成 true。这次没有输入密码。");
            }
            if (services.config.account.isBlank() || services.config.password.length == 0) {
                throw new Halt(1, "需要登录，但配置里账号或密码为空。");
            }
            clickRatio(services, window, services.config.loginAccountX, services.config.loginAccountY, "login-account");
            services.actions.typeAscii(services.config.account);
            clickRatio(services, window, services.config.loginPasswordX, services.config.loginPasswordY, "login-password");
            services.actions.typeAscii(new String(services.config.password));
            services.gate();
            services.actions.tap(KeyEvent.VK_ENTER);
            services.log.info("login", "submitted", "已提交一次，不重试");
            services.sleep(500);
            LoginDecision.Action after = LoginDecision.decide(
                    titles(services.desktop.findVisible(services.config.wegameTitle)),
                    services.config.loginTitleMark);
            if (after == LoginDecision.Action.STOP_RISK) {
                throw new Halt(2, "提交后出现验证码、二次验证或风控，已停止，不会再次提交密码。");
            }
            if (after != LoginDecision.Action.ALREADY_IN) {
                throw new Halt(2, "登录未进入主界面，已停止，不会再次提交密码。");
            }
            services.log.info("login", "ok", "已进入 WeGame");
        } finally {
            services.config.clearPassword();
        }
    }

    private void launchGame(HostServices services) {
        if (!services.config.launchConfirmed) {
            throw new Halt(2, "ui.launchConfirmed=false。请把 click.game 对准 2KOL2 后再改为 true。这次没有点击启动游戏。");
        }
        WindowRef wegame = waitTitle(services, services.config.wegameTitle, 5_000);
        clickRatio(services, wegame, services.config.gameCardX, services.config.gameCardY, "launch-game");
    }

    private void waitPlayable(HostServices services) {
        long deadline = services.clock.nowMs() + services.config.launchTimeoutMs;
        String last = "尚未出现游戏窗口";
        while (services.clock.nowMs() < deadline) {
            services.gate();
            List<WindowRef> found = services.desktop.findVisible(services.config.gameTitle);
            if (!found.isEmpty()) {
                WindowRef window = prefer(services, found);
                Desktop.ClientArea area = services.desktop.client(window);
                String problem = DisplayCheck.problem(
                        area.bordered, area.width, area.height,
                        services.config.clientWidth, services.config.clientHeight);
                if (problem == null) {
                    services.log.info("display", "ok", area.width + "x" + area.height + " 窗口模式");
                    services.log.info("display", "declared",
                            "刷新率 " + services.config.refreshHz + "，画质 " + services.config.quality
                                    + "。这两项不从外部探测，请事先在游戏内设好");
                    return;
                }
                last = problem;
            }
            services.clock.sleep(200);
        }
        throw new Halt(2, last + " 等待结束，没有点击场内菜单。");
    }

    private void enterMatch(HostServices services) {
        if (!services.config.menuConfirmed) {
            throw new Halt(2, "ui.menuConfirmed=false。请确认人机赛、王朝模式、非常简单和进入的位置后再改为 true。这次没有点菜单。");
        }
        clickChecked(services, services.config.pveX, services.config.pveY, "menu-pve");
        clickChecked(services, services.config.dynastyX, services.config.dynastyY, "menu-dynasty");
        clickChecked(services, services.config.difficultyX, services.config.difficultyY, "menu-difficulty");
        clickChecked(services, services.config.enterX, services.config.enterY, "menu-enter");
    }

    private void rematch(HostServices services, int index) {
        if (!services.config.postgameConfirmed) {
            throw new Halt(2, "下一场需要点赛后重开，但 postgame.confirmed=false，已停止，避免误点。");
        }
        clickChecked(services, services.config.rematchX, services.config.rematchY, "rematch");
        services.log.info("rematch", "ok", "准备第" + index + "场");
        services.sleep(services.config.loadWaitMs);
    }

    private void playOne(HostServices services, int index) {
        services.log.info("match", "load", "第" + index + "场等待开赛");
        services.sleep(services.config.loadWaitMs);
        long deadline = services.clock.nowMs() + services.config.matchMaxMinutes * 60_000L;
        Offense offense = new Offense();
        while (services.clock.nowMs() < deadline) {
            recheck(services);
            UserMark mark = services.signals.poll();
            if (mark != null) {
                services.log.info("match", label(mark), "第" + index + "场");
                return;
            }
            if (services.matchEnd.ended()) {
                services.log.info("match", "未判定", "第" + index + "场画面判定结束");
                return;
            }
            long before = services.clock.nowMs();
            offense.once(services);
            services.log.info("offense", "ok", "第" + index + "场一轮");
            if (services.clock.nowMs() == before) {
                throw new Halt(2, "进攻循环没有消耗时间，已停止。");
            }
        }
        throw new Halt(2, "第" + index + "场达到最长时间，仍无法确认终场，已停止。");
    }

    private void clickChecked(HostServices services, double x, double y, String step) {
        recheck(services);
        clickRatio(services, fresh(services), x, y, step);
    }

    private void recheck(HostServices services) {
        WindowRef window = fresh(services);
        Desktop.ClientArea area = services.desktop.client(window);
        String problem = DisplayCheck.problem(
                area.bordered, area.width, area.height,
                services.config.clientWidth, services.config.clientHeight);
        if (problem != null) {
            throw new Halt(2, problem);
        }
    }

    private WindowRef fresh(HostServices services) {
        List<WindowRef> found = services.desktop.findVisible(services.config.gameTitle);
        if (found.isEmpty()) {
            throw new Halt(2, "游戏窗口消失，已停止。");
        }
        return prefer(services, found);
    }

    private WindowRef prefer(HostServices services, List<WindowRef> found) {
        WindowRef fallback = found.get(0);
        for (WindowRef window : found) {
            Desktop.ClientArea area = services.desktop.client(window);
            if (DisplayCheck.problem(
                    area.bordered, area.width, area.height,
                    services.config.clientWidth, services.config.clientHeight) == null) {
                return window;
            }
            fallback = window;
        }
        return fallback;
    }

    private void clickRatio(HostServices services, WindowRef window, double xRatio, double yRatio, String step) {
        services.gate();
        services.desktop.restore(window);
        if (!services.desktop.foreground(window)) {
            throw new Halt(2, "无法把窗口放到前台，已停止。");
        }
        Desktop.ClientArea area = services.desktop.client(window);
        int localX = (int) Math.round(area.width * xRatio);
        int localY = (int) Math.round(area.height * yRatio);
        if (localX < 0 || localY < 0 || localX >= area.width || localY >= area.height) {
            throw new Halt(2, "点击位置超出客户区，已停止。");
        }
        services.actions.click(area.originX + localX, area.originY + localY);
        services.log.info(step, "click", "已点击");
        services.sleep(services.config.menuDelayMs);
    }

    private WindowRef waitTitle(HostServices services, String title, long timeoutMs) {
        long deadline = services.clock.nowMs() + timeoutMs;
        while (services.clock.nowMs() < deadline) {
            services.gate();
            List<WindowRef> found = services.desktop.findVisible(title);
            if (!found.isEmpty()) {
                return found.get(0);
            }
            services.clock.sleep(200);
        }
        throw new Halt(2, "超时未出现窗口。");
    }

    private static List<String> titles(List<WindowRef> windows) {
        List<String> titles = new ArrayList<>();
        for (WindowRef window : windows) {
            titles.add(window.title);
        }
        return titles;
    }

    private static WindowRef loginWindow(List<WindowRef> windows, String mark) {
        for (WindowRef window : windows) {
            if (window.title != null && mark != null && window.title.contains(mark)) {
                return window;
            }
        }
        return windows.get(0);
    }

    private static String label(UserMark mark) {
        return switch (mark) {
            case WIN -> "胜";
            case LOSS -> "负";
            case DONE -> "未判定";
        };
    }
}
