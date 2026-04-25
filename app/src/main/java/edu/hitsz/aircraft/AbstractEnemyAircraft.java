package edu.hitsz.aircraft;

/**
 * 所有敌机的抽象父类（BOSS,ELITE,Mob）
 * @author ZWR
 */
public abstract class AbstractEnemyAircraft extends AbstractAircraft {
    /**
     * 生命值：
     * */
    public AbstractEnemyAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
    }

    @Override
    public void forward() {
        super.forward();
    }
}
