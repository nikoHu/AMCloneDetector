package com.hdu.manager;

import com.hdu.bean.Measure;
import com.hdu.bean.Pair;
import com.hdu.conf.Config;
import com.hdu.simhash.SimHash;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@SuppressWarnings({"Duplicates"})
public class SimhashComparator implements Comparator{

    private ReentrantLock lock = new ReentrantLock();

    @Override
    public List<Pair> findPairs(List<Measure> measureList) {

        List<Pair> pairs = new ArrayList<>();
        if (measureList.size() < 2){
            return pairs;
        }

        int buffer = 10000;
        IDPairGenerator generator = new MeasureIDPairGenerator(measureList, Config.LineGapDis, Config.LineGapDisMax, Config.LineGapDisMin);
        List<String> ids = generator.generate(buffer);
        int cnt = 0;

        while (ids.size() != 0){
            ids.parallelStream().forEach(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    String[] tmp = s.split(",");
                    int id1 = Integer.parseInt(tmp[0]);
                    int id2 = Integer.parseInt(tmp[1]);
                    Measure measure1 = measureList.get(id1);
                    Measure measure2 = measureList.get(id2);
                    float lineGapDis = Math.abs(measure1.getLineCount() - measure2.getLineCount()) *1f/ Math.min(measure1.getLineCount(), measure2.getLineCount());
                    if (lineGapDis < Config.LineGapDisMin || lineGapDis >= Config.LineGapDisMax){
                        return;
                    }

                    int sameCounter = 0;
                    StringBuilder sequence = new StringBuilder();
                    for (long hash2: measure2.getHash()){
                        boolean same = false;
                        for (long hash1: measure1.getHash()){
                            if (SimHash.hammingDistance(hash1, hash2) <= 1){
                                sameCounter++;
                                same = true;
                            }
                        }
                        if(same){
                            sequence.append("1");
                        }else{
                            sequence.append("0");
                        }
                    }

                    float similarity = sameCounter *1f / measure1.getCode().size();
                    if(similarity < Config.Similarity){
                        return;
                    }

                    Matcher matcher = Pattern.compile("[1]+").matcher(sequence);
                    int oneCounter = 0;
                    while (matcher.find()){
                        oneCounter++;
                    }
                    int type = (oneCounter < 3)? 1: 2;

                    lock.lock();
                    pairs.add(new Pair(measure1.getId(), measure2.getId(), type));
                    lock.unlock();
                }
            });
            cnt += ids.size();
            log.info("processing {}", cnt);
            ids = generator.generate(buffer);
        }
        return pairs;
    }
}
