package edu.hitsz.Game;

import android.content.Context;

import edu.hitsz.ScoreRecord.ScoreRecords;
import edu.hitsz.aircraft.AbstractEnemyAircraft;
import edu.hitsz.application.ImageManager;
import edu.hitsz.MySurfaceView;
import edu.hitsz.factory.Enemy_Factory.BossEnemyFactory;

//import javax.imageio.ImageIO;
import java.io.FileInputStream;
import java.io.IOException;

public class DifficultGame extends Game{
    private ScoreRecords scoreRecords;
    private Context context;
    public DifficultGame(Context context, int width, int height){
        super(width, height);
        //难度设置
        difficultyLevel = 3;
        this.context = context;

        // 初始化分数记录
        try {
            scoreRecords = new ScoreRecords(context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize record DAO", e);
        }

    }

    @Override
    public void bossAircraftAppear(int bossScoreThreshold, int bossCnt) {
        if(score >= bossScoreThreshold && (score % bossScoreThreshold <= 50 )&& bossFlags == 0) {
            double randBossSpeedX = Math.random() * (2*MAX_SPEED_X+1)-MAX_SPEED_X;
            enemyBossFactory = new BossEnemyFactory();
            int bossHp = 400+40*bossCnt;
            AbstractEnemyAircraft bossEnemy = enemyBossFactory.createEnemyAircraft(
                    (int) (Math.random() * (MySurfaceView.WINDOW_WIDTH - ImageManager.MOB_ENEMY_IMAGE.getWidth())),
                    100,
                    (int) randBossSpeedX,
                    0,
                    bossHp
            );
            System.out.println("游戏难度提升！boss血量提升为"+bossHp);
            bossEnemy.setScore(50);
            enemyAircrafts.add(bossEnemy);
            bossFlags = 1;

//            bossMusicThread = musicManager.analyseMusic("src/videos/bgm_boss.wav");
//            musicManager.playLoop(bossMusicThread);
        }
    }

    @Override
    //每隔一分钟加难度
    public void setEnemyMaxNumber(int time) {
        if(time > 0 && time % 30000 == 0 && enemyMaxNumber < 10) {
            enemyMaxNumber++;
            System.out.println("游戏难度提升！敌机最大数量为"+enemyMaxNumber+"!");
        }

    }
    @Override
    public double setEnemyShootFreq(int time) {
        double freq = 2.0;
        if(time > 0 && time % 15000 == 0 && freq >= 1) {
            freq -= 0.4;//freq越小，发射频率越大
            System.out.println("游戏难度提升！子弹发射频率加快！子弹发射频率为"+freq*cycleDuration);
        }
        return freq;
    }

    @Override
    public void setEliteEnemyProbability(int time) {
        this.eliteEnemyProbability = 0.5;
        this.eliteEnemyPlusProbability = 0.3;
    }

    @Override
    //敌机产生周期越来越快
    public void setCycleDuration(int time) {
        this.cycleDuration = 400;
    }

    @Override
    public void setEliteEnemyHp(int time) {
        this.eliteEnemyHp = 60;
        if(time > 0 && time % 60000 == 0 && cycleDuration >= 150) {
            this.eliteEnemyHp += 15;
            System.out.println("游戏难度增加，所有精英机血量提升20！精英机血量为："+eliteEnemyHp);
        }
    }

    @Override
    public void setBossScoreTheshold() {
        this.bossScoreTheshold = 800;
    }
}
