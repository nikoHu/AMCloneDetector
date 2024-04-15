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
