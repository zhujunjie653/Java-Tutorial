package kol2.run;

import java.util.Locale;
import java.util.Optional;

/** 把控制台一行解析成暂停、继续、停止或本场标记。 */
public final class ConsoleCommands {
    /** 一种控制命令。mark 只在标记本场结果时有值。 */
    public record Command(Kind kind, UserMark mark) {
        public enum Kind {
            PAUSE,
            RESUME,
            STOP,
            MARK
        }
    }

    private ConsoleCommands() {
    }

    /** 无法识别时返回空，调用方应忽略该行。 */
    public static Optional<Command> parse(String line) {
        if (line == null) {
            return Optional.empty();
        }
        String token = line.trim().toLowerCase(Locale.ROOT);
        return switch (token) {
            case "p", "pause" -> Optional.of(new Command(Command.Kind.PAUSE, null));
            case "c", "continue" -> Optional.of(new Command(Command.Kind.RESUME, null));
            case "q", "quit", "stop" -> Optional.of(new Command(Command.Kind.STOP, null));
            case "done" -> Optional.of(new Command(Command.Kind.MARK, UserMark.DONE));
            case "win" -> Optional.of(new Command(Command.Kind.MARK, UserMark.WIN));
            case "loss" -> Optional.of(new Command(Command.Kind.MARK, UserMark.LOSS));
            default -> Optional.empty();
        };
    }
}
