package edu.hitsz.factory.Enemy_Factory;

import edu.hitsz.aircraft.AbstractEnemyAircraft;
import edu.hitsz.aircraft.MobEnemy;
public class MobEnemyFactory implements EnemyFactory {
    @Override
    public AbstractEnemyAircraft createEnemyAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        return new MobEnemy(locationX, locationY, speedX, speedY, hp);
    }
}
