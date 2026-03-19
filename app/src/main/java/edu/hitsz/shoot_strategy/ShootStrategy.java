package edu.hitsz.shoot_strategy;
import edu.hitsz.bullet.*;

import java.util.List;

public interface ShootStrategy {
    public List<BaseBullet> shoot(int locationX,int locationY,int speedX,int speedY,int power,int shootNum,int direction);
}
