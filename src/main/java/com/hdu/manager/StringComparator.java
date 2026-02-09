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
public class StringComparator implements Comparator {

    private ReentrantLock lock = new ReentrantLock();
    // 用于向最终结果添加子线程检测结果的锁
    private ReentrantLock resultLock = new ReentrantLock();
    // 用于生成ID对的锁
    private static final Object generateIDsLock = new Object();
    // 已处理id对的数量
    private static final AtomicLong dealCount = new AtomicLong(0);

    // ====== 进度统计（每 10% 输出一次） ======
    // 总 id 对数量（用于百分比进度；默认 1 防止除零）
    private static final AtomicLong totalPairCount = new AtomicLong(1);
    // 下一个要输出的百分比（10,20,...,100）
    private static final AtomicLong nextReportPercent = new AtomicLong(1);

    private static void reportProgress(long processed) {
        long total = totalPairCount.get();
        if (total <= 0) return;

        while (true) {
            long p = nextReportPercent.get(); // 10,20,...,100
            if (p > 100) return;

            long threshold = (total * p) / 100;
            if (processed >= threshold) {
                // CAS 确保多线程下每个百分比只输出一次
                if (nextReportPercent.compareAndSet(p, p + 1)) {
                    log.info("progress {}% (processed {}/{})", p, processed, total);
                } else {
                    // 其他线程已输出该档，继续检查下一档
                }
            } else {
                return;
            }
        }
    }

    private static void reportFinalProgress() {
        long processed = dealCount.get();
        long total = totalPairCount.get();
        double percent = (total <= 0) ? 100.0 : (processed * 100.0 / total);
        log.info(String.format("FINAL progress %.2f%% (processed %d/%d)", percent, processed, total));
    }
    // =========================================

    private static final List<Pair> allPairs = Collections.synchronizedList(new ArrayList<>());
    CountDownLatch countDownLatch = new CountDownLatch(Config.ThreadNum);

