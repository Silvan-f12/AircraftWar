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
import java.util.Random;

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
    //用户名
    private String userName;
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
    private static final int REQUEST_CODE_GAME_OVER = 100; // 用于 GameOverActivity 返回结果
    AudioManager audioManager = AudioManager.getInstance();

    /**
     * 入口：初始化图像/音频资源，读取启动参数并创建对应难度的游戏实例。
     */
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
        sounds.put("enemy_explosion", R.raw.bomb_explosion);  // 兼容旧调用
        sounds.put("bomb_explosion", R.raw.bomb_explosion);   // 炸弹道具实际使用的键
        sounds.put("bullet", R.raw.bullet);
        sounds.put("get_supply",R.raw.get_supply);
        sounds.put("game_over",R.raw.game_over);
        audioManager.loadSounds(sounds);
        //游戏
        gameContainer = new FrameLayout(this);
        setContentView(gameContainer);
        Intent intent = getIntent(); // 先获取
        if (intent != null) { // 先判断 Intent 是否为空
            // 难度设置
            if (intent.hasExtra("DIFFICULTY")) {
                currentLevel = intent.getStringExtra("DIFFICULTY"); // 提供默认值
            } else {
                currentLevel = "简单模式"; // 强制默认
            }

            // 音频设置 (关键：这里如果没传值，必须给默认值，否则 getBooleanExtra 可能报错)
            isAudioEnabled = intent.getBooleanExtra("AUDIO_ENABLED", true); // 第二个参数是默认值
            audioBgmVolume = intent.getFloatExtra("BGM_VOLUME", 0.8f);
            audioSfxVolume = intent.getFloatExtra("SFX_VOLUME", 0.8f);
            userName = intent.getStringExtra("USERNAME"); // 假设你修改了方法签名或做了判空
        } else {
            // 如果 Intent 为空，全部使用默认值
            currentLevel = "简单模式";
            isAudioEnabled = true;
            audioBgmVolume = 0.8f;
            audioSfxVolume = 0.8f;
            userName = "游客";
            Log.e("MainActivity", "Intent is null! Using default settings.");
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
     * 按难度创建游戏对象并绑定到 SurfaceView，同时重置运行状态。
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


        // 确保 userName 不为空
        String finalUserName = userName;
        if (finalUserName == null || finalUserName.trim().isEmpty()) {
            finalUserName = "游客" + new Random().nextInt(100);
        }
        //

        // 1. 定义要传给 Game 类的难度 Tag (统一使用英文)
        String difficultyTag;

        // 2. 根据传入的 level 判断
        if ("简单模式".equals(level)) {
            difficultyTag = "simple";
            game = new SimpleGame(this, width, height, finalUserName, difficultyTag);
        } else if ("中等模式".equals(level)) {
            difficultyTag = "medium";
            game = new MediumGame(this, width, height, finalUserName, difficultyTag);
        } else if ("困难模式".equals(level)) {
            difficultyTag = "difficult";
            game = new DifficultGame(this, width, height, finalUserName, difficultyTag);
        } else {
            difficultyTag = "simple"; // 默认
            game = new SimpleGame(this, width, height, finalUserName, difficultyTag);
        }

        if (gameSurfaceView != null) {
            gameSurfaceView.setGame(game);
            // 启动渲染线程
            gameSurfaceView.resumeGame();
        }

        isGameStarted = true;
        isGamePaused = false;
        isExitRequested = false;
    }

    @Override
    /**
     * 游戏结束回调：落盘分数并跳转到结算页展示结果。
     */
    public void onGameOver(int score) {
        Log.d("Game", "Game Over! Score: " + score);
        if (game != null) {
            game.insertScore();
        }
        isGameStarted = false;
        isGamePaused = true;

        // 2. 使用 Handler 跳转
        
        new Handler(Looper.getMainLooper()).post(() -> {
            Intent intent = new Intent(MainActivity.this, GameOverActivity.class);
            intent.putExtra("FINAL_SCORE", score);
            intent.putExtra("GAME_LEVEL", currentLevel); // 顺手把难度也传过去，方便显示
            intent.putExtra("USERNAME", userName);
            startActivityForResult(intent, REQUEST_CODE_GAME_OVER);
            // 注意：这里不 finish()，因为返回键应该回到 MainActivity 的 onPause 状态
        });
    }

    @Override
    /**
     * 处理 GameOverActivity 的返回结果（再来一局）
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_GAME_OVER && resultCode == RESULT_OK && data != null) {
            String action = data.getStringExtra("ACTION");
            if ("RESTART".equals(action)) {
                String targetLevel = data.getStringExtra("TARGET_LEVEL");
                currentLevel = targetLevel != null ? targetLevel : "简单模式";
                // 重新开始游戏
                startGame(currentLevel);
            }
        }
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
    /**
     * 用户离开应用时尝试进入画中画，保持游戏上下文。
     */
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
    /**
     * PiP UI 状态变化回调：根据窗口状态暂停/恢复并调整画面尺寸。
     */
    public void onPictureInPictureUiStateChanged(@NonNull PictureInPictureUiState uiState) {
        super.onPictureInPictureUiStateChanged(uiState);
        boolean isInPipMode = isInPictureInPictureMode();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            if (uiState.isStashed()) {
                hideGameUI();
            } else {
                showGameUI();
            }
        }
        // 核心修改：根据是否在PiP模式来调整SurfaceView的尺寸
        if (gameSurfaceView != null) {
            ViewGroup.LayoutParams params = gameSurfaceView.getLayoutParams();
            if (isInPipMode) {
                // 进入PiP模式，将SurfaceView的尺寸设置为充满整个小窗
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                // 退出PiP模式，将SurfaceView的尺寸恢复为全屏
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;

            }
            gameSurfaceView.setLayoutParams(params);
        }
    }

    /**
     * 隐藏游戏交互层（当前策略为暂停渲染线程）。
     */
    private void hideGameUI() {
        if (gameSurfaceView != null && isGameStarted) {
            gameSurfaceView.pauseGame();
            isGamePaused = true;
        }
    }

    /**
     * 恢复游戏交互层（当前策略为恢复渲染线程）。
     */
    private void showGameUI() {
        if (gameSurfaceView != null && isGameStarted && isGamePaused) {
            gameSurfaceView.resumeGame();
            isGamePaused = false;
        }
    }

    // --- 生命周期 ---

    @Override
    /**
     * 生命周期暂停：暂停游戏循环并暂停背景音乐。
     */
    protected void onPause() {
        super.onPause();
        if (gameSurfaceView != null && isGameStarted && !isGamePaused) {
            gameSurfaceView.pauseGame();
            isGamePaused = true;
        }
        audioManager.pauseBgm();
    }

    @Override
    /**
     * 生命周期恢复：恢复游戏循环并恢复背景音乐。
     */
    protected void onResume() {
        super.onResume();
        if (gameSurfaceView != null && isGameStarted && isGamePaused) {
            gameSurfaceView.resumeGame();
            isGamePaused = false;
        }
        audioManager.resumeBgm();
    }

    @Override
    /**
     * 生命周期销毁：释放线程、图像和音频等资源。
     */
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