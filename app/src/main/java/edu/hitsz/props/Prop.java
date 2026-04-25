package edu.hitsz.props;

import edu.hitsz.MySurfaceView;
import edu.hitsz.basic.AbstractFlyingObject;
//import edu.hitsz.musicthread.MusicThread;
import edu.hitsz.observers.BombObserver;

public abstract class Prop extends AbstractFlyingObject{

    public Prop(int locationX,int locationY,int speedX,int speedY){
        super(locationX,locationY,speedX,speedY);
    }

    @Override
    //道具没有横向边界反弹，只有向下飞行出界判断
    public void forward(){
        super.forward();
        
        // 判定 y 轴出界
        if (speedY > 0 && locationY >= MySurfaceView.WINDOW_HEIGHT ) {
            // 向下飞行出界
            vanish();
        }else if (locationY <= 0){
            // 向上飞行出界
            vanish();
        }
    }
    //定义抽象方法道具生效。包含音乐和道具生效线程
    public abstract int propValid();

    public abstract void addObserver(BombObserver bombObserver);
    public abstract void removeObserver(BombObserver bombObserver);
    public abstract void notifyAllObservers();

    //

}