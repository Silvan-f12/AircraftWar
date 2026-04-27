package edu.hitsz.aircraft;
import edu.hitsz.MySurfaceView;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.shoot_strategy.ShootStraight;


import java.util.List;

public class EliteEnemyAircraft extends AbstractEnemyAircraft{
    private final int shootNum = 1;
    private final int power = 10;
    private final int direction = 1;//direction=1代表向下发射

    /**
     * @param locationX 精英机位置x坐标
     * @param locationY 精英机位置y坐标
     * @param speedX 精英机射出的子弹的基准速度
     * @param speedY 精英机射出的子弹的基准速度（横向无速度）
     * @param hp    初始生命值
     */
    public EliteEnemyAircraft(int locationX, int locationY, int speedX, int speedY, int hp){
        super(locationX, locationY, speedX, speedY, hp);
        setShootStrategy(new ShootStraight());
    }


    @Override
    /**
     * 敌机射击函数
     * @return:返回子弹列表。*/
    public List<BaseBullet> shoot(){
        int locx = getLocationX();
        int locy = getLocationY();
        int speedX = getSpeedX();
        int speedY = getSpeedY();
        return shootStrategy.shoot(locx,locy,speedX,speedY,power,shootNum,direction);
    }

    @Override
    public void forward() {//精英敌机可以横向移动
        //setSpeedX();
        super.forward(); // 执行移动逻辑（更新位置）

        // 边界检查：如果超出左右边界，则反弹
        if (locationX <= 0) {
            locationX = 0; // 防止卡在边界外
            speedX = Math.abs(speedX); // 向右移动
        } else if (locationX >= MySurfaceView.WINDOW_WIDTH) {
            locationX = MySurfaceView.WINDOW_WIDTH; // 防止卡在边界外
            speedX = -Math.abs(speedX); // 向左移动
        }

        // 边界检查：垂直方向（出界则消失）
        if (locationY >= MySurfaceView.WINDOW_HEIGHT) {
            vanish();
        }

    }

}
