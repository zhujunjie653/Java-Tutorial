package kol2.config;

import java.nio.file.Path;
import java.util.Arrays;

/** 本机配置。密码只留在内存里，用完要清掉。 */
public final class AppConfig {
    public final Path wegameDir;
    public final String wegameExe;
    public final String wegameTitle;
    public final String gameTitle;
    public final String account;
    public final char[] password;
    public final boolean loginCoordsConfirmed;
    public final String loginTitleMark;
    public final int clientWidth;
    public final int clientHeight;
    public final int refreshHz;
    public final String quality;
    public final int dailyMatches;
    public final int dailyStartHour;
    public final int retryMax;
    public final long menuDelayMs;
    public final long dribbleMs;
    public final long afterCutMs;
    public final long afterPassMs;
    public final long idleMs;
    public final long loadWaitMs;
    public final long launchTimeoutMs;
    public final int matchMaxMinutes;
    public final boolean launchConfirmed;
    public final boolean menuConfirmed;
    public final boolean postgameConfirmed;
    public final KeyBindings keys;
    public final double gameCardX;
    public final double gameCardY;
    public final double loginAccountX;
    public final double loginAccountY;
    public final double loginPasswordX;
    public final double loginPasswordY;
    public final double pveX;
    public final double pveY;
    public final double dynastyX;
    public final double dynastyY;
    public final double difficultyX;
    public final double difficultyY;
    public final double enterX;
    public final double enterY;
    public final double rematchX;
    public final double rematchY;
    public final Path logDir;
    public final Path lockFile;
    public final Path templateMatchEnd;

    public AppConfig(
            Path wegameDir,
            String wegameExe,
            String wegameTitle,
            String gameTitle,
            String account,
            char[] password,
            boolean loginCoordsConfirmed,
            String loginTitleMark,
            int clientWidth,
            int clientHeight,
            int refreshHz,
            String quality,
            int dailyMatches,
            int dailyStartHour,
            int retryMax,
            long menuDelayMs,
            long dribbleMs,
            long afterCutMs,
            long afterPassMs,
            long idleMs,
            long loadWaitMs,
            long launchTimeoutMs,
            int matchMaxMinutes,
            boolean launchConfirmed,
            boolean menuConfirmed,
            boolean postgameConfirmed,
            KeyBindings keys,
            double gameCardX,
            double gameCardY,
            double loginAccountX,
            double loginAccountY,
            double loginPasswordX,
            double loginPasswordY,
            double pveX,
            double pveY,
            double dynastyX,
            double dynastyY,
            double difficultyX,
            double difficultyY,
            double enterX,
            double enterY,
            double rematchX,
            double rematchY,
            Path logDir,
            Path lockFile,
            Path templateMatchEnd) {
        this.wegameDir = wegameDir;
        this.wegameExe = wegameExe;
        this.wegameTitle = wegameTitle;
        this.gameTitle = gameTitle;
        this.account = account;
        this.password = password;
        this.loginCoordsConfirmed = loginCoordsConfirmed;
        this.loginTitleMark = loginTitleMark;
        this.clientWidth = clientWidth;
        this.clientHeight = clientHeight;
        this.refreshHz = refreshHz;
        this.quality = quality;
        this.dailyMatches = dailyMatches;
        this.dailyStartHour = dailyStartHour;
        this.retryMax = retryMax;
        this.menuDelayMs = menuDelayMs;
        this.dribbleMs = dribbleMs;
        this.afterCutMs = afterCutMs;
        this.afterPassMs = afterPassMs;
        this.idleMs = idleMs;
        this.loadWaitMs = loadWaitMs;
        this.launchTimeoutMs = launchTimeoutMs;
        this.matchMaxMinutes = matchMaxMinutes;
        this.launchConfirmed = launchConfirmed;
        this.menuConfirmed = menuConfirmed;
        this.postgameConfirmed = postgameConfirmed;
        this.keys = keys;
        this.gameCardX = gameCardX;
        this.gameCardY = gameCardY;
        this.loginAccountX = loginAccountX;
        this.loginAccountY = loginAccountY;
        this.loginPasswordX = loginPasswordX;
        this.loginPasswordY = loginPasswordY;
        this.pveX = pveX;
        this.pveY = pveY;
        this.dynastyX = dynastyX;
        this.dynastyY = dynastyY;
        this.difficultyX = difficultyX;
        this.difficultyY = difficultyY;
        this.enterX = enterX;
        this.enterY = enterY;
        this.rematchX = rematchX;
        this.rematchY = rematchY;
        this.logDir = logDir;
        this.lockFile = lockFile;
        this.templateMatchEnd = templateMatchEnd;
    }

    /** 抹掉内存中的密码。 */
    public void clearPassword() {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}
