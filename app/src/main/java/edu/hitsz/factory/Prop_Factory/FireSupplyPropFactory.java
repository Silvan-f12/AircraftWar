package edu.hitsz.factory.Prop_Factory;

import edu.hitsz.props.FireSupplyProp;
import edu.hitsz.props.Prop;

public class FireSupplyPropFactory implements PropFactory {

    @Override
    public Prop createProp(int locationX, int locationY, int speedX, int speedY){
        return new FireSupplyProp(locationX,locationY,speedX,speedY);
    }
}

