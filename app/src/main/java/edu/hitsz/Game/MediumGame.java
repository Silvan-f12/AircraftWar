package edu.hitsz.Game;

import static android.graphics.BitmapFactory.decodeResource;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.widget.ImageViewCompat;

import edu.hitsz.R;
import edu.hitsz.ScoreRecord.ScoreRecords;
import edu.hitsz.aircraft.AbstractEnemyAircraft;
import edu.hitsz.application.AudioManager;
import edu.hitsz.application.ImageManager;
import edu.hitsz.MySurfaceView;
import edu.hitsz.factory.Enemy_Factory.BossEnemyFactory;

//import javax.imageio.ImageIO;
import java.io.FileInputStream;
import java.io.IOException;

public class MediumGame extends Game{
    private ScoreRecords scoreRecords;
    private Context context;
    // 添加一个 Handler，绑定到主线程 (Looper.getMainLooper())
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private double freq = 2.0;
    public MediumGame(Context context, int width, int height,String userName,String difficultyLevel){
        super(context,width, height,userName,difficultyLevel);
        //difficultyLevel = 2;
        this.context=context;
        ENEMY_SPEED_X = 5;
        ENEMY_SPEED_Y = 10;
        try {
            scoreRecords = new ScoreRecords(context,"medium");
            // 删除 ImageIO.read 相关代码，图片由 ImageManager.init(context) 统一加载
            ImageManager.BACKGROUND_IMAGE = decodeResource(context.getResources(), R.drawable.bg3);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize record DAO", e);
        }
    }

    @Override
    public void bossAircraftAppear(int bossScoreThreshold, int bossCnt) {
        if(score >= bossScoreThreshold && (score % bossScoreThreshold <= 50 )&& bossFlags == 0) {
            double randBossSpeedX = Math.random() * (2*MAX_SPEED_X+1)-MAX_SPEED_X;
            enemyBossFactory = new BossEnemyFactory();
            AbstractEnemyAircraft bossEnemyAircraft = enemyBossFactory.createEnemyAircraft(
                    (int) (Math.random() * (MySurfaceView.WINDOW_WIDTH - ImageManager.MOB_ENEMY_IMAGE.getWidth())),
                    100,
                    (int) randBossSpeedX,
                    0,
                    800
            );
            AudioManager.getInstance().playBgm(R.raw.bgm_boss);
            bossEnemyAircraft.setScore(50);
            enemyAircrafts.add(bossEnemyAircraft);
            showGameToast("警告：Boss 战机出现！");
            bossFlags = 1;

//            bossMusicThread = musicManager.analyseMusic("src/videos/bgm_boss.wav");
//            musicManager.playLoop(bossMusicThread);
        }
    }
    /**
     * 每隔半分钟加难度*/
    @Override
    public void setEnemyMaxNumber(int time) {
        if(time > 0 && time % 30000 == 0  && enemyMaxNumber < 8) {
            enemyMaxNumber++;
            //System.out.println("游戏难度提升！敌机最大数量为"+enemyMaxNumber+"!");
            String msg = "难度提升！敌机数量上限增加到 " + enemyMaxNumber;
            System.out.println(msg);

            // 【新增】通知用户难度提升
            showGameToast(msg);
        }

    }
    @Override
    public double setEnemyShootFreq(int time) {

        if(time > 0 && time % 15000 == 0) {
            // 防止频率过低导致除以零或负数，设置下限
            if (freq > 0.5) {
                freq -= 0.2;
            }
            //System.out.println("游戏难度提升！子弹发射频率加快！");
            String msg = "警告：敌方火力变猛了！";
            System.out.println(msg);
            showGameToast(msg);
        }
        return freq;
    }

    @Override
    public void setEliteEnemyProbability(int time) {
        this.eliteEnemyProbability = 0.4;
        this.eliteEnemyPlusProbability = 0.25;
    }

    @Override
    public void setCycleDuration(int time) {
        this.cycleDuration = 500;
        //敌机产生周期不变
    }

    @Override
    public void setEliteEnemyHp(int time) {
        this.eliteEnemyHp = 60;
    }

    @Override
    //设置分数阈值
    public void setBossScoreTheshold() {
        this.bossScoreTheshold = 1000;
    }
    public void insertScore(int score) {
        if (scoreRecords != null) {
            scoreRecords.addPlayerScoreRecordsScore("user0",score);
        }
    }

    public ScoreRecords getScoreRecords() {
        return scoreRecords;
    }
    /**
     * 辅助方法：在主线程显示 Toast
     * 即使游戏逻辑在子线程运行，也能安全地更新 UI
     */
    private void showGameToast(String message) {
        mainHandler.post(() -> {
            if (context != null) {
                Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
                // 可选：设置显示位置，避免被飞机遮挡，比如显示在顶部
                // toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
                toast.show();
            }
        });
    }
}
