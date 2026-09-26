package kol2.ui;

import kol2.AppHome;
import kol2.RunMode;
import kol2.flow.HostRuntime;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** 简体中文主窗口。关闭时停止任务并松开按键。 */
public final class HostFrame {
    private final HostRuntime runtime;
    private final HostRun run = new HostRun();
    private final ConfigForm form = new ConfigForm();
    private final JFrame frame = new JFrame("2KOL2 托管");
    private final JTextField wegameDir = new JTextField(28);
    private final JTextField account = new JTextField(28);
    private final JPasswordField password = new JPasswordField(28);
    private final JTextField width = new JTextField(8);
    private final JTextField height = new JTextField(8);
    private final JCheckBox loginConfirmed = new JCheckBox("登录框位置已核对");
    private final JCheckBox launchConfirmed = new JCheckBox("游戏图标位置已核对");
    private final JCheckBox menuConfirmed = new JCheckBox("菜单点击位置已核对");
    private final JCheckBox postgameConfirmed = new JCheckBox("赛后重开位置已核对（每天 5 场必须打开）");
    private final Map<String, JTextField> advancedFields = new LinkedHashMap<>();
    private final JPanel advanced = new JPanel(new GridBagLayout());
    private final JTextArea log = new JTextArea(12, 40);
    private final JButton trial = new JButton("试跑 1 场");
    private final JButton daily = new JButton("每天 9:00 后打 5 场");
    private final JButton pause = new JButton("暂停");
    private final JButton resume = new JButton("继续");
    private final JButton stop = new JButton("停止");
    private final JButton save = new JButton("保存配置");

    private HostFrame(HostRuntime runtime) {
        this.runtime = runtime;
    }

