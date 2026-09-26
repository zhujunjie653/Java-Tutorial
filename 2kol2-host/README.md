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

## 试跑 1 场

在 `2kol2-host` 目录执行：

```bat
mvn -q package
java -jar target\kol2-host.jar
```

指定配置文件时：

```bat
java -jar target\kol2-host.jar trial --config config\application.properties
```

不写参数也是试跑，入口在 [Kol2HostApp.java](src/main/java/kol2/Kol2HostApp.java)。流程在 [Session.java](src/main/java/kol2/flow/Session.java)：登录 WeGame，启动 2KOL2，进人机赛王朝、难度非常简单，打 1 场；无法继续就停下。

运行中在同一个控制台输入：

| 输入 | 作用 |
| --- | --- |
| `p` | 暂停，并松开已按下的键 |
| `c` | 继续 |
| `q` | 停止 |
| `done` | 把本场记为未判定并结束本场 |
| `win` / `loss` | 记下胜或负并结束本场 |

日志在 `logs/kol2-host.log`，包含时间、步骤、结果和错误。不会写密码，也不会写完整账号。

试跑不会自动开第 2 场。程序还不能从真实画面确认终场：没有终场截图、你也没输入 `done` / `win` / `loss` 时，它会继续进攻循环，直到 `match.maxMinutes`（默认 45 分钟）后停止。这只表示到时停下，不表示已经打完。

## 之后改成每天 9:00 后打 5 场

确认试跑的坐标和键位可用，并把 `postgame.confirmed` 改成 `true`（同时填好 `click.rematch`）之后，用单独命令：

```bat
java -jar target\kol2-host.jar daily --config config\application.properties
```

这条命令会等到电脑本地时间 9:00 再开始。9:00 已过则立即开始。默认打 5 场，输了继续，打够停止。分不出终场、或赛后按钮未确认时，停止而不是继续点。

`daily` 不是当前默认命令。

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
