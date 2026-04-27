# Web 项目教程（Android 客户端 + Java Socket 服务端）

本仓库是一个教程向多模块工程，包含：

- `app`：Android 联机演示客户端
- `MyServer`：Java Socket 服务端模块
- `AircraftWar-main`：飞机大战联机版客户端工程

## 1. 教程路径约定

为避免绑定个人电脑路径，本文统一使用占位路径：

- `<WEB_ROOT>`：当前仓库根目录（即包含 `settings.gradle.kts` 的目录）

示例：

- macOS: `/Users/yourname/AndroidStudioProjects/Web`
- Windows: `D:\AndroidStudioProjects\Web`

## 2. 功能目标

- 两名玩家建立连接并自动匹配
- 对局过程中分数实时同步
- 双方死亡后同时收到最终结算

## 3. 协议说明（文本行）

客户端 -> 服务端：

- `HELLO|<name>`：加入匹配
- `SCORE|<score>`：上报当前分数
- `DEAD|<score>`：上报死亡及最终分数
- `QUIT`：主动断开

服务端 -> 客户端：

- `WAITING`
- `MATCHED|<playerIndex>`（1 或 2）
- `STATE|<score1>|<score2>|<dead1>|<dead2>`
- `SETTLE|<score1>|<score2>`
- `OPPONENT_LEFT|<playerIndex>`

## 4. 环境准备

- Android Studio（建议稳定版）
- JDK 17
- Android 模拟器或真机

## 5. 启动服务端（区分 macOS / Windows）

默认端口 `7777`。

### macOS

```bash
cd <WEB_ROOT>
./gradlew :MyServer:run --args='7777'
```

### Windows (PowerShell)

```powershell
cd <WEB_ROOT>
.\gradlew.bat :MyServer:run --args="7777"
```

启动成功后，终端会持续运行；不要关闭该终端。

## 6. 启动客户端（区分使用场景）

你可以运行 `app` 或 `AircraftWar-main`。

### 6.1 运行 `app` 模块（Android Studio）

1. 打开 `<WEB_ROOT>`
2. 运行 `app` 模块到设备
3. 填写地址端口：
   - 模拟器连本机：`10.0.2.2:7777`
   - 真机连本机：`<电脑局域网IP>:7777`

### 6.2 运行 `AircraftWar-main` 模块（Android Studio）

1. 打开 `<WEB_ROOT>/AircraftWar-main`
2. 运行 `app` 模块
3. 在选择页勾选联机模式并填写服务器地址端口

## 7. 构建命令（区分 macOS / Windows）

### macOS

```bash
cd <WEB_ROOT>
./gradlew :MyServer:build
./gradlew :app:assembleDebug
```

### Windows (PowerShell)

```powershell
cd <WEB_ROOT>
.\gradlew.bat :MyServer:build
.\gradlew.bat :app:assembleDebug
```

## 8. 联机验收步骤

1. 服务端已运行
2. 两台客户端连接到同一地址和端口
3. 双方匹配成功
4. 任一方得分变化，对方实时看到更新
5. 双方死亡后同时显示结算

## 9. 常见问题

- 一直 `WAITING`：确认第二个客户端已连接同一服务端
- 连接失败：检查端口、防火墙、IP 是否正确
- 模拟器连不上本机：确认地址使用 `10.0.2.2`

## 10. 已配置权限

`app/src/main/AndroidManifest.xml` 已包含：

- `android.permission.INTERNET`
- `android:usesCleartextTraffic="true"`
- `android:networkSecurityConfig="@xml/network_security_config"`

