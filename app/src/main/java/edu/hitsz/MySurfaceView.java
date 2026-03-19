package edu.hitsz;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import edu.hitsz.Game.Game;

/**
 * 游戏主视图 (Android SurfaceView 版本)
 * 负责渲染游戏画面和接收触摸输入
 *
 * @author hitsz
 */
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

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

    // 游戏结束回调接口
    private OnGameOverListener gameOverListener;

    // 标记游戏是否已结束（用于防止重复回调）
    private boolean isGameOver = false;

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
        endPaint.setTextSize(60);
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
    public void surfaceCreated(SurfaceHolder holder) {
        // 获取屏幕尺寸
        WINDOW_WIDTH = getWidth();
        WINDOW_HEIGHT = getHeight();

        // 启动渲染线程
        startGameLoop();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
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
        renderThread = new Thread(this);
        renderThread.start();
    }

    /**
     * 停止游戏循环
     */
    private void stopGameLoop() {
        isRunning = false;
        if (renderThread != null) {
            try {
                renderThread.join(500);  // 最多等待500ms
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            renderThread = null;
        }
    }

    @Override
    public void run() {
        Canvas canvas;
        long frameTime = 16; // 60FPS = 1000ms / 60 ≈ 16.67ms
        long startTime;
        long sleepTime;

        while (isRunning) {
            startTime = System.currentTimeMillis();
            canvas = null;

            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    // 清空画布
                    canvas.drawColor(Color.BLACK);

                    if (game != null) {
                        // 更新游戏逻辑
                        game.updateLogic();
                        // 绘制游戏画面
                        game.draw(canvas);

                        // 检查游戏是否结束
                        if (game.isGameOver() && !isGameOver) {
                            isGameOver = true;
                            drawGameOver(canvas);
                            if (gameOverListener != null) {
                                gameOverListener.onGameOver(game.getScore());
                            }
                        }
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            // 【关键】精确控制帧率
            sleepTime = frameTime - (System.currentTimeMillis() - startTime);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    /**
     * 绘制游戏结束界面
     */
    private void drawGameOver(Canvas canvas) {
        // 半透明黑色背景
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, bgPaint);

        // 游戏结束文字
        endPaint.setTextSize(60);
        String text = "GAME OVER";
        canvas.drawText(text, WINDOW_WIDTH / 2f, WINDOW_HEIGHT / 2f - 50, endPaint);

        // 显示分数
        if (game != null) {
            endPaint.setTextSize(40);
            String scoreText = "Final Score: " + game.getScore();
            canvas.drawText(scoreText, WINDOW_WIDTH / 2f, WINDOW_HEIGHT / 2f + 20, endPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (game != null && !game.isGameOver()) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN) {
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