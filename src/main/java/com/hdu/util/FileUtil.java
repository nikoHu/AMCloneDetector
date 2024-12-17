package com.hdu.util;


import com.hdu.conf.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

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
}
