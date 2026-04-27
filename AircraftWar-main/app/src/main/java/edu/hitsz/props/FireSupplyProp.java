package edu.hitsz.props;
import edu.hitsz.aircraft.HeroAircraft;
//import edu.hitsz.musicthread.MusicThread;
import edu.hitsz.observers.BombObserver;
import edu.hitsz.shoot_strategy.ShootFanShaped;
import edu.hitsz.shoot_strategy.ShootStraight;
//import org.apache.commons.lang3.concurrent.BasicThreadFactory;

public class FireSupplyProp extends Prop {
    public FireSupplyProp(int locationX, int locationY,int speedX,int speedY) {
        super(locationX,locationY,speedX,speedY);
    }
    @Override
    public int propValid() {
        System.out.println("FireSupply active!");
        //System.out.println("普通火力道具音效将要播放！");
//        MusicManager.getInstance().playOnce("src/videos/get_supply.wav");
        //ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
                //new BasicThreadFactory.Builder().namingPattern("prop-action-%d").daemon(true).build());
        // 创建线程控制生效时长（此处以3秒为例，可根据需求调整）
        Runnable r = () -> {
            // 道具生效持续时间（3000毫秒 = 3秒）
            //System.out.println("普通火力道具音效已经播放！");
            HeroAircraft.getInstance().setShootNum(3);
            HeroAircraft.getInstance().setShootStrategy(new ShootFanShaped());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                // 处理线程中断异常
                Thread.currentThread().interrupt();
                System.out.println("FireSupply effect interrupted!");
            }
            HeroAircraft.getInstance().setShootNum(1);
            HeroAircraft.getInstance().setShootStrategy(new ShootStraight());
        };
        //executorService.scheduleWithFixedDelay(r, 40, 40, TimeUnit.MILLISECONDS);
        new Thread(r).start();
        return 1;
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
