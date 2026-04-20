package edu.hitsz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import edu.hitsz.Game.Game;

/**
 * 游戏主视图 (Android SurfaceView 版本)
 * 负责渲染游戏画面和接收触摸输入
 *
 * @author hitsz
 */
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = "MySurfaceView"; // 1. 定义一个统一的标签
    /**
     * 窗口宽度
     */
    public static int WINDOW_WIDTH;

    /**
     * 窗口高度
     */
    public static int WINDOW_HEIGHT;

    // SurfaceHolder 用于控制 Surface
    private final SurfaceHolder holder;

    // 游戏逻辑实例
    private Game game;

    // 渲染线程
    private Thread renderThread;

    // 运行标志
    private volatile boolean isRunning = false;

    // 绘图用的 Paint (用于游戏结束界面)
    private final Paint endPaint;
    // [新增] 标记是否正在显示 Game Over 定格画面（此时线程不应停止，而应持续重绘该画面）
    private boolean isDisplayingGameOver = false;
    // 游戏结束回调接口
    private OnGameOverListener gameOverListener;

    // 标记游戏是否已结束（用于防止重复回调）
    private boolean isGameOver = false;

    // 记录 Game Over 画面首次显示时间，用于停留一段时间后再跳转
    private long gameOverShownAt = -1L;

    // 确保 Game Over 回调只触发一次
    private boolean gameOverCallbackSent = false;

    private Bitmap bgBitmap;

    /**
     * 游戏结束监听接口
     */
    public interface OnGameOverListener {
        void onGameOver(int score);
    }

    public MySurfaceView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);

        // 初始化 Paint
        endPaint = new Paint();
        endPaint.setColor(Color.WHITE);
        endPaint.setTextSize(48);
        endPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        endPaint.setAntiAlias(true);
        endPaint.setTextAlign(Paint.Align.CENTER);

        // 设置焦点，确保能接收触摸事件
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    /**
     * 设置游戏实例
     * @param game 具体的游戏逻辑类
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * 设置游戏结束监听器
     */
    public void setOnGameOverListener(OnGameOverListener listener) {
        this.gameOverListener = listener;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        // 获取屏幕尺寸
        WINDOW_WIDTH = getWidth();
        WINDOW_HEIGHT = getHeight();

        // 启动渲染线程
        startGameLoop();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        // 停止渲染线程
        stopGameLoop();
    }

    /**
     * 启动游戏循环
     */
    private void startGameLoop() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        isGameOver = false;
        gameOverShownAt = -1L;
        gameOverCallbackSent = false;
        renderThread = new Thread(this);
        renderThread.start();
    }

    /**
     * 停止游戏循环
     */
    private void stopGameLoop() {
        isRunning = false; // 1. 先发信号，告诉循环我要停止了

        // 2. 尝试中断线程，让它从 sleep 状态中立即醒来
        if (renderThread != null && renderThread.isAlive()) {
            renderThread.interrupt();
        }

        // 3. 等待线程结束，但不要等太久，防止卡死
        if (renderThread != null) {
            try {
                renderThread.join(100); // 最多等 100ms
            } catch (InterruptedException e) {
                Log.d(TAG, "停止线程时被中断");
            } finally {
                renderThread = null;
            }
        }
    }

    @Override
    public void run() {
        // 移动 sleepTime 的声明到循环内部
        //long frameTime = 16;
        long startTime;
        long sleepTime;

        // 只有 isRunning 为 true 时才循环
        while (isRunning) {
            startTime = System.currentTimeMillis();
            Canvas canvas = null;

            try {
                if (isDisplayingGameOver) {
                    // 游戏已结束，不再绘制，只等待回调时间
                    if (gameOverShownAt > 0 && !gameOverCallbackSent
                            && System.currentTimeMillis() - gameOverShownAt >= 500L) {
                        gameOverCallbackSent = true;
                        final int finalScore = game.getScore();
                        MySurfaceView.this.post(() -> {
                            if (gameOverListener != null) {
                                gameOverListener.onGameOver(finalScore);
                            }
                        });
                        break;
                    }
                    // 每 16ms 检查一次时间，避免 CPU 占用过高
                    Thread.sleep(16);
                } else {
                    // 正常游戏流程：绘制画面
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        if (game != null) {
                            game.updateLogic();
                            game.draw(canvas);

                            // 检查游戏是否结束
                            if (game.isGameOver() && !isDisplayingGameOver) {
                                isDisplayingGameOver = true;
                                gameOverShownAt = System.currentTimeMillis();
                            }
                        }
                    }
                    if (canvas != null) {
                        try {
                            holder.unlockCanvasAndPost(canvas);
                        } catch (Exception e) {
                            // 忽略释放时的异常
                        }
                    }
                }
            } catch (Exception e) {
                // 捕获所有异常，防止线程意外崩溃
                Log.e(TAG, "渲染线程异常: " + e.getMessage());
                break;
            }
        }
        // 循环结束，重置运行标志，确保下次可以启动新线程
        isRunning = false;
    }



    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (game != null && !game.isGameOver()) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN) {
                // 这里是游戏内拖拽英雄机，不是页面/Tab切换手势。
                // 获取触摸坐标
                float x = event.getX();
                float y = event.getY();
                // 移动英雄机
                game.moveHeroTo(x, y);
            }
        }
        return true;
    }

    /**
     * 重置游戏结束状态，准备开始新游戏
     * 必须在启动新游戏逻辑前调用
     */
    public void resetGameOverState() {
        isDisplayingGameOver = false;
        isGameOver = false;
        gameOverShownAt = -1L;
        gameOverCallbackSent = false;
    }

    /**
     * 恢复游戏运行 (用于重新开始)
     */
    public void resumeGame() {
        if (!isRunning && holder.getSurface().isValid()) {
            startGameLoop();
        }
    }

    /**
     * 暂停游戏
     */
    public void pauseGame() {
        stopGameLoop();
    }

    /**
     * 释放资源，在 Activity onDestroy 中调用
     */
    public void release() {
        // 1. 停止游戏循环
        stopGameLoop();

        // 2. 移除回调
        holder.removeCallback(this);

        // 3. 清理游戏对象
        game = null;
        gameOverListener = null;
    }
}