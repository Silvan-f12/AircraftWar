package edu.hitsz.shoot_strategy;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;

import java.util.LinkedList;
import java.util.List;

public class ShootStraight implements ShootStrategy {
    //设置默认值

    @Override
    public List<BaseBullet> shoot(int locationX,int locationY,int speedX,int speedY,int power,int shootNum,int direction) {
        List<BaseBullet> res = new LinkedList<>();
        int x = locationX;
        int y = locationY + direction*2;
        int speedX1 = 0;
        int speedY1 = speedY + direction*15;
        BaseBullet bullet;
        //分情况讨论，敌机和英雄机
        for(int i=0; i<shootNum; i++){
            // 子弹发射位置相对飞机位置向前偏移

            if(direction > 0) {
                bullet = new EnemyBullet(x + (i * 2 - shootNum + 1) * 10, y, speedX1, speedY1, power);
            } else {
                bullet = new HeroBullet(x + (i * 2 - shootNum + 1) * 10, y, speedX1, speedY1, power);
            }
            res.add(bullet);
        }
        return res;
    }
}
