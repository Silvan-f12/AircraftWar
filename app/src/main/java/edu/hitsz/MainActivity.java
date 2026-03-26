package edu.hitsz;

import android.app.PictureInPictureUiState;
import android.app.PictureInPictureParams;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import java.util.HashMap;
import java.util.Map;

import edu.hitsz.Game.DifficultGame;
import edu.hitsz.Game.Game;
import edu.hitsz.Game.MediumGame;
import edu.hitsz.Game.SimpleGame;
import edu.hitsz.application.AudioManager;
import edu.hitsz.application.ImageManager;

/**
 * 游戏主活动 - 纯粹的游戏运行界面
 * 包含完整的游戏循环控制、UI 覆盖层、小窗模式及生命周期管理
 */
public class MainActivity extends AppCompatActivity implements MySurfaceView.OnGameOverListener {

    private MySurfaceView gameSurfaceView;
    private FrameLayout gameContainer;
    private Game game;

    private String currentLevel = "简单模式";
    private boolean isGameStarted = false;
    private boolean isGamePaused = false;
    //音乐是否播放
    private boolean isAudioEnabled = false;
    //音量控制
    private float audioBgmVolume = 0.5f;
    private float audioSfxVolume = 0.5f;

    // --- 双击退出核心变量 ---
    private boolean isExitRequested = false;
    private final Handler exitHandler = new Handler(Looper.getMainLooper());
    private final Runnable resetExitFlagRunnable = () -> {
        isExitRequested = false;
        Log.d("MainActivity", "Exit flag reset (timeout).");
    };
    private static final long EXIT_TIME_DELAY = 2000;
    AudioManager audioManager = AudioManager.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 图片管理器
        ImageManager.init(this);
        // 音频管理器

        audioManager.init(this);
        // 预加载音效 (定义好映射关系)
        Map<String, Integer> sounds = new HashMap<>();
        sounds.put("bullet_hit", R.raw.bullet_hit);      // 对应 res/raw/bullet_hit.wav
        sounds.put("enemy_explosion", R.raw.bomb_explosion);  // 对应 res/raw/explosion.ogg
        sounds.put("bullet", R.raw.bullet);
        sounds.put("get_supply",R.raw.get_supply);
        sounds.put("game_over",R.raw.game_over);
        audioManager.loadSounds(sounds);
        //游戏
        gameContainer = new FrameLayout(this);
        setContentView(gameContainer);
        // 获取启动参数
        // 获取启动参数
        if (getIntent() != null) {
            if (getIntent().hasExtra("DIFFICULTY")) {
                currentLevel = getIntent().getStringExtra("DIFFICULTY");
            }

            // --- 接收音频设置 (关键修改) ---
            if (getIntent().hasExtra("AUDIO_ENABLED")) {
                isAudioEnabled = getIntent().getBooleanExtra("AUDIO_ENABLED", true);
            }
            if (getIntent().hasExtra("BGM_VOLUME")) {
                audioBgmVolume = getIntent().getFloatExtra("BGM_VOLUME", 0.5f);
            }
            if (getIntent().hasExtra("SFX_VOLUME")) {
                audioSfxVolume = getIntent().getFloatExtra("SFX_VOLUME",0.5f);
            }

            Log.d("MainActivity", "Audio Settings -> Enabled: " + isAudioEnabled + ", BgmVolume: " + audioBgmVolume+", SfxVolume: "+audioSfxVolume);
        }
        audioManager.setAudioEnabled(isAudioEnabled);
        // 应用全局音量到 AudioManager
        audioManager.setBgmVolume(audioBgmVolume);
        audioManager.setSoundVolume(audioSfxVolume);


        // 初始化游戏
        startGame(currentLevel);

