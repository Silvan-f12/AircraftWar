package edu.hitsz; // 请确保包名一致

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;
import edu.hitsz.ScoreRecord.ScoreRecords;
import edu.hitsz.application.AudioManager;

public class GameOverActivity extends AppCompatActivity {

    private AudioManager audioManager = AudioManager.getInstance();
    private TextView tvScore, tvLevel, tvRankInfo;
    private TextView tvOpponentScore;
    private Button btnRestart, btnMenu;
    private String userName;
    private String currentLevel;
    private int finalScore;
    private boolean onlineMode;
    private int opponentScore;

    @Override
    /**
     * 入口：读取结算参数、展示分数与排名提示，并绑定按钮事件。
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over); // 我们稍后创建这个布局

        // 1. 初始化 AudioManager
        audioManager.init(this); // 确保音频服务可用

        // 2. 绑定控件
        tvScore = findViewById(R.id.tv_final_score);
        tvLevel = findViewById(R.id.tv_game_level);
        tvRankInfo = findViewById(R.id.tv_rank_info);
        tvOpponentScore = findViewById(R.id.tv_opponent_score);
        btnRestart = findViewById(R.id.btn_restart);
        btnMenu = findViewById(R.id.btn_main_menu);

        // 3. 获取传递的数据
        Intent intent = getIntent();
        if (intent != null) {
            finalScore = intent.getIntExtra("FINAL_SCORE", 0);
            currentLevel = intent.getStringExtra("GAME_LEVEL");
            userName = intent.getStringExtra("USERNAME");
            onlineMode = intent.getBooleanExtra("ONLINE_MODE", false);
            opponentScore = intent.getIntExtra("OPPONENT_SCORE", 0);
        }

        // 4. 填充数据
        tvScore.setText(String.valueOf(finalScore));
        tvLevel.setText(currentLevel);
        if (onlineMode) {
            tvOpponentScore.setVisibility(View.VISIBLE);
            tvOpponentScore.setText("对手得分: " + opponentScore);
        } else {
            tvOpponentScore.setVisibility(View.GONE);
        }
        // 映射难度为英文标签
        String difficultyTag = getDifficultyTag(currentLevel);
        ScoreRecords scoreRecords = new ScoreRecords(this, difficultyTag);
        // 【调试】打印当前 Activity 打算读取的文件名
        System.out.println("【GameOverActivity】正在尝试读取文件/Key: " + difficultyTag);
        // 这里可以写一个排名信息
        int rank = scoreRecords.getRank(finalScore);
        // 4. 根据排名显示不同的提示语
        displayRankMessage(rank);
//        if (rank <= 20) {
//            tvRankInfo.setText("太棒了！你进入了本周前 " + rank + " 名！");
//        } else {
//            tvRankInfo.setText("再接再厉，离上榜只差一点点！");
//        }

        // 5. 设置按钮点击事件
        setupClickListeners();
    }

    /**
     * 按排名设置结算文案。
     */
    private void displayRankMessage(int rank) {
        if(rank == 1) {
            tvRankInfo.setText("Fantanstic!你就是全网飞机大战的顶流！");
        }
        if (rank <= 20) {
            tvRankInfo.setText("太棒了！你进入了本排行榜前20名！排名为第"+rank+"名！");
        } else {
            tvRankInfo.setText("再接再厉，离上榜只差一点点！");
        }
    }

    /**
     * 绑定“再来一局/返回菜单”两个按钮的点击行为。
     */
    private void setupClickListeners() {
        // 再来一局
        btnRestart.setOnClickListener(v -> {

            // 创建 Intent 回传给 MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("ACTION", "RESTART");
            resultIntent.putExtra("TARGET_LEVEL", currentLevel);
            setResult(RESULT_OK, resultIntent);
            finish(); // 关闭当前 Activity，回到 MainActivity
        });

        // 返回菜单
        btnMenu.setOnClickListener(v -> {
            audioManager.playSound("get_supply");
            Intent intent = new Intent(GameOverActivity.this, SelectActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    /**
     * 将中文难度名映射为存档使用的英文 tag。
     */
    private String getDifficultyTag(String level) {
        switch (level) {
            case "简单模式": return "simple";
            case "中等模式": return "medium";
            case "困难模式": return "difficult";
            default: return "simple";
        }
    }
}