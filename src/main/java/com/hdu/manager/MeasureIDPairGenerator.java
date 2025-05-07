package com.hdu.manager;

import com.hdu.bean.Measure;
import java.util.ArrayList;
import java.util.List;

public class MeasureIDPairGenerator extends IDPairGenerator{

    private List<Measure> measureList;
    private float lineGapDis;

    public MeasureIDPairGenerator(int size) {
        super(size);
    }

    public MeasureIDPairGenerator(List<Measure> measureList, float lineGapDis) {
        super(measureList.size());
        this.measureList = measureList;
        this.lineGapDis = lineGapDis;
    }

    @Override
    public List<String> generate(int length) {
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
                Measure m1 = measureList.get(m);
                Measure m2 = measureList.get(n);
                float lineGapDis = Math.abs(m1.getLineCount() - m2.getLineCount()) *1f/ Math.min(m1.getLineCount(), m2.getLineCount());
                if (lineGapDis > this.lineGapDis){
                    continue;
                }
                pairs.add(m + "," + n);
            }
        }
        return pairs;
    }
}