    /** 打开主窗口。没有显示器时直接返回。 */
    public static void launch(HostRuntime runtime) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("当前环境没有图形界面，不能打开窗口。");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // 用默认外观即可。
            }
            new HostFrame(runtime).show();
        });
    }

    private void show() {
        Font font = uiFont();
        frame.setFont(font);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent event) {
                closeWindow();
            }
        });
        loadForm();
        frame.setContentPane(content(font));
        frame.getRootPane().setDefaultButton(trial);
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
        setRunning(false);
        frame.setVisible(true);
    }

    private JComponent content(Font font) {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formPane(font), logPane(font));
        split.setResizeWeight(0.55);
        root.add(split, BorderLayout.CENTER);
        root.add(buttons(font), BorderLayout.SOUTH);
        return root;
    }

    private JScrollPane formPane(Font font) {
        JPanel formPanel = new JPanel(new GridBagLayout());
        int row = 0;
        row = row(formPanel, row, "说明", label("尚未在真实客户端验证。只通过官方窗口操作。", font), font);
        row = row(formPanel, row, "WeGame 路径", wegameDir, font);
        row = row(formPanel, row, "QQ 号", account, font);
        row = row(formPanel, row, "密码", password, font);
        JPanel size = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        size.add(width);
        size.add(new JLabel("x"));
        size.add(height);
        row = row(formPanel, row, "客户区分辨率", size, font);
        row = row(formPanel, row, "注意", label("没核对点击位置就不要打开确认开关。", font), font);
        JButton toggle = new JButton("高级：点击比例和按键");
        toggle.setFont(font);
        toggle.addActionListener(event -> {
            advanced.setVisible(!advanced.isVisible());
            frame.revalidate();
        });
        row = row(formPanel, row, "", toggle, font);
        fillAdvanced(font);
        advanced.setVisible(false);
        GridBagConstraints advancedConstraints = constraints(0, row);
        advancedConstraints.gridwidth = 2;
        advancedConstraints.weightx = 1;
        advancedConstraints.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(advanced, advancedConstraints);
        applyFont(formPanel, font);
        return new JScrollPane(formPanel);
    }

    private void fillAdvanced(Font font) {
        int row = 0;
        row = checkRow(row, loginConfirmed, font);
        row = checkRow(row, launchConfirmed, font);
        row = checkRow(row, menuConfirmed, font);
        row = checkRow(row, postgameConfirmed, font);
        for (String[] item : ConfigForm.ADVANCED) {
            JTextField field = new JTextField(12);
            field.setFont(font);
            advancedFields.put(item[0], field);
            row = row(advanced, row, item[1], field, font);
        }
    }

    private int checkRow(int row, JCheckBox box, Font font) {
        box.setFont(font);
        GridBagConstraints constraints = constraints(0, row);
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        advanced.add(box, constraints);
        return row + 1;
    }

    private JScrollPane logPane(Font font) {
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setLineWrap(true);
        JScrollPane pane = new JScrollPane(log);
        pane.setBorder(BorderFactory.createTitledBorder("日志"));
        return pane;
    }

    private JPanel buttons(Font font) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        trial.addActionListener(event -> start(RunMode.TRIAL));
        daily.addActionListener(event -> start(RunMode.DAILY));
        pause.addActionListener(event -> run.pause());
        resume.addActionListener(event -> run.resume());
        stop.addActionListener(event -> run.stop());
        save.addActionListener(event -> saveForm(false));
        for (JButton button : new JButton[] {trial, daily, pause, resume, stop, save}) {
            button.setFont(font);
            panel.add(button);
        }
        return panel;
    }

    private void loadForm() {
        try {
            ConfigStore.ensure(AppHome.exampleFile(), AppHome.configFile());
            form.read(ConfigStore.load(AppHome.configFile()));
            showForm();
            append("已读取本地配置。确认开关默认关闭。");
        } catch (IOException ex) {
            append("读取配置失败。");
        }
    }

    private void showForm() {
        wegameDir.setText(form.wegameDir);
        account.setText(form.account);
        password.setText(form.password);
        width.setText(form.width);
        height.setText(form.height);
        loginConfirmed.setSelected(form.loginConfirmed);
        launchConfirmed.setSelected(form.launchConfirmed);
        menuConfirmed.setSelected(form.menuConfirmed);
        postgameConfirmed.setSelected(form.postgameConfirmed);
        for (Map.Entry<String, JTextField> entry : advancedFields.entrySet()) {
            entry.getValue().setText(form.advanced.getOrDefault(entry.getKey(), ""));
        }
    }

    private void collect() {
        form.wegameDir = wegameDir.getText();
        form.account = account.getText();
        form.password = new String(password.getPassword());
        form.width = width.getText();
        form.height = height.getText();
        form.loginConfirmed = loginConfirmed.isSelected();
        form.launchConfirmed = launchConfirmed.isSelected();
        form.menuConfirmed = menuConfirmed.isSelected();
        form.postgameConfirmed = postgameConfirmed.isSelected();
        for (Map.Entry<String, JTextField> entry : advancedFields.entrySet()) {
            form.advanced.put(entry.getKey(), entry.getValue().getText());
        }
    }

    private boolean saveForm(boolean starting) {
        collect();
        if (!isInteger(form.width) || !isInteger(form.height)) {
            append("客户区分辨率必须是整数，配置没有保存。");
            return false;
        }
        try {
            ConfigStore.save(AppHome.configFile(), AppHome.exampleFile(), form);
            if (!starting) {
                append("配置已保存。");
            }
            return true;
        } catch (IOException ex) {
            append("配置没有保存。");
            return false;
        }
    }

    private void start(RunMode mode) {
        if (run.isBusy()) {
            append("已有任务在跑。");
            return;
        }
        if (mode == RunMode.DAILY && !allowDaily(postgameConfirmed.isSelected())) {
            append("每天 5 场需要先在高级项里勾选「赛后重开位置已核对」。这次没有启动。");
            return;
        }
        if (!saveForm(true)) {
            return;
        }
        setRunning(true);
        append(mode == RunMode.TRIAL ? "开始试跑 1 场。" : "开始每天 9:00 后打 5 场。");
        boolean started = run.start(mode, AppHome.configFile(), runtime, this::append, () ->
                SwingUtilities.invokeLater(() -> {
                    if (!frame.isDisplayable()) {
                        System.exit(0);
                    }
                    setRunning(false);
                }));
        if (!started) {
            setRunning(false);
            append("已有任务在跑。");
        }
    }

    private void closeWindow() {
        run.stop();
        frame.dispose();
        if (!run.isBusy()) {
            System.exit(0);
        }
    }

    private void setRunning(boolean running) {
        trial.setEnabled(!running);
        daily.setEnabled(!running);
        save.setEnabled(!running);
        pause.setEnabled(running);
        resume.setEnabled(running);
        stop.setEnabled(running);
    }

    private void append(String line) {
        Runnable write = () -> {
            log.append(line);
            log.append("\n");
            if (log.getDocument().getLength() > 120_000) {
                log.replaceRange("", 0, 20_000);
            }
            log.setCaretPosition(log.getDocument().getLength());
        };
        if (SwingUtilities.isEventDispatchThread()) {
            write.run();
        } else {
            SwingUtilities.invokeLater(write);
        }
    }

    /** 每天 5 场必须已经确认赛后重开位置。 */
    static boolean allowDaily(boolean postgameConfirmed) {
        return postgameConfirmed;
    }

    private static boolean isInteger(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(text.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static int row(JPanel panel, int row, String name, JComponent field, Font font) {
        JLabel label = new JLabel(name);
        label.setFont(font);
        panel.add(label, constraints(0, row));
        GridBagConstraints fieldConstraints = constraints(1, row);
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, fieldConstraints);
        return row + 1;
    }

    private static GridBagConstraints constraints(int x, int y) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(3, 6, 3, 6);
        return constraints;
    }

    private static JLabel label(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        return label;
    }

    private static void applyFont(JComponent component, Font font) {
        component.setFont(font);
    }

    private static Font uiFont() {
        Font font = new Font("Microsoft YaHei", Font.PLAIN, 14);
        if ("Microsoft YaHei".equals(font.getFamily())) {
            return font;
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    }
}
