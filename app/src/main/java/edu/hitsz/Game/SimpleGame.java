package edu.hitsz.Game;

import android.content.Context;
import android.util.Log;

import java.time.LocalDateTime;

import edu.hitsz.ScoreRecord.ScoreRecords;
import edu.hitsz.application.ImageManager;
import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.factory.Enemy_Factory.BossEnemyFactory;

public class SimpleGame extends Game {

    private ScoreRecords scoreRecords;
    private Context context;
    // 构造函数需要传入 width 和 height，调用父类构造
    public SimpleGame(Context context, int width, int height,String userName,String difficultyLevel) {
        super(context,width, height, userName,difficultyLevel);
        //难度设置
        //difficultyLevel = 1;
        this.context = context;
        ENEMY_SPEED_X = 5;
        ENEMY_SPEED_Y = 10;
        // 初始化分数记录
        try {
            scoreRecords = new ScoreRecords(context,"simple");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize record DAO", e);
        }
        Log.d("SimpleGame", "Constructor called. Score initialized to: " + this.score);
    }

    @Override
    public void bossAircraftAppear(int bossScoreThreshold, int bossCnt) {
        // Boss 出现逻辑：当分数达到阈值且当前没有 Boss 时生成 Boss
        if (score >= bossScoreThreshold && bossFlags == 0) {
            bossFlags = 1;

            // 创建 Boss 敌机（需要你有 BossEnemyFactory）
            if (enemyBossFactory == null) {
                enemyBossFactory = new BossEnemyFactory();
            }

            // 在屏幕顶部中间位置生成 Boss
            AbstractAircraft boss = enemyBossFactory.createEnemyAircraft(
                    screenWidth / 2,  // X 位置
                    50,               // Y 位置
                    ENEMY_SPEED_X,                // 水平速度
                    ENEMY_SPEED_Y,                // 垂直速度
                    1200               // 血量
            );

            if (boss != null) {
                boss.setScore(100);
                enemyAircrafts.add(boss);
            }
        }
    }

    @Override
    public void setEnemyMaxNumber(int time) {
        enemyMaxNumber = 4;
    }

    @Override
    public double setEnemyShootFreq(int time) {
        return 2.0;
    }

    @Override
    public void setEliteEnemyProbability(int time) {
        this.eliteEnemyProbability = 0.1;
        this.eliteEnemyPlusProbability = 0.05;
    }

    @Override
    public void setCycleDuration(int time) {
        this.cycleDuration = 500;
    }

    @Override
    public void setEliteEnemyHp(int time) {
        this.eliteEnemyHp = 60;
    }

    @Override
    public void setBossScoreTheshold() {
        this.bossScoreTheshold = Integer.MAX_VALUE;  // 简单模式不生成 Boss
    }

    public void insertScore(int score) {
        if (scoreRecords != null) {
            scoreRecords.addPlayerScoreRecordsScore("user0",score);
        }
    }

    public ScoreRecords getScoreRecords() {
        return scoreRecords;
    }
}