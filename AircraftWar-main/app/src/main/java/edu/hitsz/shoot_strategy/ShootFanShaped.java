package edu.hitsz.shoot_strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;

import java.util.LinkedList;
import java.util.List;
public class ShootFanShaped implements ShootStrategy{
    public List<BaseBullet> shoot(int locationX,int locationY,int speedX,int speedY,int power,int shootNum,int direction) {
        List<BaseBullet> res = new LinkedList<>();
        int x = locationX;
        int y = locationY + direction * 2;
        int speedX1 = 0;
        int speedY1 = speedY + direction * 10;
        BaseBullet bullet;
        for (int i = 0; i < shootNum; i++) {
            // 子弹发射位置相对飞机位置向前偏移
            // 多个子弹横向分散
            if(direction > 0) {
                bullet = new EnemyBullet(x + (i * 2 - shootNum + 1) * 10, y, speedX1 + 4*i - shootNum + 1, speedY1, power);
            } else {
                bullet = new HeroBullet(x + (i * 2 - shootNum + 1) * 10, y, speedX1 + 4*i - shootNum + 1, speedY1, power);
            }
            res.add(bullet);
        }
        return res;
    }
}
