package edu.hitsz.factory.Enemy_Factory;

import edu.hitsz.aircraft.AbstractEnemyAircraft;

import edu.hitsz.aircraft.EliteEnemyAircraft;

public class EliteEnemyFactory implements EnemyFactory{

    @Override
    public AbstractEnemyAircraft createEnemyAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        return new EliteEnemyAircraft(locationX, locationY, speedX, speedY, hp);
    }
}
