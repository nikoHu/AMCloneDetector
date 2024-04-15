package com.hdu.manager;

import java.util.ArrayList;
import java.util.List;

public class IDPairGenerator {

    protected int size;
    protected int m;
    protected int n;

    public IDPairGenerator(int size){
        this.size = size;
        m = 0;
        n = 1;
    }

    public List<String> generate(int length){
        List<String> pairs = new ArrayList<>();
        if (m == size - 1 && n == size){
            return pairs;
        }
        for (; m < size - 1; m++){
            if (n == size){
                n = m + 1;
            }
            for(; n < size; n++){
                if (pairs.size() >= length){
                    return pairs;
                }
                pairs.add(m + "," + n);
            }
        }
        return pairs;
    }

    public void reset(){
        m = 0;
        n = 1;
    }
}
