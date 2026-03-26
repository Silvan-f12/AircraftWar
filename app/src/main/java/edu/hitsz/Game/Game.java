package edu.hitsz.Game;

import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.RectF;

import edu.hitsz.R;
import edu.hitsz.aircraft.*;
import edu.hitsz.application.AudioManager;
import edu.hitsz.application.ImageManager;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.factory.Enemy_Factory.*;
import edu.hitsz.factory.Prop_Factory.*;
import edu.hitsz.props.*;
import edu.hitsz.observers.*;
import edu.hitsz.ScoreRecord.PlayerScore;
import edu.hitsz.ScoreRecord.ScoreRecords;

import java.util.*;
import java.util.List;

/**
 * 游戏核心逻辑类 (Android 适配版)
 * 不再继承 JPanel，不再负责绘图循环，只负责状态更新和绘制指令
 *
 * @author hitsz
 */
public abstract class Game {

    private int backGroundTop = 0;

    // 【关键】固定时间间隔 (60FPS = 16ms)
    private final long timeInterval = 40;

    // 英雄机创建实例
    private final HeroAircraft heroAircraft;

    // 敌机创建工厂
    protected BossEnemyFactory enemyBossFactory;
    private EliteEnemyFactory enemyEliteFactory;
    private EliteEnemyPlusFactory enemyElitePlusFactory;
    private MobEnemyFactory enemyMobFactory;

    // 敌机创建实例列表
    protected final List<AbstractAircraft> enemyAircrafts;

    // 英雄机发射子弹列表
    private final List<BaseBullet> heroBullets;

    // 敌机发射子弹的列表
    private final List<BaseBullet> enemyBullets;

    // 道具工厂
    private HpPropFactory propHpFactory;
    private FireSupplyPropFactory propFireFactory;
    private BombSupplyPropFactory propBombFactory;
    private FireSupplyPlusPropFactory propFirePlusFactory;

    // 道具列表
    private final List<Prop> props;

    /**
     * 屏幕中出现的敌机最大数量
     */
    protected int enemyMaxNumber = 3;

    /**
     * 当前得分
     */
    protected int score = 0;

    /**
     * 当前时刻
     */
    private int time = 0;

    /**
     * 周期（ms) 指示子弹的发射、敌机的产生频率
     */
    protected int cycleDuration = 80;
    private int cycleTime = 0;

    /**
     * 定义敌机的速度
     */
    protected int ENEMY_SPEED_X = 5;
    protected int ENEMY_SPEED_Y = 10;

    /**
     * 定义精英机和道具的产生频率
     */
    protected double eliteEnemyProbability = 0.3;
    protected double eliteEnemyPlusProbability = 0.2;
    protected int bossFlags = 0;
    private int bossCnt = 0;

    /**
     * 设置不同难度精英机的血量
     */
    protected int eliteEnemyHp = 60;
    protected int bossScoreTheshold = 1200;

    // 定义道具产生频率
    private double hpPropProbability = 0.3;
    private double firePropProbability = 0.1;
    private double bombPropProbability = 0.5;
    private double firePlusPropProbability = 0.1;
    // 记录距离上次敌机射击过去了多少毫秒
    private double enemyShootTimer = 0;

    // 精英机最大水平速度
    protected int MAX_SPEED_X = 10;

    /**
     * 游戏结束标志
     */
    private boolean gameOverFlag = false;

    // 玩家游戏记录数据库
    private PlayerScore playerScore;
    protected ScoreRecords scoreRecords;

    // 游戏难度，默认为简单模式
    protected int difficultyLevel = 1;

    // 观察者实现炸弹
    private MobEnemyObserver mobEnemyObserver;
    private EliteEnemyObserver eliteEnemyObserver;
    private EliteEnemyPlusObserver eliteEnemyPlusObserver;
    private EnemyBulletObserver enemyBulletObserver;

    // 屏幕尺寸 (由外部传入)
    protected int screenWidth;
    protected int screenHeight;

    // 绘图用的 Paint 对象
    private final Paint scorePaint;

