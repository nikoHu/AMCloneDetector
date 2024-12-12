package com.hdu.processor;

import com.hdu.bean.Measure;
import com.hdu.bean.Pair;
import com.hdu.common.Constants;
import com.hdu.common.DetectLanguage;
import com.hdu.conf.Config;
import com.hdu.lexer.*;
import com.hdu.manager.Comparator;
import com.hdu.manager.SimhashComparator;
import com.hdu.manager.StringComparator;
import com.hdu.manager.TokenComparator;
import com.hdu.util.FileUtil;
import com.hdu.util.Printer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class SRCFProcessor {

    private String[] args = null;

    public SRCFProcessor(String[] args) {
        this.args = args;
    }

    public void run(){
        //检查入口参数
        checkArgs();

        //加载配置
        long start = System.currentTimeMillis();
        try {
            log.info("load config");
            loadConfig();
        }catch (IOException e){
            e.printStackTrace();
            System.exit(2);
        }

        //加载文件列表
        log.info("loading files");
        File dataset = new File(args[0]);
        List<File> files = new ArrayList<>();
        if(dataset.isDirectory()){
            files = FileUtil.listFile(dataset.getAbsolutePath());
        }else{
            try {
                List<String> paths = FileUtils.readLines(dataset);
                files = paths.stream().map(new Function<String, File>() {

                    @Override
                    public File apply(String s) {
                        return new File(s);
                    }
                }).collect(Collectors.toList());
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        log.info("{} files have been loaded", files.size());
        try {
            FileUtils.writeLines(new File(Constants.FILE_LIST), files);
        }catch (IOException e){
            e.printStackTrace();
        }


        //加载measureList
        log.info("loading measure list");
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        List<File> finalFiles = files;
        Word word = initWord();
        for(int i=0; i<files.size(); i++){
            final int index = i;
            executorService.submit(() -> {
                log.info(Thread.currentThread().getName() + ' ' + String.format("%.2f%%", index * 100f / finalFiles.size()));
                word.segment(finalFiles.get(index).getAbsolutePath(), index);
            });
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        //输出measureList
        log.info("outputting to {}", Constants.MEASURE_INDEX_FILENAME);
        try {
            Printer.printMeasureIndex(files);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }

        //按照代码行数排序measureList
        Measure.sortMeasureList(Measure.measureList);


        //初始化比较器
        log.info("init comparator");
        Comparator comparator = null;
        switch (Config.CompareType){
            case 1:
                comparator = new StringComparator();
                break;
            case 2:
                comparator = new SimhashComparator();
                break;
            case 3:
                comparator = new TokenComparator();
                break;
            default:
                comparator = new StringComparator();
        }

        //寻找目标克隆对
        log.info("find pairs");
        List<Pair> pairList = comparator.findPairsByAsync(Measure.measureList);
//        List<Pair> pairList = comparator.findPairs(Measure.measureList);
        //输出
        log.info("output pairs");
        try {
            output(pairList);
        }catch (IOException e){
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        log.info("task finish, time cost: {}", Duration.ofMillis(end - start).toString());
    }

    /**
     * 加载配置
     */
    private void loadConfig()throws IOException {
        File configFile = new File(Config.CONFIG_FILE);
        if (!configFile.exists()){
            log.info("please update {}", Config.CONFIG_FILE);
            Config.save();
            System.exit(2);
        }
        Config.load();
    }

    private void output(@NonNull  List<Pair> pairList) throws IOException{
        //区分不同类型的pair
        List<Pair> type1Pairs = pairList.parallelStream().filter(p->p.getType() == 1).collect(Collectors.toList());
        List<Pair> type2Pairs = pairList.parallelStream().filter(p->p.getType() == 2).collect(Collectors.toList());

        //输出type1 pair
        List<String> lines = type1Pairs.parallelStream().map(new Function<Pair, String>() {
            @Override
            public String apply(Pair pair) {
                return pair.getId1() + "," + pair.getId2();
            }
        }).collect(Collectors.toList());
        FileUtils.writeLines(new File(Constants.COMMON_PAIR_OUTPUT_FILE), lines);

        //输出type2 pair
        lines = type2Pairs.parallelStream().map(new Function<Pair, String>() {
            @Override
            public String apply(Pair pair) {
                return pair.getId1() + "," + pair.getId2();
            }
        }).collect(Collectors.toList());
        FileUtils.writeLines(new File(Constants.SPECIAL_PAIR_OUTPUT_FILE), lines);
    }

    /**
     * 入口参数校验
     */
    private void checkArgs(){
        if (args == null || args.length < 1){
            log.info("usage: java -jar single-responsibility-code-finder.jar [dataset, files]");
            log.info("dataset is a folder");
            log.info("files is a file contains file paths");
            System.exit(0);
        }
    }

    /**
     * 初始化分词器
     * @return
     */
    private Word initWord(){
        Word word = null;
        switch (Config.Language){
            case DetectLanguage.JAVA:
                word = new JavaWord();
                break;
            case DetectLanguage.C:
                word = new CWord();
                break;
            case DetectLanguage.CPP:
                word = new CPPWord();
                break;
            case DetectLanguage.JAVA_SCRIPT:
                word = new JSWord();
                break;
            case DetectLanguage.PYTHON:
                word = new PythonWord();
                break;
            case DetectLanguage.GO:
                word = new GoWord();
                break;
        }
        word.setMlc(Config.Mlc);
        word.setMinLine(Config.MinLine);
        word.setMaxLine(Config.MaxLine);
        return word;
    }
}
