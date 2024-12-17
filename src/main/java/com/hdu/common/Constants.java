package com.hdu.common;

public class Constants {

    public static final String FILE_LIST = "result/files.txt";
    public static final String MEASURE_INDEX_FILENAME = "result/measure-index.csv";
    public static final String COMMON_PAIR_OUTPUT_FILE = "result/common-pair.csv";
    public static final String SPECIAL_PAIR_OUTPUT_FILE = "result/special-pair.csv";
    public static final String RESULT_PATH = "result/";

    public static final String FILTER_WORDS_REGEX = "\\{|}|}else\\{|try[\\s]*\\{|}catch[\\s]*\\(Exception e\\)\\{|continue;|}\\);|" +
            "}[\\s]*finally[\\s]*\\{|break;";
}
