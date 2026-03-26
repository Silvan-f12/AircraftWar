package edu.hitsz;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SelectActivity extends AppCompatActivity {

    private LinearLayout buttonContainer;
    private RadioGroup bottomNavigation;
    private RadioButton rbHome, rbLeaderboard;

    // 用于延迟执行的 Handler
    private final Handler handler = new Handler(Looper.getMainLooper());
    // 用于取消可能存在的延迟任务，防止重复跳转
    private Runnable navigationRunnable;

    // 标记是否正在初始化
    private boolean isInitialized = false;

    private final String[] difficultyLevels = {"简单模式", "中等模式", "困难模式"};
    private final int[] levelColors = {0xFF4CAF50, 0xFFFF9800, 0xFFF44336};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        initDifficultySection();
        initBottomNavigationBar();

        isInitialized = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次页面重新显示时，强制确保“首页”被选中
        if (isInitialized && bottomNavigation != null) {
            if (bottomNavigation.getCheckedRadioButtonId() != R.id.rbHome) {
                rbHome.setChecked(true);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 如果页面要暂停了（比如即将跳转），取消还未执行的延迟任务，防止出错
        if (navigationRunnable != null) {
            handler.removeCallbacks(navigationRunnable);
        }
    }

    private void initDifficultySection() {
        TextView titleText = findViewById(R.id.titleText);
        buttonContainer = findViewById(R.id.buttonContainer);

        buttonContainer.removeAllViews();

        for (int i = 0; i < difficultyLevels.length; i++) {
            final String level = difficultyLevels[i];
            int color = levelColors[i];

            Button btn = new Button(this);
            setupStyledButton(btn, level, color, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(SelectActivity.this, "启动：" + level, Toast.LENGTH_SHORT).show();
                    startGame(level);
                }
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 20, 0, 20);
            btn.setLayoutParams(params);
            buttonContainer.addView(btn);
        }
    }

    private void initBottomNavigationBar() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        rbHome = findViewById(R.id.rbHome);
        rbLeaderboard = findViewById(R.id.rbLeaderboard);

        // 初始选中首页
        rbHome.setChecked(true);

        bottomNavigation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // 如果之前有 pending 的跳转任务，先取消它（防止快速连续点击导致多次跳转）
                if (navigationRunnable != null) {
                    handler.removeCallbacks(navigationRunnable);
                }

                if (checkedId == R.id.rbLeaderboard) {
                    // 在 SelectActivity.java 的 Runnable 中替换 overridePendingTransition 部分
                    navigationRunnable = new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(SelectActivity.this, LeaderBoardActivity.class);
                            startActivity(intent);
                            // SelectActivity.java (保持原样，这是对的)
//                            if (Build.VERSION.SDK_INT >= 35) {
//                                overrideActivityTransition(
//                                        Activity.OVERRIDE_TRANSITION_OPEN,
//                                        R.anim.slide_in_right,   // 从左进
//                                        R.anim.slide_out_left  // 向右出
//                                );
//                            } else overridePendingTransition(
//                                    R.anim.slide_in_left,
//                                    R.anim.slide_out_right
//                            );



                        }
                    };

                    // 延迟 250 毫秒执行跳转 (250ms 是一个比较舒适的视觉反馈时间)
                    handler.postDelayed(navigationRunnable, 250);

                } else if (checkedId == R.id.rbHome) {
                    // 已经在首页，什么都不做
                }
            }
        });
    }

    private void startGame(String level) {
        Intent intent = new Intent(SelectActivity.this, MainActivity.class);
        intent.putExtra("DIFFICULTY", level);
        startActivity(intent);
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void setupStyledButton(Button btn, String text, int borderColor, View.OnClickListener listener) {
        btn.setText(text);
        btn.setTextSize(20);
        btn.setTextColor(0xFF333333);
        btn.setPadding(50, 40, 50, 40);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(50f);
        drawable.setColor(0x00000000);
        drawable.setStroke(4, borderColor);
        btn.setBackground(drawable);
        btn.setOnClickListener(listener);
    }
}