package edu.hitsz;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;

public class LeaderBoardActivity extends AppCompatActivity {

    private RadioGroup bottomNavigation;
    private RadioButton rbHome, rbLeaderboard;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable navigationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        bottomNavigation = findViewById(R.id.bottomNavigation);
        rbHome = findViewById(R.id.rbHome);
        rbLeaderboard = findViewById(R.id.rbLeaderboard);

        // 初始状态：确保选中排行榜
        rbLeaderboard.setChecked(true);

        bottomNavigation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // 取消之前的任务
                if (navigationRunnable != null) {
                    handler.removeCallbacks(navigationRunnable);
                }

                if (checkedId == R.id.rbHome) {
                    // ✅ 修正逻辑：点击首页 -> 跳转回 SelectActivity
                    navigationRunnable = () -> {
                        // 1. 创建意图：从 当前页面 (LeaderBoard) -> 目标页面 (Select)
                        Intent intent = new Intent(LeaderBoardActivity.this, SelectActivity.class);

                        // 2. 启动活动
                        startActivity(intent);

//                        if (Build.VERSION.SDK_INT >= 35) {
//                            overrideActivityTransition(
//                                    Activity.OVERRIDE_TRANSITION_CLOSE, // 方向：关闭
//                                    R.anim.slide_in_right,              // 新页面(首页) 从右边滑入
//                                    R.anim.slide_out_left               // 旧页面(排行榜) 向左边滑出
//                            );
//                        } else {
//                            overridePendingTransition(
//                                    R.anim.slide_in_right,              // ✅ 修改
//                                    R.anim.slide_out_left               // ✅ 修改
//                            );
//                        }
                    };

                    // 延迟执行，给用户一点视觉反馈
                    handler.postDelayed(navigationRunnable, 250);

                } else if (checkedId == R.id.rbLeaderboard) {
                    // 点击当前页，什么都不做
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 页面不可见时取消延迟任务，防止内存泄漏或错误跳转
        if (navigationRunnable != null) {
            handler.removeCallbacks(navigationRunnable);
        }
    }


}