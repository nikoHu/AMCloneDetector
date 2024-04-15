import java.util.ArrayList;
import java.util.List;

public class RemoveListElementTest {

    public static void main(String[] args) {
//        List<String> list1 = new ArrayList<>();
//        List<String> list2 = new ArrayList<>();
//
//        list1.add("aa");
//        list1.add("bb");
//
//        list2.add("bb");
//        list2.add("cc");
//
//        List<String> list3 = list2;
//        List<String> list4 = new ArrayList<String>(list2);
//
//        for(int i = 0; i < list1.size(); i ++){
//            for(int j = 0; j < list2.size(); j ++){
//                System.out.println(i + " -- " + j + " -- " + list1.get(i) + " -- " + list2.get(j));
//                if(list1.get(i).equals((list2.get(j)))){
//                    list1.remove(i);
//                    list2.remove(j);
//                    i --;
//                    continue;
//                }
//            }
//        }
//
//        list3.set(0, "adf");
//        System.out.println(list2);
//        System.out.println(list3);
//        System.out.println(list4);
        System.out.println(str2hash("country"));

        System.out.println(str2hash("customer"));
    }

    protected static byte str2hash(String str) {
        str = str.toLowerCase();
        if (str.length() < 2) {
            int h = str.toCharArray()[str.length() - 1];
            h <<= 1;
            return (byte) (-3 - (h & 0x7f));
        } else {
            int h1 = str.toCharArray()[str.length() - 1];
            int h2 = str.toCharArray()[str.length() - 2];
            h1 <<= 1;
            int h = h1 ^ h2;
            return (byte) (-3 - (h & 0x7f));
        }
    }
}
