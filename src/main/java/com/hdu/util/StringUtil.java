package com.hdu.util;


public class StringUtil {


    public static byte hashCodeLine2Byte(String str){
        String[] words = str.split(" ");
        byte res = 0;
        for(String word: words){
            byte h = word2hash(word);
            res |= h;
        }
        return res;
    }

    @SuppressWarnings("Duplicates")
    private static byte word2hash(String str) {
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
