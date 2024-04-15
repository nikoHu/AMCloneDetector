import com.hdu.util.CommentUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CommentUtilTests {

    @Test
    public void test(){
        List<String> code = new ArrayList<>();
        code.add("int main(){");
        code.add("//single");
        code.add("int a = 0;");
        code.add("/*");
        code.add("multi");
        code.add("*/");
        code.add("int b = 0;");
        code.add("}");
        List<String> lines = CommentUtil.removeComments2(code);
        for (String line: lines){
            System.out.println(line);
        }
    }
}
