package edu.hitsz.shoot_strategy;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;

import java.util.LinkedList;
import java.util.List;
public class ShootCircle implements ShootStrategy{
    //不需要的参数:speedX,speedY,direction
    public List<BaseBullet> shoot(int locationX,int locationY,int speedX,int speedY,int power,int shootNum,int direction){
        int baseSpeed = 40;
        List<BaseBullet> res = new LinkedList<>();
        int centerX = locationX;
        int centerY = locationY + 4*direction; // 从飞行物下方发射
        BaseBullet bullet;
        for(int i = 0; i < shootNum; i++) {
            // 计算每个子弹的角度（弧度）
            double angle = 2 * Math.PI * i / shootNum;

            // 根据角度计算x和y方向的速度分量
            int speedX_new = (int) (baseSpeed * Math.cos(angle));
            int speedY_new = (int) (baseSpeed * Math.sin(angle));

            // 子弹位置稍微偏移
            int bulletLocationX = centerX + (int)(20 * Math.cos(angle));
            int bulletLocationY = centerY + (int)(20 * Math.sin(angle));

            if(direction >= 0) {
                bullet = new EnemyBullet(bulletLocationX, bulletLocationY, speedX_new, speedY_new, power);
            } else {
                bullet = new HeroBullet(bulletLocationX, bulletLocationY, speedX_new, speedY_new, power);
            }
            res.add(bullet);

        }
        return res;
    }
}
