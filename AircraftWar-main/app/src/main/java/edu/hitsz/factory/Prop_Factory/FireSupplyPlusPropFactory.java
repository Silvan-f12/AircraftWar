package edu.hitsz.factory.Prop_Factory;

import edu.hitsz.props.FireSupplyPlusProp;
import edu.hitsz.props.FireSupplyProp;
import edu.hitsz.props.Prop;

public class FireSupplyPlusPropFactory implements PropFactory {
    @Override
    public Prop createProp(int locationX, int locationY, int speedX, int speedY){
        return new FireSupplyPlusProp(locationX,locationY,speedX,speedY);
    }
}
