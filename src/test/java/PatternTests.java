import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternTests {


    @Test
    public void testPattern(){
        String pattern = "[1]+";
        String sequence = "11100010010010101011";
        Matcher matcher = Pattern.compile(pattern).matcher(sequence);
        while (matcher.find()){
            System.out.println(matcher.group());
        }
    }
}
