package com.hdu.manager;

import com.hdu.bean.Measure;
import com.hdu.bean.Pair;
import com.hdu.conf.Config;
import com.hdu.util.FileUtil;
import com.hdu.util.CpuAffinity;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    CountDownLatch countDownLatch = new CountDownLatch(Config.ThreadNum);


    @Override
    public List<Pair> findPairs(final List<Measure> measureList) {
        List<Pair> pairs = new ArrayList<>();
        if (measureList.size() < 2){
            return pairs;
        }

        int buffer = Config.Buffer;
        IDPairGenerator generator = new MeasureIDPairGenerator(measureList, Config.LineGapDis);
        List<String> ids = generator.generate(buffer);
        long cnt = 0;
        while (ids.size() != 0){
            List<Pair> bufferPairs = new ArrayList<>();
            ids.parallelStream().forEach(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    String[] tmp = s.split(",");
                    int id1 = Integer.parseInt(tmp[0]);
                    int id2 = Integer.parseInt(tmp[1]);
                    Measure measure1 = measureList.get(id1);
                    Measure measure2 = measureList.get(id2);
                    float lineGapDis = Math.abs(measure1.getLineCount() - measure2.getLineCount()) *1f/ Math.min(measure1.getLineCount(), measure2.getLineCount());
                    if (lineGapDis > Config.LineGapDis){
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
                    bufferPairs.add(new Pair(measure1.getId(), measure2.getId(), type));
                    lock.unlock();
                }
            });
            lock.lock();
            try {
                FileUtil.outputBuffer(bufferPairs);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
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
        int buffer = Config.Buffer;
        IDPairGenerator generator = new MeasureIDPairGenerator(measureList, Config.LineGapDis);

        ExecutorService executor = Executors.newFixedThreadPool(Config.ThreadNum); // 创建ThreadNum个线程的线程池
        for (int i = 0; i < Config.ThreadNum; i++){
            executor.submit(new GenerateIDs(generator, buffer, measureList,i));
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 关闭线程池
        executor.shutdown();

        try{
            FileUtil.outputBuffer(allPairs);
        }catch (IOException e){
            e.printStackTrace();
        }
        // 输出整合后的所有 pairs
        return allPairs;
    }

    private class GenerateIDs implements Runnable {
        private IDPairGenerator generator;
        private int buffer;
        private List<Measure> measureList;
        private int cpuid;
        public GenerateIDs(IDPairGenerator generator, int buffer, List<Measure> measureList,int cpuid) {
            this.generator = generator;
            this.buffer = buffer;
            this.measureList = measureList;
            this.cpuid = cpuid;
        }

        public List<String> generateIDs(){
            List<String> ids = new ArrayList<>();
            synchronized (generateIDsLock) {
                ids = generator.generate(buffer);
            }
            return ids;
        }

        @Override
        public void run() {
            CpuAffinity.bindToCpu(cpuid);
            List<String> ids;
            List<Pair> pairs = new ArrayList<>();
            // 全部id对的数量
            long cnt = 0;
            while ((ids=generateIDs()).size() != 0){
                List<Pair> bufferPairs = Collections.synchronizedList(new ArrayList<>());
                ids.parallelStream().forEach(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        String[] tmp = s.split(",");
                        int id1 = Integer.parseInt(tmp[0]);
                        int id2 = Integer.parseInt(tmp[1]);
                        Measure measure1 = measureList.get(id1);
                        Measure measure2 = measureList.get(id2);
                        float lineGapDis = Math.abs(measure1.getLineCount() - measure2.getLineCount()) *1f/ Math.min(measure1.getLineCount(), measure2.getLineCount());
                        if (lineGapDis > Config.LineGapDis){
                            return;
                        }
                        Map<String, Integer> freqMap = new HashMap<>();
                        int sameCounter = 0;
                        int minRequired = (int) Math.ceil(Config.Similarity * measure1.getCode().size());
                        int[] matchSequence = new int[measure2.getCode().size()];
                        List<String> code2 = measure2.getCode();
                        for (int i = 0; i < code2.size(); i++) {
                            String line = code2.get(i);
                            Integer count = freqMap.get(line);
                            if (count != null && count > 0) {
                                sameCounter++;
                                matchSequence[i] = 1;
                                freqMap.put(line, count - 1);
                            }
                            // 提前终止检查
                            if (sameCounter + (code2.size() - i - 1) < minRequired) break;
                        }
                        float similarity = sameCounter *1f / measure1.getCode().size();
                        if(similarity < Config.Similarity){
                            return;
                        }

                        int oneCounter = 0;
                        boolean inBlock = false;
                        for (int match : matchSequence) {
                            if (match == 1) {
                                if (!inBlock) {
                                    oneCounter++;
                                    inBlock = true;
                                }
                            } else {
                                inBlock = false;
                            }
                        }
                        int type = (oneCounter < 3)? 1: 2;

                        bufferPairs.add(new Pair(measure1.getId(), measure2.getId(), type));

                    }
                });
                pairs.addAll(bufferPairs);
//                resultLock.lock();
//                try {
//                    FileUtil.outputBuffer(bufferPairs);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } finally {
//                    resultLock.unlock();
//                }
                cnt += ids.size();
//                if (cnt % (buffer * 100) == 0) {
//                    log.info("{} processing {}", Thread.currentThread().getName(), cnt);
//                }

            }
            resultLock.lock();
            try {
                allPairs.addAll(pairs);
            }finally {
                resultLock.unlock();
            }
            countDownLatch.countDown();
        }
    }
}
