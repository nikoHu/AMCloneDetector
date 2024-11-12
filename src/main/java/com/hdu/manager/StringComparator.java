package com.hdu.manager;

import com.hdu.bean.Measure;
import com.hdu.bean.Pair;
import com.hdu.conf.Config;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("Duplicates")
@Slf4j
public class StringComparator  implements Comparator {

    private ReentrantLock lock = new ReentrantLock();
    // 用于向最终结果添加子线程检测结果的锁
    private ReentrantLock resultLock = new ReentrantLock();
    // 用于生成ID对的锁
    private static final Object generateIDsLock = new Object();
    // 已处理id对的数量
    private static AtomicLong dealCount = new AtomicLong(0);

    private static final List<Pair> allPairs = Collections.synchronizedList(new ArrayList<>());


    @Override
    public List<Pair> findPairs(final List<Measure> measureList) {
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
                    List<String> codes2 = new ArrayList<String>(measure2.getCode());
                    List<String> codes1 = new ArrayList<String>(measure1.getCode());
                    for (int i = 0; i < codes2.size(); i++){
                        boolean same = false;
                        for (int j = 0; j < codes1.size(); j++){
                            if (codes1.get(j).equals(codes2.get(i))){
                                sameCounter++;
                                same = true;
                                codes2.remove(i);
                                codes1.remove(j);
                                i --;
                                break;
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

    @Override
    public List<Pair> findPairsByAsync(final List<Measure> measureList) {
        List<Pair> pairs = new ArrayList<>();
        if (measureList.size() < 2){
            return pairs;
        }
        int buffer = 2;
        IDPairGenerator generator = new MeasureIDPairGenerator(measureList, Config.LineGapDis, Config.LineGapDisMax, Config.LineGapDisMin);

        ExecutorService executor = Executors.newFixedThreadPool(Config.ThreadNum); // 创建两个线程的线程池
        for (int i = 0; i < Config.ThreadNum; i++){
            executor.submit(new GenerateIDs(generator, buffer, measureList));
        }

        executor.shutdown(); // 关闭线程池

        // 等待所有线程完成
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        // 输出整合后的所有 pairs
        return allPairs;
    }

    private class GenerateIDs implements Runnable {
        private IDPairGenerator generator;
        private int buffer;
        private List<Measure> measureList;
        public GenerateIDs(IDPairGenerator generator, int buffer, List<Measure> measureList){
            this.generator = generator;
            this.buffer = buffer;
            this.measureList = measureList;
        }

        @Override
        public void run() {
            List<String> ids = null;
            List<Pair> pairs = new ArrayList<>();
            // 全部id对的数量
            long allCount = (long) measureList.size() * (measureList.size() - 1) / 2;
            int cnt = 0;
            while (dealCount.get() < allCount) {
                synchronized (generateIDsLock) {
                    ids = generator.generate(buffer);
                    dealCount.addAndGet(ids.size());
                }

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
                        List<String> codes2 = new ArrayList<String>(measure2.getCode());
                        List<String> codes1 = new ArrayList<String>(measure1.getCode());
                        for (int i = 0; i < codes2.size(); i++){
                            boolean same = false;
                            for (int j = 0; j < codes1.size(); j++){
                                if (codes1.get(j).equals(codes2.get(i))){
                                    sameCounter++;
                                    same = true;
                                    codes2.remove(i);
                                    codes1.remove(j);
                                    i --;
                                    break;
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
                log.info(Thread.currentThread().getName()+ " processing {}", cnt);
            }
            resultLock.lock();
            try {
                allPairs.addAll(pairs);
            } finally {
                resultLock.unlock();
            }
        }
    }
}
