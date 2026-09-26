package kol2.flow;

import kol2.config.KeyBindings;

/**
 * 一轮进攻。
 * 假定镜头在身后，向前键朝进攻方向；没球时不按防守键，只松开等待。
 */
public final class Offense {
    /** 运球到前场，呼叫内切，传球，投篮，然后等待。 */
    public void once(HostServices services) {
        services.gate();
        KeyBindings keys = services.config.keys;
        if (keys.takeControl != 0) {
            tap(services, keys.takeControl);
        }
        holdForward(services, keys);
        tap(services, keys.callCut);
        services.sleep(services.config.afterCutMs);
        tap(services, keys.pass);
        services.sleep(services.config.afterPassMs);
        tap(services, keys.shoot);
        services.actions.releaseAll();
        services.sleep(services.config.idleMs);
    }

    private void holdForward(HostServices services, KeyBindings keys) {
        pressPair(services, keys);
        try {
            long left = services.config.dribbleMs;
            while (left > 0) {
                if (services.control.isPaused() || services.control.isStopped()) {
                    services.actions.releaseAll();
                }
                services.gate();
                if (!services.actions.isDown(keys.sprint)) {
                    services.actions.keyDown(keys.sprint);
                }
                if (!services.actions.isDown(keys.moveForward)) {
                    services.actions.keyDown(keys.moveForward);
                }
                long slice = Math.min(100, left);
                services.clock.sleep(slice);
                left -= slice;
            }
        } finally {
            services.actions.keyUp(keys.moveForward);
            services.actions.keyUp(keys.sprint);
        }
    }

    private void pressPair(HostServices services, KeyBindings keys) {
        services.actions.keyDown(keys.sprint);
        services.actions.keyDown(keys.moveForward);
    }

    private void tap(HostServices services, int vk) {
        services.gate();
        services.actions.tap(vk);
    }
}