    // ==================== 构造函数 ====================
    public Game(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        // 初始化英雄机
        Bitmap heroImg = ImageManager.HERO_IMAGE;
        int initW = heroImg != null ? heroImg.getWidth() : 50;
        int initH = heroImg != null ? heroImg.getHeight() : 50;

        HeroAircraft.resetInstance();
        HeroAircraft.initHeroAircraft(
                width / 2,
                height - initH,
                0,
                0,
                1000
        );

        heroAircraft = HeroAircraft.getInstance();
        enemyAircrafts = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();
        props = new LinkedList<>();

        propHpFactory = new HpPropFactory();
        propFireFactory = new FireSupplyPropFactory();
        propFirePlusFactory = new FireSupplyPlusPropFactory();
        propBombFactory = new BombSupplyPropFactory();

        // 初始化绘图工具
        scorePaint = new Paint();
        scorePaint.setColor(Color.RED);
        scorePaint.setTextSize(40);
        scorePaint.setTypeface(Typeface.DEFAULT_BOLD);
        scorePaint.setAntiAlias(true);

        // 初始化观察者
        mobEnemyObserver = new MobEnemyObserver();
        eliteEnemyObserver = new EliteEnemyObserver();
        eliteEnemyPlusObserver = new EliteEnemyPlusObserver();
        enemyBulletObserver = new EnemyBulletObserver();
    }

    public ScoreRecords getScoreRecords() {
        return scoreRecords;
    }

    public void insertScore(int score) {
        scoreRecords.addPlayerScoreRecordsScore("user0", score);
    }

    /**
     * 核心逻辑更新方法
     * 在 SurfaceView 的渲染循环中直接调用此方法
     */
    public void updateLogic() {
        if (gameOverFlag) {
            return;
        }

        // 【关键】使用固定时间增量 (16ms = 60FPS)
        time += (int) timeInterval;
        setCycleDuration(time);
        setEliteEnemyProbability(time);
        setEliteEnemyHp(time);
        setBossScoreTheshold();
        //敌机子弹发射周期变化
        double currentShootInterval = cycleDuration * setEnemyShootFreq(time);

        if (timeCountAndNewCycleJudge()) {
            setEnemyMaxNumber(time);

            if (enemyAircrafts.size() < enemyMaxNumber) {
                bossAircraftAppear(bossScoreTheshold, ++bossCnt);
                spawnEnemy();
            }

            shootActionHero();

//            if (time % (cycleDuration * setEnemyShootFreq(time)) == 0) {
//                shootActionEnemy();
//            }
        }
        // ================= 新增：独立的射击计时逻辑 =================
        // 无论是否在生成周期内，每帧都更新射击计时器
        enemyShootTimer += timeInterval; // 累加经过的时间 (40ms)

        // 如果累积时间超过了当前需要的间隔
        if (enemyShootTimer >= currentShootInterval) {
            shootActionEnemy(); // 开火！

            // 关键：减去消耗的间隔，保留剩余的时间（防止帧率波动导致漏射或多射）
            enemyShootTimer -= currentShootInterval;
        }

        bulletsMoveAction();
        aircraftsMoveAction();
        propsMoveAction();
        crashCheckAction();
        postProcessAction();

        if (heroAircraft.getHp() <= 0) {
            // 【删除】executorService.shutdown(); 已移除线程池
            gameOverFlag = true;
            onGameOver();
        }
    }

    /**
     * 生成敌机
     */
    private void spawnEnemy() {
        double random = Math.random();
        int xBound = screenWidth - 50;

        if (random < eliteEnemyProbability) {
            enemyEliteFactory = new EliteEnemyFactory();
            AbstractAircraft eliteEnemy = enemyEliteFactory.createEnemyAircraft(
                    (int) (Math.random() * xBound),
                    (int) (Math.random() * screenHeight * 0.05),
                    ENEMY_SPEED_X,
                    ENEMY_SPEED_Y,
                    eliteEnemyHp
            );
            eliteEnemy.setScore(30);
            enemyAircrafts.add(eliteEnemy);
        } else if (random < eliteEnemyProbability + eliteEnemyPlusProbability) {
            enemyElitePlusFactory = new EliteEnemyPlusFactory();
            AbstractAircraft elitePlusEnemy = enemyElitePlusFactory.createEnemyAircraft(
                    (int) (Math.random() * xBound),
                    (int) (Math.random() * screenHeight * 0.05),
                    ENEMY_SPEED_X,
                    ENEMY_SPEED_Y,
                    eliteEnemyHp
            );
            elitePlusEnemy.setScore(30);
            enemyAircrafts.add(elitePlusEnemy);
        } else {
            enemyMobFactory = new MobEnemyFactory();
            AbstractAircraft mobEnemy = enemyMobFactory.createEnemyAircraft(
                    (int) (Math.random() * xBound),
                    (int) (Math.random() * screenHeight * 0.05),
                    0,
                    ENEMY_SPEED_Y,
                    eliteEnemyHp / 2
            );
            mobEnemy.setScore(20);
            enemyAircrafts.add(mobEnemy);
        }
    }

