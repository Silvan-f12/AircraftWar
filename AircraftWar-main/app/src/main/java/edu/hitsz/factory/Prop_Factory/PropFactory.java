package edu.hitsz.factory.Prop_Factory;
import edu.hitsz.props.Prop;
public interface PropFactory {
    Prop createProp(int locationX, int locationY, int speedX, int speedY);

}
