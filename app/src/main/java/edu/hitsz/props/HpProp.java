package edu.hitsz.props;
//import edu.hitsz.musicthread.MusicThread;
import edu.hitsz.observers.BombObserver;

public class HpProp extends Prop {
    private int hpServe = 30;
    public HpProp(int locationX, int locationY,int speedX,int speedY) {
        super(locationX,locationY,speedX,speedY);
    }
    @Override
    /**@return:返回道具提供的生命值*/
    public int propValid() {
        System.out.println("HpProp生效！");
//        MusicManager.getInstance().playOnce("src/videos/get_supply.wav");
        return hpServe;
    }

    @Override
    public void addObserver(BombObserver bombObserver) {
        return;
    }

    @Override
    public void removeObserver(BombObserver bombObserver) {
        return;
    }

    @Override
    public void notifyAllObservers() {
        return;
    }

}
