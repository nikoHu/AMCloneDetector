package com.hdu.lexer;


import com.hdu.bean.SimToken;
import com.hdu.bean.Token;

import java.io.*;
import java.util.List;


/**
 * Created by huage on 2017/3/28.
 */
@SuppressWarnings("Duplicates")
public abstract class Lexer {

    public int line = 1;
    public int indexOfChar = 0;
    public int wordIndexOfChar = 1;

    protected char peek = ' ';
    protected char lastPeek = ' ';
    protected char lastIdentifier = ' ';
    protected BufferedReader reader = null;
    protected Boolean isEnd = false;

    protected Boolean varibleOneOrSecond = true;
    protected String lastVariable = "";
    protected int lastline = 0;


    public Boolean getReaderIsEnd() {
        return this.isEnd;
    }


    public void saveToken(List<SimToken> list, String outputFileName) throws IOException {

        FileWriter writer = new FileWriter(outputFileName);
        writer.write("token info");

        writer.write("\n");

        if (list.size() > 0) {
            for (SimToken x : list) {
                writer.write(x.toString() + "\t\t" + "\n");
            }

        }
        writer.flush();
        writer.close();
    }

    public Lexer(String inputFileName) throws IOException {
        reader = new BufferedReader(new FileReader(inputFileName));
    }

    public void getChar() throws IOException {
        lastPeek = peek;
        peek = (char) reader.read();
        if ((int) peek == 0xffff) {
            this.isEnd = true;
        }
        indexOfChar++;
    }

    public boolean getChar(char x) throws IOException {
        getChar();
        if (peek != x) {
            return false;
        }
        peek = ' ';
        return true;
    }

    public Token scan() throws IOException {
        return null;
    }

    public void closeReader() {
        try {
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 哈希函数，将字符串映射到[-128,-3]u[125,127]字节空间
     * @param str
     * @return
     */
    protected byte str2hash(String str) {
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
