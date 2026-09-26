package kol2.flow;

import java.util.List;
import java.util.Locale;

/** 只根据窗口标题决定要不要输入一次密码。 */
public final class LoginDecision {
    /** 登录窗口对应的下一步。 */
    public enum Action {
        ALREADY_IN,
        SUBMIT_ONCE,
        STOP_RISK,
        UNKNOWN
    }

    private LoginDecision() {
    }

    /** 有风控标题就停止。有登录标题才允许输入一次。 */
    public static Action decide(List<String> titles, String loginMark) {
        boolean shell = false;
        boolean login = false;
        for (String title : titles) {
            if (title == null || title.isBlank()) {
                continue;
            }
            if (risk(title)) {
                return Action.STOP_RISK;
            }
            if (loginMark != null && !loginMark.isBlank() && title.contains(loginMark)) {
                login = true;
            }
            if (title.toLowerCase(Locale.ROOT).contains("wegame")) {
                shell = true;
            }
        }
        if (login) {
            return Action.SUBMIT_ONCE;
        }
        if (shell) {
            return Action.ALREADY_IN;
        }
        return Action.UNKNOWN;
    }

    private static boolean risk(String title) {
        return title.contains("验证码")
                || title.contains("二次验证")
                || title.contains("风控")
                || title.contains("安全验证")
                || title.contains("扫码");
    }
}
