# 2KOL2 试跑

在你自己的 Windows 电脑上，通过官方 WeGame 和 2KOL2 窗口做键鼠操作。默认命令只试跑 **1 场** 王朝人机（非常简单）。每天 9:00 之后打 5 场是另一条启动命令。

尚未在真实客户端上验证。点击位置和键位都是假定值，对不上就停，不会改游戏文件，也不会读内存。

## 运行前

- Windows，已安装官方 WeGame 和 2KOL2。
- 安装 JDK 17 或更高版本，并准备 Maven。
- 游戏使用**带标题栏的窗口模式**，客户区正好 **2560x1440**。全屏、无边框或分辨率不对时，程序停止且不点菜单。
- 刷新率 120、画质「高」请事先在游戏里设好。程序只把这两项记在配置里，不会替你打开画质菜单。
- 登录前把输入法换成英文。程序按美式键盘符号键入，不会切换输入法。

## 填写本地配置

1. 进入本目录 [2kol2-host](.)。
2. 把 [config/application.example.properties](config/application.example.properties) 复制为 `config/application.properties`。
3. 只在这份本地文件里填写 WeGame 目录、账号和密码。`application.properties` 已被 [.gitignore](.gitignore) 排除，不要提交。
4. 示例里的账号和密码是空的。程序里也没有默认账号或密码。
5. 路径请用正斜杠，例如 `D:/Program Files (x86)/WeGame`。
6. 对照你的画面修改点击比例，对照游戏「键盘」设置修改按键。核对之后再把对应开关改成 `true`：
   - `login.coordsConfirmed`：只有出现登录窗、并且你确认了账号框和密码框位置，才允许输入一次密码。已登录时不会输入。
   - `ui.launchConfirmed`：确认 WeGame 里 2KOL2 图标的位置后，才点击启动。
   - `ui.menuConfirmed`：确认「人机赛 / 王朝模式 / 非常简单 / 进入」的位置后，才点菜单。
   - `postgame.confirmed`：只影响每天 5 场的第 2 场及以后。试跑不会用它。

四个开关默认都是 `false`。未改成 `true` 时，程序会说明缺什么并停止，避免按未核对的坐标点击。

## 生成可双击的客户端

在你自己的 Windows 上执行这一条（脚本会自己找到 Maven 和 JDK，不要求它们已经在 PATH 里）：

```bat
2kol2-host\package-windows.cmd
```

脚本是 [package-windows.cmd](package-windows.cmd)。它固定使用：

- Maven：`C:\Program Files\Apache\apache-maven-3.9.16\bin\mvn.cmd`
- JDK：`C:\Program Files\Microsoft` 下面第一个带 `jpackage.exe` 的 `jdk-21*`

产物在 `2kol2-host\dist\2KOL2Host\`。生成后双击：

```text
2kol2-host\dist\2KOL2Host\2KOL2Host.exe
```

这个目录里带有 JDK 21 运行时，打开 exe 不用再装 Maven。用的是 `jpackage` 的 app-image，不需要 WiX。云端是 Linux，这里没有、也不能假装已经生成这个 exe。

配置放在 exe 旁边，不打进包。第一次打开如果还没有 `application.properties`，程序会从旁边的 `application.example.properties` 复制一份。不要把填好的配置提交到 git。

## 窗口里怎么用

界面在 [HostFrame.java](src/main/java/kol2/ui/HostFrame.java)。没有参数时，[Kol2HostApp.java](src/main/java/kol2/Kol2HostApp.java) 会打开这个窗口。

- 填写 WeGame 路径、QQ 号、密码和客户区分辨率，点「保存配置」。
- 「高级：点击比例和按键」里是点击比例、按键和确认开关。没核对点击位置就不要打开那三个开关。
- 「试跑 1 场」是默认按钮。
- 「每天 9:00 后打 5 场」必须先在高级项勾选「赛后重开位置已核对」。
- 「暂停」「继续」「停止」。关掉窗口等于停止，并松开按键。
- 下面的日志区和 `logs\kol2-host.log` 是同一步骤，不显示密码，也不显示完整账号。

流程仍在 [Session.java](src/main/java/kol2/flow/Session.java)。试跑不会自动开第 2 场。程序还不能从真实画面确认终场：没有终场截图时，会继续进攻循环，直到 `match.maxMinutes`（默认 45 分钟）后停止。这只表示到时停下，不表示已经打完。

命令行还可以显式指定模式（不带参数会打开窗口，而不是直接开赛）：

```bat
java -jar target\kol2-host.jar trial --config config\application.properties
java -jar target\kol2-host.jar daily --config config\application.properties
```

有控制台时也可以输入 `p` 暂停、`c` 继续、`q` 停止，`done` / `win` / `loss` 结束本场。

## 之后改成每天 9:00 后打 5 场

在窗口的高级项里核对赛后重开位置，勾选「赛后重开位置已核对」，再点「每天 9:00 后打 5 场」。这条会等到电脑本地时间 9:00；已经过了 9:00 就马上开始。默认打 5 场，输了继续，打够停止。没勾选赛后位置时不会启动。

它不是当前默认动作。默认仍是试跑 1 场。

## 假定的默认键位

下面是 [示例配置](config/application.example.properties) 里的假定值，**不是从游戏读出来的**。请打开 2KOL2 的「键盘」设置对照，不一致就改配置。

| 动作 | 假定按键 | 说明 |
| --- | --- | --- |
| 向前运球 | `W` | 假定镜头在球员身后 |
| 冲刺 | `SHIFT` | 和向前一起按住 |
| 呼叫队友内切 | `Q` | 有的键盘方案里 Q 是挡拆，对不上就改 |
| 传球 | `K` | |
| 投篮 | `J` | |
| 切换持球人 | 留空 | 假定开局已经控制持球人 |

进攻循环在 [Offense.java](src/main/java/kol2/flow/Offense.java)：按住冲刺向前，点按内切、传球、投篮，然后松开按键等待。没有抢断、盖帽或包夹。程序不能从外部可靠判断球权，所以用一轮进攻后的短暂停顿当作无球等待。

## 安全边界

只启动官方 WeGame，并对它和游戏窗口做键盘、鼠标和置前。不做内存读写、注入、封包、绕过反作弊、破解或修改游戏文件。

登录密码最多提交一次。出现验证码、二次验证、风控，或无法判断是否已登录时，直接停止。同一时间只运行一个程序实例。
