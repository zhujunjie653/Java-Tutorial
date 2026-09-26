package kol2.flow;

import kol2.run.Clock;
import kol2.win.Actions;
import kol2.win.Desktop;
import kol2.win.ImageDiff;
import kol2.win.WindowRef;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** 用一张本地截图粗略比对终场。比对失败就当作还没结束。 */
public final class TemplateProbe implements MatchEndProbe {
    private static final double THRESHOLD = 0.12;
    private final Path file;
    private final Desktop desktop;
    private final Actions actions;
    private final String gameTitle;
    private final Clock clock;
    private long nextCheck;

    public TemplateProbe(Path file, Desktop desktop, Actions actions, String gameTitle, Clock clock) {
        this.file = file;
        this.desktop = desktop;
        this.actions = actions;
        this.gameTitle = gameTitle;
        this.clock = clock;
    }

    @Override
    public boolean ended() {
        if (clock.nowMs() < nextCheck || file == null || !Files.isRegularFile(file)) {
            return false;
        }
        nextCheck = clock.nowMs() + 5000;
        List<WindowRef> windows = desktop.findVisible(gameTitle);
        if (windows.isEmpty()) {
            return false;
        }
        Desktop.ClientArea area = desktop.client(windows.get(0));
        BufferedImage shot = actions.capture(area.originX, area.originY, area.width, area.height);
        if (shot == null) {
            return false;
        }
        try {
            BufferedImage template = ImageIO.read(file.toFile());
            if (template == null) {
                return false;
            }
            return ImageDiff.mad(shot, template) < THRESHOLD;
        } catch (IOException ex) {
            return false;
        }
    }
}
