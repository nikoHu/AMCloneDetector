package com.hdu.util;

import com.hdu.bean.Measure;
import com.hdu.common.Constants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Printer class
 *
 * @author hauge
 * @date 2019/9/11
 */
public class Printer {

    public static void printMeasureIndex(List<File> files) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(Constants.MEASURE_INDEX_FILENAME);
        List<Measure> list = Measure.measureList;
        for (int i = 0; i < list.size(); i++) {
            pw.write(i + ",");
            pw.write(files.get(list.get(i).getFileId()).getAbsolutePath());
            pw.write(",");
            pw.write(list.get(i).getStartLine() + "");
            pw.write(",");
            pw.write(list.get(i).getEndLine() + "");
            pw.write("\n");
        }
        pw.flush();
        pw.close();
    }
}