    /**
     * 判断是否进入新的周期
     */
    private boolean timeCountAndNewCycleJudge() {
        cycleTime += timeInterval;
        if (cycleTime >= cycleDuration) {
            cycleTime %= cycleDuration;
            return true;
        } else {
            return false;
        }
    }

    /**
     * 敌机射击
     */
    private void shootActionEnemy() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            if (!enemyAircraft.notValid()) {
                enemyBullets.addAll(enemyAircraft.shoot());
            }
        }
    }

    /**
     * 英雄射击
     */
    private void shootActionHero() {
        if (!heroAircraft.notValid()) {
            heroBullets.addAll(heroAircraft.shoot());
        }
        //添加英雄机音效
        if(time % (2*timeInterval)==0) {
            AudioManager.getInstance().playSound("bullet");
        }
    }

    /**
     * 子弹移动
     */
    private void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (BaseBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    /**
     * 飞机移动
     */
    private void aircraftsMoveAction() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            enemyAircraft.forward();
        }
    }

    /**
     * 道具向下移动
     */
    public void propsMoveAction() {
        for (Prop prop : props) {
            prop.forward();
        }
    }

    /**
     * Boss 机产生函数
     */
    public abstract void bossAircraftAppear(int bossScoreThreshold, int bossCnt);

    /**
     * 设置最大敌机数量
     */
    public abstract void setEnemyMaxNumber(int time);

    /**
     * 设置敌机的射击周期
     */
    public abstract double setEnemyShootFreq(int time);

    /**
     * 设置精英和精英 plus 敌机产生的概率
     */
    public abstract void setEliteEnemyProbability(int time);

    /**
     * 设置敌机产生周期
     */
    public abstract void setCycleDuration(int time);

    /**
     * 设置敌机血量上升
     */
    public abstract void setEliteEnemyHp(int time);

    /**
     * 设置 boss 敌机的分数阈值
     */
    public abstract void setBossScoreTheshold();

    /**
     * 敌机摧毁获得道具和分数
     */
    public void EnemyCrashGetScoreAndProp(AbstractAircraft enemyAircraft) {
        if (enemyAircraft.notValid()) {
            return;
        }

        // 获得分数
        score += enemyAircraft.getScore();

        // 普通敌机不掉落道具
        if (enemyAircraft instanceof MobEnemy) {
            enemyAircraft.vanish();
            return;
        }

        // 精英敌机掉落道具
        if (enemyAircraft instanceof EliteEnemyAircraft ||
                enemyAircraft instanceof EliteEnemyPlusAircraft) {
            double random = Math.random();
            if (random < hpPropProbability) {
                props.add(propHpFactory.createProp(
                        enemyAircraft.getLocationX(),
                        enemyAircraft.getLocationY(),
                        0,
                        6
                ));
            } else if (random < bombPropProbability + hpPropProbability) {
                props.add(propBombFactory.createProp(
                        enemyAircraft.getLocationX(),
                        enemyAircraft.getLocationY(),
                        0,
                        6
                ));
            } else if (random < bombPropProbability + hpPropProbability + firePropProbability) {
                props.add(propFireFactory.createProp(
                        enemyAircraft.getLocationX(),
                        enemyAircraft.getLocationY(),
                        0,
                        6
                ));
            } else if (random < bombPropProbability + hpPropProbability +
                    firePropProbability + firePlusPropProbability) {
                props.add(propFirePlusFactory.createProp(
                        enemyAircraft.getLocationX(),
                        enemyAircraft.getLocationY(),
                        0,
                        6
                ));
            }
            enemyAircraft.vanish();
        }

        // Boss 敌机掉落 1-3 个道具
        if (enemyAircraft instanceof BossEnemyAircraft) {
            bossFlags = 0;
            int propCount = (int) (Math.random() * 3) + 1;
            int baseX = enemyAircraft.getLocationX();
            int baseY = enemyAircraft.getLocationY();

            for (int i = 0; i < propCount; i++) {
                double random = Math.random();
                int offsetX = (i - propCount / 2) * 30;

                if (random < hpPropProbability) {
                    props.add(propHpFactory.createProp(baseX + offsetX, baseY, 0, 6));
                } else if (random < bombPropProbability + hpPropProbability) {
                    props.add(propBombFactory.createProp(baseX + offsetX, baseY, 0, 6));
                } else if (random < bombPropProbability + hpPropProbability + firePropProbability) {
                    props.add(propFireFactory.createProp(baseX + offsetX, baseY, 0, 6));
                } else if (random < bombPropProbability + hpPropProbability +
                        firePropProbability + firePlusPropProbability) {
                    props.add(propFirePlusFactory.createProp(baseX + offsetX, baseY, 0, 6));
                }
            }
            enemyAircraft.vanish();
            AudioManager.getInstance().stopBgm();
        }
    }

    /**
     * 碰撞检测
     */
    private void crashCheckAction() {
        // 敌机子弹攻击英雄机
        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) {
                continue;
            }
            if (heroAircraft.crash(bullet)) {
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }

        // 英雄子弹攻击敌机
        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractAircraft enemyAircraft : enemyAircrafts) {
                if (enemyAircraft.notValid()) {
                    continue;
                }
                if (enemyAircraft.crash(bullet)) {
                    enemyAircraft.decreaseHp(bullet.getPower());
                    bullet.vanish();
                    EnemyCrashGetScoreAndProp(enemyAircraft);
                    AudioManager.getInstance().playSound("bullet_hit");
                }
                // 英雄机与敌机相撞
                if (enemyAircraft.crash(heroAircraft) || heroAircraft.crash(enemyAircraft)) {
                    enemyAircraft.decreaseHp(Integer.MAX_VALUE);
                    enemyAircraft.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                    AudioManager.getInstance().playSound("game)over");
                }
            }
        }

        // 英雄获得道具
        for (Prop prop : props) {
            if (prop.notValid()) {
                continue;
            }
            if (heroAircraft.notValid()) {
                break;
            }
            if (heroAircraft.crash(prop)) {
                if (prop instanceof HpProp) {
                    heroAircraft.increaseHp(prop.propValid());
                    AudioManager.getInstance().playSound("get_supply");
                    prop.vanish();
                }
                if (prop instanceof FireSupplyProp) {
                    prop.propValid();
                    prop.vanish();
                }
                if (prop instanceof FireSupplyPlusProp) {
                    prop.propValid();
                    prop.vanish();
                }
                if (prop instanceof BombSupplyProp) {
                    AudioManager.getInstance().playSound("bomb_explosion");
                    prop.addObserver(mobEnemyObserver);
                    prop.addObserver(eliteEnemyPlusObserver);
                    prop.addObserver(eliteEnemyObserver);
                    prop.addObserver(enemyBulletObserver);
                    prop.propValid();
                    prop.removeObserver(mobEnemyObserver);
                    prop.removeObserver(eliteEnemyObserver);
                    prop.removeObserver(eliteEnemyPlusObserver);
                    prop.removeObserver(enemyBulletObserver);
                    prop.vanish();
                }
            }
        }
    }

    /**
     * 后处理：删除无效的物体
     */
    private void postProcessAction() {
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyAircrafts.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractFlyingObject::notValid);
    }

    /**
     * 绘制方法 (由 MySurfaceView 传入 Canvas)
     */
    public void draw(Canvas canvas) {
        if (canvas == null) {
            return;
        }

        // ==================== 绘制背景 (滚动 + 适配分辨率) ====================
        if (ImageManager.BACKGROUND_IMAGE != null) {
            Bitmap bg = ImageManager.BACKGROUND_IMAGE;

            // 1. 计算缩放比例 (CENTER_CROP 策略，保持宽高比)
            float scale = Math.max(
                    (float) screenWidth / bg.getWidth(),
                    (float) screenHeight / bg.getHeight()
            );

            // 2. 计算缩放后的尺寸
            int scaledWidth = (int) (bg.getWidth() * scale);
            int scaledHeight = (int) (bg.getHeight() * scale);

            // 3. 计算绘制起始位置 (居中裁剪)
            float left = (screenWidth - scaledWidth) / 2f;
            float top1 = backGroundTop - screenHeight;  // 上方背景
            float top2 = backGroundTop;                  // 下方背景

            // 4. 绘制两张背景图实现滚动效果
            canvas.drawBitmap(bg, null,
                    new RectF(left, top1, left + scaledWidth, top1 + scaledHeight),
                    null);
            canvas.drawBitmap(bg, null,
                    new RectF(left, top2, left + scaledWidth, top2 + scaledHeight),
                    null);

            // 5. 更新滚动位置
            backGroundTop += 2;
            if (backGroundTop >= screenHeight) {
                backGroundTop = 0;
            }
        } else {
            canvas.drawColor(Color.BLACK);
        }

        // ==================== 绘制游戏对象 ====================

        // 绘制子弹
        drawObjects(canvas, enemyBullets);
        drawObjects(canvas, heroBullets);

        // 绘制敌机
        drawObjects(canvas, enemyAircrafts);

        // 绘制道具
        drawObjects(canvas, props);

        // 绘制英雄
        if (!heroAircraft.notValid() && ImageManager.HERO_IMAGE != null) {
            Bitmap img = ImageManager.HERO_IMAGE;
            canvas.drawBitmap(
                    img,
                    heroAircraft.getLocationX() - img.getWidth() / 2f,
                    heroAircraft.getLocationY() - img.getHeight() / 2f,
                    null
            );
        }

        // 绘制分数和血量
        scorePaint.setColor(Color.RED);
        canvas.drawText("SCORE: " + score, 20, 150, scorePaint);
        canvas.drawText("LIFE: " + heroAircraft.getHp(), 20, 190, scorePaint);

        // 游戏结束提示
        if (gameOverFlag) {
            scorePaint.setColor(Color.WHITE);
            scorePaint.setTextSize(60);
            String text = "GAME OVER";
            float textWidth = scorePaint.measureText(text);
            canvas.drawText(text, (screenWidth - textWidth) / 2, screenHeight / 2, scorePaint);
        }
    }

    /**
     * 绘制物体列表
     */
    private void drawObjects(Canvas canvas, List<? extends AbstractFlyingObject> objects) {
        if (objects == null || objects.size() == 0) {
            return;
        }
        for (AbstractFlyingObject object : objects) {
            if (object.notValid()) {
                continue;
            }
            Bitmap image = object.getImage();
            if (image != null) {
                canvas.drawBitmap(
                        image,
                        object.getLocationX() - image.getWidth() / 2f,
                        object.getLocationY() - image.getHeight() / 2f,
                        null
                );
            }
        }
    }

    /**
     * 游戏结束回调
     */
    protected void onGameOver() {
        // 子类可以实现保存分数等操作
    }

    /**
     * 释放资源 (内存优化)
     */
    public void release() {
        gameOverFlag = true;
        enemyAircrafts.clear();
        heroBullets.clear();
        enemyBullets.clear();
        props.clear();
    }

    public boolean isGameOver() {
        return gameOverFlag;
    }

    public int getScore() {
        return score;
    }

    public HeroAircraft getHeroAircraft() {
        return heroAircraft;
    }

    /**
     * 供外部调用，更新英雄位置 (触屏控制)
     */
    public void moveHeroTo(float x, float y) {
        if (heroAircraft != null && !heroAircraft.notValid()) {
            Bitmap heroImg = ImageManager.HERO_IMAGE;
            int w = heroImg != null ? heroImg.getWidth() : 50;
            int h = heroImg != null ? heroImg.getHeight() : 50;

            float clampX = Math.max(w / 2f, Math.min(x, screenWidth - w / 2f));
            float clampY = Math.max(h / 2f, Math.min(y, screenHeight - h / 2f));

            heroAircraft.setLocation((int) clampX, (int) clampY);
        }
    }

    // ==================== 内部观察者类 ====================

    public class EnemyBulletObserver implements BombObserver {
        @Override
        public void update(BombSupplyProp subject) {
            for (BaseBullet bullet : enemyBullets) {
                bullet.vanish();
            }
        }
    }

    public class EliteEnemyObserver implements BombObserver {
        @Override
        public void update(BombSupplyProp subject) {
            if (!enemyAircrafts.isEmpty()) {
                for (AbstractAircraft abstractAircraft : enemyAircrafts) {
                    if (abstractAircraft instanceof EliteEnemyAircraft) {
                        score += abstractAircraft.getScore();
                        abstractAircraft.vanish();
                    }
                }
            }
        }
    }

    public class EliteEnemyPlusObserver implements BombObserver {
        @Override
        public void update(BombSupplyProp subject) {
            if (!enemyAircrafts.isEmpty()) {
                for (AbstractAircraft abstractAircraft : enemyAircrafts) {
                    if (abstractAircraft instanceof EliteEnemyPlusAircraft) {
                        abstractAircraft.decreaseHp(30);
                    }
                }
            }
        }
    }

    public class MobEnemyObserver implements BombObserver {
        @Override
        public void update(BombSupplyProp subject) {
            if (!enemyAircrafts.isEmpty()) {
                for (AbstractAircraft abstractAircraft : enemyAircrafts) {
                    if (abstractAircraft instanceof MobEnemy) {
                        score += abstractAircraft.getScore();
                        abstractAircraft.vanish();
                    }
                }
            }
        }
    }
}