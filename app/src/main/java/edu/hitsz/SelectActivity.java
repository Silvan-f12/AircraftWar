package edu.hitsz;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.BuildCompat;

import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

public class SelectActivity extends AppCompatActivity {

    private LinearLayout buttonContainer;
    private RadioGroup bottomNavigation;
    private RadioButton rbHome, rbLeaderboard;

    // 新增：音频控制 UI
    private SeekBar volumeSeekBar;
    private TextView volumeValueText;
    private CheckBox cbAudioSwitch; // 独立的声音总开关
    // 新增：独立的音效音量控制 UI
    private SeekBar sfxVolumeSeekBar;
    private TextView sfxVolumeValueText;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable navigationRunnable;
    private boolean isInitialized = false;

    // 默认状态
    private float currentBgmVolume = 0.5f;
    private float currentSfxVolume = 0.5f;      // 音效音量 (新增)
    private boolean isAudioEnabled = true; // 默认开启

    private final String[] difficultyLevels = {"简单模式", "中等模式", "困难模式"};
    private final int[] levelColors = {0xFF4CAF50, 0xFFFF9800, 0xFFF44336};

    private OnBackInvokedCallback mBackCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        initDifficultySection();
        initAudioControls(); // 初始化音量和开关
        initBottomNavigationBar();

