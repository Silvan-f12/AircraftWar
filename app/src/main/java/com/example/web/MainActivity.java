package com.example.web;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.web.net.SocketGameClient;

public class MainActivity extends AppCompatActivity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private EditText hostInput;
    private EditText portInput;
    private EditText nameInput;
    private TextView statusView;
    private TextView selfScoreView;
    private TextView opponentScoreView;
    private TextView settleView;
    private Button connectButton;
    private Button addScoreButton;
    private Button deadButton;

    private SocketGameClient client;
    private int myScore;
    private int myPlayerIndex;
    private boolean dead;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(buildContentView());

        client = new SocketGameClient(new SocketGameClient.Listener() {
            @Override
            public void onConnected() {
                runOnMain(() -> setStatus("已连接服务器，等待匹配..."));
            }

            @Override
            public void onWaiting() {
                runOnMain(() -> setStatus("等待另一名玩家加入..."));
            }

            @Override
            public void onMatched(int playerIndex) {
                runOnMain(() -> {
                    myPlayerIndex = playerIndex;
                    setStatus("匹配成功，你是玩家 P" + playerIndex);
                    addScoreButton.setEnabled(true);
                    deadButton.setEnabled(true);
                    dead = false;
                    settleView.setText("结算分数: 未结束");
                });
            }

            @Override
            public void onState(int score1, int score2, boolean dead1, boolean dead2) {
                runOnMain(() -> {
                    int self = myPlayerIndex == 2 ? score2 : score1;
                    int opp = myPlayerIndex == 2 ? score1 : score2;
                    selfScoreView.setText("我的分数: " + self);
                    opponentScoreView.setText("对手分数: " + opp);
                    if ((myPlayerIndex == 1 && dead1) || (myPlayerIndex == 2 && dead2)) {
                        setStatus("你已死亡，等待对手结束...");
                    }
                });
            }

            @Override
            public void onSettle(int score1, int score2) {
                runOnMain(() -> {
                    int self = myPlayerIndex == 2 ? score2 : score1;
                    int opp = myPlayerIndex == 2 ? score1 : score2;
                    setStatus("双方已死亡，对局结束");
                    settleView.setText("结算分数: 我方=" + self + " / 对手=" + opp);
                    addScoreButton.setEnabled(false);
                    deadButton.setEnabled(false);
                });
            }

            @Override
            public void onOpponentLeft(int playerIndex) {
                runOnMain(() -> {
                    setStatus("玩家 P" + playerIndex + " 已断开连接");
                    addScoreButton.setEnabled(false);
                    deadButton.setEnabled(false);
                });
            }

            @Override
            public void onError(String message) {
                runOnMain(() -> setStatus("错误: " + message));
            }

            @Override
            public void onDisconnected() {
                runOnMain(() -> {
                    setStatus("连接已断开");
                    addScoreButton.setEnabled(false);
                    deadButton.setEnabled(false);
                });
            }
        });

        connectButton.setOnClickListener(v -> doConnect());
        addScoreButton.setOnClickListener(v -> {
            if (dead) {
                return;
            }
            myScore += 10;
            selfScoreView.setText("我的分数: " + myScore);
            client.sendScore(myScore);
        });
        deadButton.setOnClickListener(v -> {
            if (dead) {
                return;
            }
            dead = true;
            client.sendDead(myScore);
            addScoreButton.setEnabled(false);
            deadButton.setEnabled(false);
            setStatus("你已死亡，已上报服务器");
        });

        resetState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (client != null) {
            client.close();
        }
    }

    private void doConnect() {
        resetState();
        String host = hostInput.getText().toString().trim();
        String portText = portInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        if (host.isEmpty()) {
            host = "10.0.2.2";
        }
        if (name.isEmpty()) {
            name = "player";
        }
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            port = 7777;
            portInput.setText("7777");
        }
        setStatus("正在连接 " + host + ":" + port + " ...");
        client.connect(host, port, name);
    }

    private void resetState() {
        myScore = 0;
        myPlayerIndex = 1;
        dead = false;
        selfScoreView.setText("我的分数: 0");
        opponentScoreView.setText("对手分数: 0");
        settleView.setText("结算分数: 未结束");
        addScoreButton.setEnabled(false);
        deadButton.setEnabled(false);
    }

    private View buildContentView() {
        int pad = dp(16);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scrollView.addView(root);

        hostInput = new EditText(this);
        hostInput.setHint("服务器地址，例如 10.0.2.2");
        hostInput.setText("10.0.2.2");

        portInput = new EditText(this);
        portInput.setHint("端口，例如 7777");
        portInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        portInput.setText("7777");

        nameInput = new EditText(this);
        nameInput.setHint("玩家名");

        connectButton = new Button(this);
        connectButton.setText("连接并匹配");

        statusView = new TextView(this);
        selfScoreView = new TextView(this);
        opponentScoreView = new TextView(this);
        settleView = new TextView(this);

        addScoreButton = new Button(this);
        addScoreButton.setText("+10 分并同步");

        deadButton = new Button(this);
        deadButton.setText("我已死亡，提交结算");

        root.addView(hostInput);
        root.addView(portInput);
        root.addView(nameInput);
        root.addView(connectButton);
        root.addView(statusView);
        root.addView(selfScoreView);
        root.addView(opponentScoreView);
        root.addView(settleView);
        root.addView(addScoreButton);
        root.addView(deadButton);

        return scrollView;
    }

    private void setStatus(String text) {
        statusView.setText("状态: " + text);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void runOnMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
            return;
        }
        mainHandler.post(runnable);
    }
}

