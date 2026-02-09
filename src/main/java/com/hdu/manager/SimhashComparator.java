package com.hdu.manager;

import com.hdu.bean.Measure;
import com.hdu.bean.Pair;
import com.hdu.conf.Config;
import com.hdu.simhash.SimHash;
import com.hdu.util.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
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

@Slf4j
@SuppressWarnings({"Duplicates"})
public class SimhashComparator implements Comparator {

    private ReentrantLock lock = new ReentrantLock();
    // 用于向最终结果添加子线程检测结果的锁
    private ReentrantLock resultLock = new ReentrantLock();
    // 用于生成ID对的锁
    private static final Object generateIDsLock = new Object();

    // 已处理id对的数量
    private static AtomicLong dealCount = new AtomicLong(0);

    // ====== 进度统计（每 10% 输出一次） ======
    private static final AtomicLong totalPairCount = new AtomicLong(1);
    private static final AtomicLong nextReportPercent = new AtomicLong(10);

    private static void reportProgress(long processed) {
        long total = totalPairCount.get();
        if (total <= 0) return;

        while (true) {
            long p = nextReportPercent.get(); // 10,20,...,100
            if (p > 100) return;

            long threshold = (total * p) / 100;
            if (processed >= threshold) {
                if (nextReportPercent.compareAndSet(p, p + 10)) {
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

    @Override
    public List<Pair> findPairs(List<Measure> measureList) {
        List<Pair> pairs = new ArrayList<>();
        if (measureList.size() < 2) {
            return pairs;
        }

        int buffer = Config.Buffer;
        IDPairGenerator generator = new MeasureIDPairGenerator(measureList, Config.LineGapDis);
        List<String> ids = generator.generate(buffer);

        // ====== 初始化总量与进度计数器 ======
        long n = measureList.size();
        totalPairCount.set(n * (n - 1) / 2);
        dealCount.set(0);
        nextReportPercent.set(10);
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
                    for (long hash2 : measure2.getHash()) {
                        boolean same = false;
                        for (long hash1 : measure1.getHash()) {
                            if (SimHash.hammingDistance(hash1, hash2) <= 1) {
                                sameCounter++;
                                same = true;
                            }
                        }
                        if (same) {
                            sequence.append("1");
                        } else {
                            sequence.append("0");
                        }
                    }

                    int compareLine;
                    switch (Config.Denominator) {
                        case 1:
                            compareLine = measure1.getCode().size();
                            break;
                        case 2:
                            compareLine = (measure1.getCode().size() + measure2.getCode().size()) / 2;
                            break;
                        case 3:
                            compareLine = measure2.getCode().size();
                            break;
                        default:
                            compareLine = measure1.getCode().size();
                    }
                    if (compareLine <= 0) {
                        return;
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

        // ====== 最终进度输出 ======
        reportFinalProgress();
        // ========================

        return pairs;
    }

    @Override
    public List<Pair> findPairsByAsync(final List<Measure> measureList) {
        List<Pair> pairs = new ArrayList<>();
        if (measureList.size() < 2) {
            return pairs;
        }

        int buffer = Config.Buffer;
        IDPairGenerator generator = new MeasureIDPairGenerator(measureList, Config.LineGapDis);

        // ====== 初始化总量与进度计数器 ======
        long n = measureList.size();
        totalPairCount.set(n * (n - 1) / 2);
        dealCount.set(0);
        nextReportPercent.set(10);
        // ======================================

        // ====== FIX：每次调用都重新 new latch（不能是成员变量复用） ======
        CountDownLatch countDownLatch = new CountDownLatch(Config.ThreadNum);
        // ==================================================================

        // ====== FIX：每次运行前清空 allPairs，避免残留 ======
        allPairs.clear();
        // ====================================================

        ExecutorService executor = Executors.newFixedThreadPool(Config.ThreadNum);
        for (int i = 0; i < Config.ThreadNum; i++) {
            executor.submit(new GenerateIDs(generator, buffer, measureList, countDownLatch));
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ====== 最终进度输出 ======
        reportFinalProgress();
        // ========================

        executor.shutdown();

        // 按你原来写法：返回 allPairs（这里才有数据）
        return allPairs;
    }

    private class GenerateIDs implements Runnable {
        private IDPairGenerator generator;
        private int buffer;
        private List<Measure> measureList;
        private CountDownLatch countDownLatch;

        public GenerateIDs(IDPairGenerator generator, int buffer, List<Measure> measureList, CountDownLatch countDownLatch) {
            this.generator = generator;
            this.buffer = buffer;
            this.measureList = measureList;
            this.countDownLatch = countDownLatch;
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
            try {
                List<String> ids;
                long cnt = 0;

                while ((ids = generateIDs()).size() != 0) {
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
                            for (long hash2 : measure2.getHash()) {
                                boolean same = false;
                                for (long hash1 : measure1.getHash()) {
                                    if (SimHash.hammingDistance(hash1, hash2) <= 1) {
                                        sameCounter++;
                                        same = true;
                                    }
                                }
                                if (same) {
                                    sequence.append("1");
                                } else {
                                    sequence.append("0");
                                }
                            }

                            int compareLine;
                            switch (Config.Denominator) {
                                case 1:
                                    compareLine = measure1.getCode().size();
                                    break;
                                case 2:
                                    compareLine = (measure1.getCode().size() + measure2.getCode().size()) / 2;
                                    break;
                                case 3:
                                    compareLine = measure2.getCode().size();
                                    break;
                                default:
                                    compareLine = measure1.getCode().size();
                            }
                            if (compareLine <= 0) {
                                return;
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

                    resultLock.lock();
                    try {
                        FileUtil.outputBuffer(bufferPairs);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        resultLock.unlock();
                    }

                    // ====== FIX：异步版要汇总到 allPairs，保证最终返回有数据 ======
                    allPairs.addAll(bufferPairs);
                    // ===============================================================

                    cnt += ids.size();
                    log.info(Thread.currentThread().getName() + " processing {}", cnt);
                }
            } finally {
                // ====== FIX：放 finally，避免异常导致 await 永久卡住 ======
                countDownLatch.countDown();
                // ==========================================================
            }
        }
    }
}