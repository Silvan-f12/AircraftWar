package edu.hitsz.factory.Enemy_Factory;

import edu.hitsz.aircraft.AbstractEnemyAircraft;

public interface EnemyFactory {
    AbstractEnemyAircraft createEnemyAircraft(int locationX, int locationY, int speedX, int speedY, int hp);
}
