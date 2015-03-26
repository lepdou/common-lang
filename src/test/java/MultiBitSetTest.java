import org.lepdou.common.MultiBitSet;
import org.testng.annotations.Test;


/**
 * Created by lepdou on 15/3/26.
 */
public class MultiBitSetTest {

    @Test
    public void test(){
        MultiBitSet bitSet = new MultiBitSet(4);
        bitSet.set(2,15);
        bitSet.set(3,5);
        bitSet.set(3,6);
        bitSet.set(7,6);
        bitSet.set(8,6);
        bitSet.set(1000, 5);
        bitSet.clear(1000);
        System.out.print(bitSet.get(1000));
    }


}
