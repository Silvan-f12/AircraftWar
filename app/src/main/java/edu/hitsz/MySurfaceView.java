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
        long frameTime = 16;
        long startTime;
        long sleepTime;

        // 只有 isRunning 为 true 时才循环
        while (isRunning) {
            startTime = System.currentTimeMillis();
            Canvas canvas = null;

            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    // 清空画布
                    canvas.drawColor(Color.BLACK);

                    if (game != null) {
                        if (isDisplayingGameOver) {
                            drawGameOver(canvas);
                        } else {
                            // 正常游戏流程
                            game.updateLogic();
                            game.draw(canvas);

                            // 检查游戏是否结束
                            if (game.isGameOver() && !isDisplayingGameOver) {
                                isDisplayingGameOver = true;

                                // 获取最终分数
                                final int finalScore = game.getScore();

                                // 回调给主线程
                                MySurfaceView.this.post(() -> {
                                    if (gameOverListener != null) {
                                        gameOverListener.onGameOver(finalScore);
                                    }
                                    // 关键：不要在这里调用 stopGameLoop()！
                                    // 让主线程去调用 stopGameLoop()，这里只负责退出循环
                                });

                                // 直接跳出循环，防止继续绘制
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 捕获所有异常，防止线程意外崩溃
                Log.e(TAG, "渲染线程异常: " + e.getMessage());
                break;
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        // 忽略释放时的异常
                    }
                }
            }

            // 控制帧率（关键：加了 try-catch 并且移除了外部的 sleepTime 计算）
            sleepTime = frameTime - (System.currentTimeMillis() - startTime);
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    // 一旦被中断，立即退出
                    break;
                } catch (Exception e) {
                    break;
                }
            }
        }
        // 循环结束，线程自然死亡
    }

    /**
     * 绘制游戏结束界面 (模拟磨砂玻璃效果)
     */
    private void drawGameOver(Canvas canvas) {
        // --- 1. 绘制深色遮罩 (让背景游戏画面变暗，突出前景) ---
        Paint bgPaint = new Paint();
        // 提高透明度到 230 (范围 0-255)，让背景更暗，更有聚焦感
        bgPaint.setColor(Color.argb(20, 0, 0, 0));
        canvas.drawRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, bgPaint);

        // --- 2. 配置“磨砂玻璃”卡片样式 ---
        Paint glassPaint = new Paint(Paint.ANTI_ALIAS_FLAG); // 开启抗锯齿
        // 颜色：半透明白色 (Alpha=60, 约 25% 不透明度)
        // 这种半透明白色叠加在深色背景上，就是经典的磨砂玻璃效果
        glassPaint.setColor(Color.argb(60, 255, 255, 255));
        glassPaint.setStyle(Paint.Style.FILL);

        // 定义卡片的大小和位置
        float cardWidth = WINDOW_WIDTH * 0.8f; // 卡片宽度占屏幕 80%
        float cardHeight = WINDOW_HEIGHT * 0.4f; // 卡片高度占屏幕 40%
        float left = (WINDOW_WIDTH - cardWidth) / 2f;
        float top = (WINDOW_HEIGHT - cardHeight) / 2f;
        float right = left + cardWidth;
        float bottom = top + cardHeight;
        float minEdge = Math.min(WINDOW_WIDTH, WINDOW_HEIGHT);
        float radius = minEdge * 0.035f; // 圆角半径按屏幕比例缩放

        // 绘制圆角矩形 (这就是“玻璃”本体)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, glassPaint);

        // (可选) 给玻璃卡片加一个细微的白色边框，增加质感
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.argb(100, 255, 255, 255)); // 更淡的白边
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(minEdge * 0.002f);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, borderPaint);

        // --- 3. 绘制文字 (现在文字是在“玻璃”上面) ---

        // 游戏结束文字
        endPaint.setTextSize(Math.max(44f, minEdge * 0.065f));
        endPaint.setTextAlign(Paint.Align.CENTER);
        endPaint.setColor(Color.WHITE); // 确保文字是白色的，在玻璃上更清晰
        // 可以加一点阴影，让文字更立体
        endPaint.setShadowLayer(minEdge * 0.006f, 0f, minEdge * 0.003f, Color.BLACK);

        Paint.FontMetrics fm = endPaint.getFontMetrics();
        float lineHeight = fm.bottom - fm.top;

        // 计算文字在卡片内的垂直中心位置
        // 卡片中心 Y = top + cardHeight / 2
        float cardCenterY = top + cardHeight / 2f;

        // 第一行 "GAME OVER" (稍微偏上一点)
        String text = "GAME OVER";
        // 基线位置 = 卡片中心 - (文字总高度 / 2) + 一些微调
        float textY = cardCenterY - (lineHeight / 2f) - minEdge * 0.02f;
        canvas.drawText(text, WINDOW_WIDTH / 2f, textY, endPaint);

        // 显示分数
        if (game != null) {
            endPaint.setTextSize(Math.max(30f, minEdge * 0.045f));
            // 重置阴影或调整阴影以适应小字
            endPaint.setShadowLayer(minEdge * 0.003f, 0f, minEdge * 0.0015f, Color.BLACK);

            String scoreText = "Final Score: " + game.getScore();
            // 分数在标题下方
            float scoreY = textY + lineHeight + minEdge * 0.02f;
            canvas.drawText(scoreText, WINDOW_WIDTH / 2f, scoreY, endPaint);
        }

        // 恢复 Paint 设置 (防止影响下一帧的游戏绘制)
        endPaint.clearShadowLayer();
        endPaint.setColor(Color.BLACK); // 假设你默认文字是黑色，改回来
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