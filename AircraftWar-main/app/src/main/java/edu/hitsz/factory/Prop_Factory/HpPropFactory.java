package edu.hitsz.factory.Prop_Factory;

import edu.hitsz.props.HpProp;
import edu.hitsz.props.Prop;

public class HpPropFactory implements PropFactory {

    @Override
    public Prop createProp(int locationX, int locationY, int speedX, int speedY){
        return new HpProp(locationX,locationY,speedX,speedY);
    }

}
