package edu.hitsz;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import edu.hitsz.Game.SimpleGame;
import edu.hitsz.application.ImageManager;

/**
 * 游戏主活动
 */
public class MainActivity extends Activity implements MySurfaceView.OnGameOverListener {

    private MySurfaceView gameSurfaceView;
    private FrameLayout gameContainer;
    private SimpleGame game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 初始化图片管理器 (必须在最前面)
        ImageManager.init(this);

        // 2. 创建游戏容器
        gameContainer = new FrameLayout(this);
        setContentView(gameContainer);

        // 3. 创建游戏视图
        gameSurfaceView = new MySurfaceView(this);
        gameSurfaceView.setOnGameOverListener(this);

        // 4. 获取屏幕尺寸
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;

        // 5. 创建游戏逻辑实例 (传入屏幕尺寸)
        game = new SimpleGame(this,width, height);

        // 6. 将游戏实例传入视图
        gameSurfaceView.setGame(game);

        // 7. 添加到容器
        gameContainer.addView(gameSurfaceView);
    }

    @Override
    public void onGameOver(int score) {
        // 游戏结束后的处理
        android.util.Log.d("Game", "Game Over! Score: " + score);

        // 保存分数到数据库
        if (game != null && game.getScoreRecords() != null) {
            game.insertScore(score);
        }

        // 延迟后重启游戏或返回菜单
        // 重启游戏
        // 或者 finish() 返回菜单
        gameSurfaceView.postDelayed(this::restartGame, 2000);
    }

    /**
     * 重启游戏
     */
    private void restartGame() {
        if (gameContainer != null) {
            gameContainer.removeView(gameSurfaceView);
        }

        // 重新创建视图和游戏
        gameSurfaceView = new MySurfaceView(this);
        gameSurfaceView.setOnGameOverListener(this);

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        game = new SimpleGame(this,width, height);

        gameSurfaceView.setGame(game);
        gameContainer.addView(gameSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameSurfaceView != null) {
            gameSurfaceView.pauseGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameSurfaceView != null) {
            gameSurfaceView.resumeGame();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 停止游戏线程
        if (gameSurfaceView != null) {
            gameSurfaceView.release();
        }

        // 清理游戏逻辑对象
        if (game != null) {
            game.release();
        }

        // 【必须】清理图片资源，防止内存泄漏
        ImageManager.clear();

        Log.d("MainActivity", "App destroyed, resources released.");
    }
}