import com.hdu.manager.IDPairGenerator;
import org.junit.Test;

import java.util.List;

public class IDPairGeneratorTests {


    @Test
    public void testGenerate(){
        IDPairGenerator generator = new IDPairGenerator(10);
        List<String> pairs = generator.generate(4);
        while (pairs.size() != 0){
            System.out.println(pairs);
            pairs = generator.generate(4);
        }
    }
}
