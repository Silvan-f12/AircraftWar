# Socket 联机对战实验（Android + Java Module）

本工程包含两部分：

- `MyServer`：Java Module，作为 Socket 对战服务器
- `app`：Android 客户端，连接服务器并实时同步分数

## 功能点对应

- 两名玩家建立连接：服务器将前两名在线玩家自动配对
- 实时同步分数：任一方发送分数变化后，服务器广播最新对局状态
- 双方死亡同步结算：两人都发送 `DEAD` 后，服务器同时下发结算消息

## 协议（文本行协议）

客户端 -> 服务器：

- `HELLO|<name>`：加入匹配
- `SCORE|<score>`：上报当前分数
- `DEAD|<score>`：上报死亡及最终分数

服务器 -> 客户端：

- `WAITING`
- `MATCHED|<playerIndex>`（1 或 2）
- `STATE|<score1>|<score2>|<dead1>|<dead2>`
- `SETTLE|<score1>|<score2>`
- `OPPONENT_LEFT|<playerIndex>`

## 快速运行

1) 启动服务器（默认端口 7777）

```bash
cd /Users/duchongyang/AndroidStudioProjects/Web
./gradlew :MyServer:run
```

2) 运行 Android 客户端

- 在 Android Studio 运行 `app` 模块
- 在真机/模拟器中输入服务器地址与端口：
  - 模拟器访问宿主机可用 `10.0.2.2`
  - 端口默认 `7777`

3) 启动两个客户端实例进行对战验证

- 玩家 A/B 连接后自动匹配
- 点击 `+10 分并同步`，另一端可看到分数实时变化
- 双方分别点击 `我已死亡，提交结算`，两端会同时显示最终结算

## 权限与明文配置

已在 `app/src/main/AndroidManifest.xml` 中配置：

- `android.permission.INTERNET`
- `android:usesCleartextTraffic="true"`
- `android:networkSecurityConfig="@xml/network_security_config"`