        // 设置返回键监听
        setupOnBackPressedCallback();
    }

    /**
     * 启动或重置游戏
     */
    private void startGame(String level) {
        Log.d("MainActivity", "Starting game: " + level);
        gameContainer.removeAllViews();

        if (gameSurfaceView == null) {
            gameSurfaceView = new MySurfaceView(this);
            gameSurfaceView.setOnGameOverListener(this);
        } else {
            gameSurfaceView.resetGameOverState();
        }
        gameContainer.addView(gameSurfaceView);

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        if (game != null) {
            game.release();
            game = null;
        }

        audioManager.playBgm(R.raw.bgm);
        switch(level) {
            case "简单模式": game = new SimpleGame(this, width, height); break;
            case "中等模式": game = new MediumGame(this, width, height); break;
            case "困难模式": game = new DifficultGame(this, width, height); break;
            default: game = new SimpleGame(this, width, height); break;
        }

        if (gameSurfaceView != null) {
            gameSurfaceView.setGame(game);
        }

        isGameStarted = true;
        isGamePaused = false;
        isExitRequested = false;
    }

    @Override
    public void onGameOver(int score) {
        Log.d("Game", "Game Over! Score: " + score);
        if (game != null && game.getScoreRecords() != null) {
            game.insertScore(score);
        }

        isGameStarted = false;
        isGamePaused = true;

        // 显示游戏结束选项按钮
        showGameOverOptions();
    }

    /**
     * 显示游戏结束时的按钮 (再来一局 / 返回菜单)
     */
    private void showGameOverOptions() {
        // 1. 清理旧按钮
        for (int i = 0; i < gameContainer.getChildCount(); i++) {
            View child = gameContainer.getChildAt(i);
            if (child instanceof Button || child instanceof LinearLayout) {
                gameContainer.removeView(child);
                i--;
            }
        }

        // 2. 创建水平布局
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        // --- 按钮 A: 再来一局 ---
        Button restartBtn = new Button(this);
        restartBtn.setText("再来一局");
        restartBtn.setTextSize(20);
        restartBtn.setTextColor(0xFF333333);
        restartBtn.setPadding(40, 30, 40, 30);
        restartBtn.setBackground(createRoundRectDrawable(0xFF4CAF50));

        restartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAudioEnabled) {
                    audioManager.playBgm(R.raw.bgm);
                }
                startGame(currentLevel);
            }
        });

        // --- 按钮 B: 返回菜单 ---
        Button menuBtn = new Button(this);
        menuBtn.setText("返回菜单");
        menuBtn.setTextSize(20);
        menuBtn.setTextColor(0xFF333333);
        menuBtn.setPadding(40, 30, 40, 30);
        menuBtn.setBackground(createRoundRectDrawable(0xFF9E9E9E));

        menuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSelect();
            }
        });

        layout.addView(restartBtn);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(40, LinearLayout.LayoutParams.MATCH_PARENT));
        layout.addView(spacer);

        layout.addView(menuBtn);

        // 3. 添加到屏幕下方
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.bottomMargin = 100;
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        gameContainer.addView(layout, params);
    }

    /**
     * 返回难度选择界面
     */
    private void goToSelect() {
        if (game != null) {
            game.release();
            game = null;
        }
        if (gameSurfaceView != null) {
            gameSurfaceView.release();
            gameSurfaceView = null;
        }
        isGameStarted = false;
        audioManager.stopBgm(); // 跳转前停止音乐

        Intent intent = new Intent(MainActivity.this, SelectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * 创建圆角边框 Drawable
     */
    private GradientDrawable createRoundRectDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(50f);
        drawable.setColor(0x00000000); // 透明背景
        drawable.setStroke(4, color); // 边框
        return drawable;
    }

    /**
     * 设置返回键回调 (双击退出逻辑)
     */
    private void setupOnBackPressedCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.d("MainActivity", "Back pressed. isExitRequested: " + isExitRequested);

                if (isExitRequested) {
                    // 第二次按下：返回菜单
                    exitHandler.removeCallbacks(resetExitFlagRunnable);
                    isExitRequested = false;
                    if (gameSurfaceView != null) gameSurfaceView.pauseGame();
                    goToSelect();
                } else {
                    // 第一次按下：提示
                    isExitRequested = true;
                    Toast.makeText(MainActivity.this, "再按一次返回菜单", Toast.LENGTH_SHORT).show();
                    exitHandler.removeCallbacks(resetExitFlagRunnable);
                    exitHandler.postDelayed(resetExitFlagRunnable, EXIT_TIME_DELAY);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    // --- 小窗模式 (PiP) 相关 ---

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isGameStarted) {
            Log.d("PiP", "Entering Picture-in-Picture mode...");
            Rational aspectRatio = new Rational(9, 16);
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
            enterPictureInPictureMode(params);
        }
    }

    @Override
    public void onPictureInPictureUiStateChanged(@NonNull PictureInPictureUiState uiState) {
        super.onPictureInPictureUiStateChanged(uiState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if (uiState.isStashed()) {
                hideGameUI();
            } else {
                showGameUI();
            }
        }
    }

    private void hideGameUI() {
        if (gameSurfaceView != null && isGameStarted) {
            gameSurfaceView.pauseGame();
            isGamePaused = true;
        }
    }

    private void showGameUI() {
        if (gameSurfaceView != null && isGameStarted && isGamePaused) {
            gameSurfaceView.resumeGame();
            isGamePaused = false;
        }
    }

    // --- 生命周期 ---

    @Override
    protected void onPause() {
        super.onPause();
        if (gameSurfaceView != null && isGameStarted && !isGamePaused) {
            gameSurfaceView.pauseGame();
            isGamePaused = true;
        }
        audioManager.pauseBgm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameSurfaceView != null && isGameStarted && isGamePaused) {
            gameSurfaceView.resumeGame();
            isGamePaused = false;
        }
        audioManager.resumeBgm();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exitHandler.removeCallbacks(resetExitFlagRunnable);
        if (gameSurfaceView != null) {
            gameSurfaceView.release();
        }
        if (game != null) {
            game.release();
        }
        ImageManager.clear();
        Log.d("MainActivity", "App destroyed.");
    }
}