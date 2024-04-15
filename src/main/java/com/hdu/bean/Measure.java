package com.hdu.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class Measure {

    private long startToken;
    private long endToken;
    private int startLine;
    private int endLine;
    private int fileId;
    private int id;
    private String path;
    private Integer lineCount;
    private List<String> code;
    private List<Long> hash;
    private List<Byte> token;

    public static List<Measure> measureList = new ArrayList<>();
    public static AtomicInteger measureID = new AtomicInteger(0);

    public Measure() {
    }

    public Measure(long startToken, long endToken, int startLine, int endLine, int fileId) {
        this.startToken = startToken;
        this.endToken = endToken;
        this.startLine = startLine;
        this.endLine = endLine;
        this.fileId = fileId;
    }

    public Measure(long startToken, int startLine) {
        this.startToken = startToken;
        this.startLine = startLine;
    }

    @Override
    public String toString() {
        return startLine + "," + endLine + "," + startToken + "," + endToken + "," + fileId;
    }

    /**
     * 为measureList按照LineCount升序排序
     * @param measureList
     */
    public static void sortMeasureList(List<Measure> measureList){
        measureList.sort(new java.util.Comparator<Measure>() {
            @Override
            public int compare(Measure o1, Measure o2) {
                if (o1.getLineCount() < o2.getLineCount()){
                    return -1;
                }
                if (o1.getLineCount().equals(o2.getLineCount())){
                    return 0;
                }
                return 1;
            }
        });
    }
}
