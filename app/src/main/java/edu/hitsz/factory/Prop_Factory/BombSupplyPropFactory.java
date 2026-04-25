package edu.hitsz.factory.Prop_Factory;
import edu.hitsz.props.BombSupplyProp;
import edu.hitsz.props.Prop;
public class BombSupplyPropFactory implements PropFactory {

    @Override
    public Prop createProp(int locationX, int locationY, int speedX, int speedY){
        return new BombSupplyProp(locationX,locationY,speedX,speedY);
    }
}
