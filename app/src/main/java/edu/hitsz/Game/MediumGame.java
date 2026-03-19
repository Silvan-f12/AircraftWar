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

public class MediumGame extends Game{
    private ScoreRecords scoreRecords;
    private Context context;
    public MediumGame(Context context,int width, int height){
        super(width, height);
        difficultyLevel = 2;
        this.context=context;
        try {
            scoreRecords = new ScoreRecords(context);
            // 删除 ImageIO.read 相关代码，图片由 ImageManager.init(context) 统一加载
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
                    400
            );
            bossEnemyAircraft.setScore(50);
            enemyAircrafts.add(bossEnemyAircraft);
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
            System.out.println("游戏难度提升！敌机最大数量为"+enemyMaxNumber+"!");
        }

    }
    @Override
    public double setEnemyShootFreq(int time) {
        double freq = 2.0;
        if(time > 0 && time % 15000 == 0 && freq >= 1) {
            freq -= 0.2;//freq越小，发射频率越大
            System.out.println("游戏难度提升！子弹发射频率加快！");
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


}
