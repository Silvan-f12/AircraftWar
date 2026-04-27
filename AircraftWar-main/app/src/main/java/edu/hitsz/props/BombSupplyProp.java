package edu.hitsz.props;
import edu.hitsz.observers.BombObserver;


import java.util.ArrayList;
import java.util.List;

public class BombSupplyProp extends Prop {
    private List<BombObserver> bombObservers = new ArrayList<>();
    private int scoreBonus;

    public BombSupplyProp(int locationX, int locationY,int speedX,int speedY) {
        super(locationX,locationY,speedX,speedY);
    }
    @Override
    public int propValid() {
        System.out.println("BombSupply active!");
//        MusicManager.getInstance().playOnce("src/videos/bomb_explosion.wav");

        notifyAllObservers();
        return 1;
    }
    //增加观察者
    public void addObserver(BombObserver bombObserver) {
        this.bombObservers.add(bombObserver);
    }
    //删除观察者
    public void removeObserver(BombObserver bombObserver) {
        this.bombObservers.remove(bombObserver);
    }
    //通知所有观察者
    public void notifyAllObservers() {
        System.out.println("notify");
        for(BombObserver observer:bombObservers) {
            observer.update(this);//传入自身，让观察者查看道具信息
        }
    }

//    // 方法：计算总得分（汇总被清除敌机的分数）
//    public int calculate(List<AbstractAircraft> enemyAircrafts) {
//        scoreBonus = 0;
//        for (AbstractAircraft enemy : enemyAircrafts) {
//            // 仅普通/精英敌机计入得分（超级精英和Boss不计）
//            if (enemy instanceof MobEnemy || enemy instanceof EliteEnemyAircraft) {
//                scoreBonus += enemy.getScore();
//            }
//        }
//        return scoreBonus;
//    }
    //获取道具生效信息
//    public boolean getValid() {
//        return valid;
//    }


}
