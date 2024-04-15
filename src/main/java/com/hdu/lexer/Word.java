package com.hdu.lexer;

import com.hdu.bean.Token;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by huage on 2017/3/28.
 */
public abstract class Word {
    public HashMap<Long, Long> equivHashMap = new HashMap<>();
    public List<Long> neglectList = new ArrayList<>();

    public Lexer lexer;

    public static AtomicLong tokenID = new AtomicLong(0);
    public static AtomicLong line = new AtomicLong(0L);

    protected int mlc = 50;
    protected int minLine = 2;
    protected int maxLine = 0;

    public Word() {
        tokenID = new AtomicLong(0);
        line = new AtomicLong(0);
    }

    public void setMlc(int mlc) {
        this.mlc = mlc;
    }


    public void setMinLine(int minLine) {
        this.minLine = minLine;
    }

    public void setMaxLine(int maxLine) {
        this.maxLine = maxLine;
    }


    public List<Token> segment(String inputFileName, int fileID) {
        return new ArrayList<>();
    }


    public List<Token> segment(String inputFileName) throws IOException {
        return segment(inputFileName, -1);
    }
}
