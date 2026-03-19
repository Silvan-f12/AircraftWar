package edu.hitsz.aircraft;
import edu.hitsz.MySurfaceView;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.shoot_strategy.ShootCircle;


import java.util.List;
import java.util.Random;

public class BossEnemyAircraft extends AbstractEnemyAircraft{
    private final int shootNum = 12;
    private final int power = 10;
    private final int direction = 0;//boss敌机悬浮在上空
    // 随机移动相关变量
    private Random random = new Random();
    private int moveDirection = 1; // 1表示向右，-1表示向左
    private int directionChangeCounter = 0;
    private final int DIRECTION_CHANGE_INTERVAL = 60; // 每60帧考虑改变方向

    public BossEnemyAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, 0, hp);
        setShootStrategy(new ShootCircle());
    }
    //子弹弹道需要重新写
    @Override
    public List<BaseBullet> shoot() {

        int locx = getLocationX();
        int locy = getLocationY();
        int speedX = getSpeedX();
        int speedY = getSpeedY();
        return shootStrategy.shoot(locx,locy,speedX,speedY,power,shootNum,direction);
    }

    @Override
    public void forward() {
        // 随机移动逻辑全部集成在此方法中
        directionChangeCounter++;

        // 每隔一定时间考虑改变方向
        if (directionChangeCounter >= DIRECTION_CHANGE_INTERVAL) {
            directionChangeCounter = 0;

            // 30%的几率改变方向
            if (random.nextDouble() < 0.3) {
                moveDirection = -moveDirection;
            }

            // 随机调整速度，使移动更加自然
            int baseSpeedValue = 2; // 基础速度
            int speedVariation = random.nextInt(3); // 速度变化范围0-2
            speedX = moveDirection * (baseSpeedValue + speedVariation);
        }

        // 小概率随机暂停（模拟思考）
        if (random.nextDouble() < 0.02) {
            speedX = 0;
        }

        // 执行移动
        super.forward();

        // 边界检查：如果超出左右边界，则反弹
        if (locationX <= 0) {
            locationX = 0;
            moveDirection = 1; // 碰到左边界就向右移动
            speedX = Math.abs(speedX); // 确保速度为正（向右）
        } else if (locationX >= MySurfaceView.WINDOW_WIDTH) {
            locationX = MySurfaceView.WINDOW_WIDTH;
            moveDirection = -1; // 碰到右边界就向左移动
            speedX = -Math.abs(speedX); // 确保速度为负（向左）
        }

        // 确保Boss保持悬浮状态（Y坐标不变）
        if (speedY != 0) {
            speedY = 0;
        }
    }
}
