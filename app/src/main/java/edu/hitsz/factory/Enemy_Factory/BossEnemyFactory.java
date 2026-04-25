package edu.hitsz.factory.Enemy_Factory;

import edu.hitsz.aircraft.AbstractEnemyAircraft;

import edu.hitsz.aircraft.BossEnemyAircraft;

public class BossEnemyFactory implements EnemyFactory{

    @Override
    public AbstractEnemyAircraft createEnemyAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        return new BossEnemyAircraft(locationX, locationY, speedX, speedY, hp);
    }
}