    @Override
    public List<Pair> findPairs(final List<Measure> measureList) {
        List<Pair> pairs = new ArrayList<>();
        if (measureList.size() < 2) {
            return pairs;
        }

        int buffer = Config.Buffer;
        IDPairGenerator generator = new InvertedIndexMeasureIDPairGenerator(measureList, Config.LineGapDis);
        List<String> ids = generator.generate(buffer);

        // ====== 初始化总量与进度计数器 ======
        long n = measureList.size();
        totalPairCount.set(n * (n - 1) / 2);
        dealCount.set(0);
        nextReportPercent.set(1);
        // ======================================

        long cnt = 0;
        while (ids.size() != 0) {
            List<Pair> bufferPairs = new ArrayList<>();
            ids.parallelStream().forEach(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    // ====== 进度更新 ======
                    long processed = dealCount.incrementAndGet();
                    reportProgress(processed);
                    // ======================

                    String[] tmp = s.split(",");
                    int id1 = Integer.parseInt(tmp[0]);
                    int id2 = Integer.parseInt(tmp[1]);
                    Measure measure1 = measureList.get(id1);
                    Measure measure2 = measureList.get(id2);

                    // ====== FIX：除零保护 ======
                    int minLine = Math.min(measure1.getLineCount(), measure2.getLineCount());
                    if (minLine <= 0) {
                        return;
                    }
                    float lineGapDis = Math.abs(measure1.getLineCount() - measure2.getLineCount()) * 1f / minLine;
                    // ============================

                    if (lineGapDis > Config.LineGapDis) {
                        return;
                    }

                    int sameCounter = 0;
                    StringBuilder sequence = new StringBuilder();
                    List<String> codes2 = new ArrayList<String>(measure2.getCode());
                    List<String> codes1 = new ArrayList<String>(measure1.getCode());
                    for (int i = 0; i < codes2.size(); i++) {
                        boolean same = false;
                        for (int j = 0; j < codes1.size(); j++) {
                            if (codes1.get(j).equals(codes2.get(i))) {
                                sameCounter++;
                                same = true;
                                codes2.remove(i);
                                codes1.remove(j);
                                i--;
                                break;
                            }
                        }
                        if (same) {
                            sequence.append("1");
                        } else {
                            sequence.append("0");
                        }
                    }
                    int compareLine;
                    switch (Config.Denominator){
                        case 1:
                            compareLine = measure1.getCode().size();
                            break;
                        case 2:
                            compareLine = (measure1.getCode().size()+measure2.getCode().size())/2;
                            break;
                        case 3:
                            compareLine =measure2.getCode().size();
                            break;
                        default:
                            compareLine = measure1.getCode().size();
                    }
                    float similarity = sameCounter * 1f / compareLine;
                    if (similarity < Config.Similarity) {
                        return;
                    }

                    Matcher matcher = Pattern.compile("[1]+").matcher(sequence);
                    int oneCounter = 0;
                    while (matcher.find()) {
                        oneCounter++;
                    }
                    int type = (oneCounter < 3) ? 1 : 2;

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

            // ====== FIX：同步版要把结果累积进返回 pairs，否则永远返回空 ======
            pairs.addAll(bufferPairs);
            // ===================================================================

            cnt += ids.size();
            log.info("processing {}", cnt);
            ids = generator.generate(buffer);
        }

        // ====== 新增：同步版本最终输出（一定有） ======
        reportFinalProgress();
        // ===========================================

        return pairs;
    }

    @Override
    public List<Pair> findPairsByAsync(final List<Measure> measureList) {
        List<Pair> pairs = new ArrayList<>();
        if (measureList.size() < 2) {
            return pairs;
        }

        int buffer = Config.Buffer;
        IDPairGenerator generator = new InvertedIndexMeasureIDPairGenerator(measureList, Config.LineGapDis);

        // ====== 初始化总量与进度计数器 ======
        long n = measureList.size();
        totalPairCount.set(n * (n - 1) / 2);
        dealCount.set(0);
        nextReportPercent.set(1);
        // ======================================

        ExecutorService executor = Executors.newFixedThreadPool(Config.ThreadNum); // 创建ThreadNum个线程的线程池
        for (int i = 0; i < Config.ThreadNum; i++) {
            executor.submit(new GenerateIDs(generator, buffer, measureList, i));
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // ====== 新增：异步版本最终输出（一定有） ======
        reportFinalProgress();
        // ===========================================

        // 关闭线程池
        executor.shutdown();

//        try {
//            FileUtil.outputBuffer(allPairs);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        // 输出整合后的所有 pairs
        return pairs;
    }

    private class GenerateIDs implements Runnable {
        private IDPairGenerator generator;
        private int buffer;
        private List<Measure> measureList;
        private int cpuid;

        public GenerateIDs(IDPairGenerator generator, int buffer, List<Measure> measureList, int cpuid) {
            this.generator = generator;
            this.buffer = buffer;
            this.measureList = measureList;
            this.cpuid = cpuid;
        }

        public List<String> generateIDs() {
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
            long cnt = 0;

            while ((ids = generateIDs()).size() != 0) {
                List<Pair> bufferPairs = Collections.synchronizedList(new ArrayList<>());
                ids.parallelStream().forEach(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        // ====== 进度更新 ======
                        long processed = dealCount.incrementAndGet();
                        reportProgress(processed);
                        // ======================

                        String[] tmp = s.split(",");
                        int id1 = Integer.parseInt(tmp[0]);
                        int id2 = Integer.parseInt(tmp[1]);
                        Measure measure1 = measureList.get(id1);
                        Measure measure2 = measureList.get(id2);

                        // ====== FIX：除零保护 ======
                        int minLine = Math.min(measure1.getLineCount(), measure2.getLineCount());
                        if (minLine <= 0) {
                            return;
                        }
                        float lineGapDis = Math.abs(measure1.getLineCount() - measure2.getLineCount()) * 1f / minLine;
                        // ============================

                        if (lineGapDis > Config.LineGapDis) {
                            return;
                        }

                        // ====== FIX：freqMap 必须先用 measure1.getCode() 初始化（最小改动） ======
                        Map<String, Integer> freqMap = new HashMap<>();
                        for (String line : measure1.getCode()) {
                            Integer c = freqMap.get(line);
                            freqMap.put(line, (c == null) ? 1 : (c + 1));
                        }
                        // =======================================================================

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
                        int compareLine;
                        switch (Config.Denominator){
                            case 1:
                                compareLine = measure1.getCode().size();
                                break;
                            case 2:
                                compareLine = (measure1.getCode().size()+measure2.getCode().size())/2;
                                break;
                            case 3:
                                compareLine =measure2.getCode().size();
                                break;
                            default:
                                compareLine = measure1.getCode().size();
                        }

                        float similarity = sameCounter * 1f / compareLine;
                        if (similarity < Config.Similarity) {
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
                        int type = (oneCounter < 3) ? 1 : 2;

                        bufferPairs.add(new Pair(measure1.getId(), measure2.getId(), type));
                    }
                });
                resultLock.lock();
                try {
                    FileUtil.outputBuffer(bufferPairs);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    resultLock.unlock();
                }

//                pairs.addAll(bufferPairs);
                cnt += ids.size();
            }

//            resultLock.lock();
//            try {
//                allPairs.addAll(pairs);
//            } finally {
//                resultLock.unlock();
//            }
            countDownLatch.countDown();
        }
    }
}
