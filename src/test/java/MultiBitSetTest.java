import org.lepdou.common.MultiBitSet;
import org.testng.annotations.Test;


/**
 * Created by lepdou on 15/3/26.
 */
public class MultiBitSetTest {

    @Test
    public void test(){
        MultiBitSet multiBitSet = new MultiBitSet(4);
        multiBitSet.set(2,15);
        multiBitSet.set(100,4);
        int v2 = multiBitSet.get(2);
        int v100 = multiBitSet.get(100);



        System.out.print("");
    }




}