        isInitialized = true;
        enablePredictiveBackGesture();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isInitialized && bottomNavigation != null) {
            if (bottomNavigation.getCheckedRadioButtonId() != R.id.rbHome) {
                rbHome.setChecked(true);
            }
        }
        // 同步 UI 状态
        if (volumeSeekBar != null) volumeSeekBar.setProgress((int) (currentBgmVolume * 100));
        if (volumeValueText != null) volumeValueText.setText((int)(currentBgmVolume * 100) + "%");
        // --- 新增：同步音效滑块 ---
        if (sfxVolumeSeekBar != null) sfxVolumeSeekBar.setProgress((int) (currentSfxVolume * 100));
        if (sfxVolumeValueText != null) sfxVolumeValueText.setText((int)(currentSfxVolume * 100) + "%");

        if (cbAudioSwitch != null) cbAudioSwitch.setChecked(isAudioEnabled);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (navigationRunnable != null) {
            handler.removeCallbacks(navigationRunnable);
        }
    }

    /**
     * 初始化音频控制区域 (开关 + 背景音量条 + 音效音量条)
     */
    private void initAudioControls() {
        // 外层容器
        LinearLayout audioLayout = new LinearLayout(this);
        audioLayout.setOrientation(LinearLayout.VERTICAL);
        audioLayout.setPadding(40, 20, 40, 20);

        // --- 第一行：总开关 ---
        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        switchRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        cbAudioSwitch = new CheckBox(this);
        cbAudioSwitch.setText("开启声音 (音乐/音效)");
        cbAudioSwitch.setTextSize(18);
        cbAudioSwitch.setTextColor(0xFF333333);
        cbAudioSwitch.setChecked(isAudioEnabled);

        cbAudioSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAudioEnabled = isChecked;
            // 同步更新两个滑块的状态
            if (volumeSeekBar != null) volumeSeekBar.setEnabled(isChecked);
            if (sfxVolumeSeekBar != null) sfxVolumeSeekBar.setEnabled(isChecked);

            // 简单刷新一下文字颜色 (可选，更严谨的做法是遍历子视图)
            // 这里主要依赖 onResume 或 SeekBar 创建时的初始状态
        });

        switchRow.addView(cbAudioSwitch);
        audioLayout.addView(switchRow);

        // --- 第二行：背景音乐音量 ---
        // 传入 false 表示这是 BGM 行，会将 UI 引用赋值给 volumeSeekBar
        createVolumeRow(audioLayout, "背景音乐: ", currentBgmVolume, false);

        // --- 第三行：游戏音效音量 ---
        // 传入 true 表示这是 SFX 行，会将 UI 引用赋值给 sfxVolumeSeekBar
        createVolumeRow(audioLayout, "游戏音效: ", currentSfxVolume, true);

        if (buttonContainer != null) {
            buttonContainer.addView(audioLayout);
        }
    }
    // 辅助方法：创建音量行，避免代码重复
    /**
     * 【通用方法】创建一行音量控制 UI (标签 + 滑块 + 数值)
     * @param parent 父容器
     * @param labelText 标签文字
     * @param initialVolume 初始音量值
     * @param isSfxRow true=音效行 (绑定 sfxVolumeSeekBar), false=背景行 (绑定 volumeSeekBar)
     */
    private void createVolumeRow(LinearLayout parent, String labelText, float initialVolume, boolean isSfxRow) {
        LinearLayout volumeRow = new LinearLayout(this);
        volumeRow.setOrientation(LinearLayout.HORIZONTAL);
        volumeRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        volumeRow.setPadding(0, 10, 0, 0);

        // 1. 标签
        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(16);
        label.setTextColor(isAudioEnabled ? 0xFF666666 : 0xFFCCCCCC);

        // 2. 滑块
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(100);
        seekBar.setProgress((int) (initialVolume * 100));
        seekBar.setEnabled(isAudioEnabled);

        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        seekParams.setMargins(20, 0, 20, 0);
        seekBar.setLayoutParams(seekParams);

        // 3. 数值文本
        TextView valueText = new TextView(this);
        valueText.setText((int)(initialVolume * 100) + "%");
        valueText.setTextSize(14);
        valueText.setTextColor(isAudioEnabled ? 0xFF666666 : 0xFFCCCCCC);
        valueText.setWidth(100);

        // 4. 绑定滑块事件
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newVal = progress / 100.0f;
                valueText.setText(progress + "%");

                // 根据是哪一行，更新对应的成员变量
                if (isSfxRow) {
                    currentSfxVolume = newVal;
                } else {
                    currentBgmVolume = newVal;
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 5. 【关键】保存 UI 引用到类成员变量，以便 onResume 中同步状态
        if (isSfxRow) {
            this.sfxVolumeSeekBar = seekBar;
            this.sfxVolumeValueText = valueText;
        } else {
            this.volumeSeekBar = seekBar;
            this.volumeValueText = valueText;
        }

        // 6. 监听总开关变化来动态更新这一行的状态 (颜色和启用状态)
        // 注意：这里需要移除旧的监听器避免重复，但 CheckBox 通常直接设置新的也没大问题，
        // 更严谨的做法是在外部统一控制，这里为了简单直接在内部引用外部 cbAudioSwitch
        if (cbAudioSwitch != null) {
            // 为了防止多次添加监听器导致回调多次，实际生产中最好用 tag 标记或移除旧监听
            // 但在此简单场景下，我们主要依赖初始化时的状态。
            // 如果需要动态切换，建议在 cbAudioSwitch 的监听器里遍历 audioLayout 的子视图来更新。
            // 此处仅做初始化颜色设置，动态变化主要靠 isEnabled 属性。
        }

        volumeRow.addView(label);
        volumeRow.addView(seekBar);
        volumeRow.addView(valueText);
        parent.addView(volumeRow);
    }

    private void initDifficultySection() {
        TextView titleText = findViewById(R.id.titleText);
        buttonContainer = findViewById(R.id.buttonContainer);
        if (buttonContainer == null) return;
        buttonContainer.removeAllViews();

        for (int i = 0; i < difficultyLevels.length; i++) {
            final String level = difficultyLevels[i];
            int color = levelColors[i];

            Button btn = new Button(this);
            setupStyledButton(btn, level, color, v -> {
                String status = isAudioEnabled ? "开" : "关";
                Toast.makeText(SelectActivity.this, "启动：" + level + " | 声音:" + status + " 音量:" + (int)(currentBgmVolume*100) + "%", Toast.LENGTH_SHORT).show();
                startGame(level);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 20, 0, 20);
            btn.setLayoutParams(params);
            buttonContainer.addView(btn);
        }
    }

    /**
     * 底部导航栏样式，首页和排行榜两种功能*/
    private void initBottomNavigationBar() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        rbHome = findViewById(R.id.rbHome);
        rbLeaderboard = findViewById(R.id.rbLeaderboard);
        rbHome.setChecked(true);

        bottomNavigation.setOnCheckedChangeListener((group, checkedId) -> {
            if (navigationRunnable != null) handler.removeCallbacks(navigationRunnable);
            if (checkedId == R.id.rbLeaderboard) {
                navigationRunnable = () -> {
                    Intent intent = new Intent(SelectActivity.this, LeaderBoardActivity.class);
                    startActivity(intent);
                    if (Build.VERSION.SDK_INT >= 35) {
                        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left);
                    } else {
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    }
                };
                handler.postDelayed(navigationRunnable, 200);
            }
        });
    }

    private void startGame(String level) {
        Intent intent = new Intent(SelectActivity.this, MainActivity.class);
        intent.putExtra("DIFFICULTY", level);

        // --- 关键：传递两个参数 ---
        intent.putExtra("AUDIO_ENABLED", isAudioEnabled);
        intent.putExtra("BGM_VOLUME", currentBgmVolume);   // 背景音乐音量
        intent.putExtra("SFX_VOLUME", currentSfxVolume);   // 音效音量 (新增)
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

    private void enablePredictiveBackGesture() {
        // 2. 使用安全的 BuildCompat 检查
        if (BuildCompat.isAtLeastT()) {
            // 3. 使用 Lambda 简化实现（这才是现代写法！）
            mBackCallback = () -> {
                // 逻辑：关闭页面
                finish();
            };

            // 4. 注册
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    mBackCallback
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(mBackCallback);
        }
    }
}