package edu.hitsz.aircraft;

import android.util.Log;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.shoot_strategy.ShootFanShaped;
import edu.hitsz.shoot_strategy.ShootStraight;

import java.util.List;

/**
 * 英雄飞机，游戏玩家操控
 * @author hitsz
 */
public class HeroAircraft extends AbstractAircraft {

    /** 单例实例 */
    private static volatile HeroAircraft instance;
    /** 创建单例需要用到的参数*/
    public static int sin_locationX;
    public static int sin_locationY;
    public static int sin_speedX;
    public static int sin_speedY;
    public static int sin_hp;
    /**攻击方式 */

    /**
     * 子弹一次发射数量
     */
    private int shootNum = 1;

    /**
     * 子弹伤害
     */
    private int power = 30;

    /**
     * 子弹射击方向 (向上发射：-1，向下发射：1)
     */
    private int direction = -1;

    /**
     * @param locationX 英雄机位置x坐标
     * @param locationY 英雄机位置y坐标
     * @param speedX 英雄机射出的子弹的基准速度（英雄机无特定速度）
     * @param speedY 英雄机射出的子弹的基准速度（英雄机无特定速度）
     * @param hp    初始生命值
     */
    public HeroAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        setShootStrategy(new ShootStraight());//初始为直射
    }

    @Override
    public void forward() {
        // 英雄机由鼠标控制，不通过forward函数移动
    }

    public void setShootNum(int num) {
        this.shootNum = num;
    }


    @Override
    /**
     * 通过射击产生子弹
     * @return 射击出的子弹List
     */
    public List<BaseBullet> shoot() {
        int locx = getLocationX();
        int locy = getLocationY();
        int speedX = getSpeedX();
        int speedY = getSpeedY();

        //直射情形
        if (shootStrategy instanceof ShootStraight) {
            return shootStrategy.shoot(locx,locy,speedX,speedY,power,shootNum,direction);
        } else if (shootStrategy instanceof ShootFanShaped) {
            return shootStrategy.shoot(locx,locy,speedX,speedY,power,shootNum,direction);
        }else {
            int shootNum_New = 20;
            return shootStrategy.shoot(locx,locy,speedX,speedY,power,shootNum_New,direction);
        }

    }

    //如果英雄机触碰到加血道具，则可以加血
    public void increaseHp(int increase){
        hp += increase;
        if(hp > maxHp){
            hp = maxHp;
        }
    }

    /**英雄机的参数初始化,Game中先做初始化*/
    public static void initHeroAircraft(int locationX, int locationY, int speedX, int speedY, int hp){
        sin_locationX =  locationX;
        sin_locationY =  locationY;
        sin_speedX = speedX;
        sin_speedY = speedY;
        sin_hp = hp;
    }
    /**
     * 获取英雄机单例实例
     * @return 英雄机单例实例
     */
    public static HeroAircraft getInstance() {
        if (instance == null) {
            synchronized (HeroAircraft.class) {
                if (instance == null) {
                    instance = new HeroAircraft(sin_locationX, sin_locationY, sin_speedX, sin_speedY, sin_hp);
                }
            }
        }
        return instance;
    }
    /**
     * 关键方法：重置单例实例
     * 在每局新游戏开始前必须调用此方法，强制销毁旧对象
     */
    public static void resetInstance() {
        instance = null;
        Log.d("HeroAircraft", "Singleton instance destroyed. Ready for new game.");
    }
    // [新增] 重置状态方法
    public void reset(int x, int y, int speedX,int speedY,int hp) {
        sin_locationX = x;
        sin_locationY = y;
        sin_speedX = speedX;
        sin_speedY = speedY;
        sin_hp = hp;

        Log.d("HeroAircraft", "Hero reset to (" + x + ", " + y + ") with HP: " + hp);
    }


}
