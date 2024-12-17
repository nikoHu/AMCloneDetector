package com.hdu.util;


import com.hdu.bean.Pair;
import com.hdu.common.Constants;
import com.hdu.conf.Config;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FileUtil {

    public static List<File> listFile(String dir) {
        Path path = Paths.get(dir);
        FilterFileVisitor filterFileVisitor = new FilterFileVisitor();
        try {
            Files.walkFileTree(path, filterFileVisitor);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filterFileVisitor.getFileLists();
    }

    public static void deleteDirectory(String dir) throws IOException {
        Path directoryPath = Paths.get(dir);
        if (Files.exists(directoryPath)) {
            // 删除目录及其内容
            Files.walk(directoryPath)
                    .sorted((path1, path2) -> path2.compareTo(path1)) // 反向排序以确保文件夹在文件之后删除
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    private static class FilterFileVisitor extends SimpleFileVisitor<Path> {

        private List<File> fileLists = new ArrayList<>();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            for (String ext : Config.ExtensionsArray) {
                if (file.toFile().getName().endsWith("." + ext)) {
                    fileLists.add(file.toFile());
                    break;
                }
            }
            return super.visitFile(file, attrs);
        }

        public List<File> getFileLists() {
            return fileLists;
        }
    }

    public static void outputBuffer(@NonNull List<Pair> pairList) throws IOException {
        //区分不同类型的pair
        List<Pair> type1Pairs = pairList.parallelStream().filter(p -> p.getType() == 1).collect(Collectors.toList());
        List<Pair> type2Pairs = pairList.parallelStream().filter(p -> p.getType() == 2).collect(Collectors.toList());

        //输出type1 pair
        List<String> lines = type1Pairs.parallelStream().map(new Function<Pair, String>() {
            @Override
            public String apply(Pair pair) {
                return pair.getId1() + "," + pair.getId2();
            }
        }).collect(Collectors.toList());
        FileUtils.writeLines(new File(Constants.COMMON_PAIR_OUTPUT_FILE), lines, true);

        //输出type2 pair
        lines = type2Pairs.parallelStream().map(new Function<Pair, String>() {
            @Override
            public String apply(Pair pair) {
                return pair.getId1() + "," + pair.getId2();
            }
        }).collect(Collectors.toList());
        FileUtils.writeLines(new File(Constants.SPECIAL_PAIR_OUTPUT_FILE), lines, true);
    }
}
