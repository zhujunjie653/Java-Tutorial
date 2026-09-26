package kol2;

final class ConfigCasesTestBase {
    private ConfigCasesTestBase() {
    }

    static String properties(String wegameDir, String logDir, String lock) {
        return """
                wegame.dir=%s
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
                match.launchTimeoutSec=1
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
                log.dir=%s
                instance.lock=%s
                template.matchEnd=
                """.formatted(wegameDir, logDir, lock);
    }
}
