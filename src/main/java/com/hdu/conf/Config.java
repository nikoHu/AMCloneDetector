package com.hdu.conf;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class Config {

    public static final String CONFIG_FILE = "cloneDetector.properties";
    private static Properties properties = new Properties();

    public static Integer CompareType = 1;
    public static Float Similarity = 0.5f;
    public static Float LineGapDis = 0.5f;
    public static Integer MinLine = 10;
    public static Integer MaxLine = 50;
    public static Integer Mlc = 50;
    public static Integer OpenStringHash = 1;
    public static String Language = "java";
    public static String Extensions = "java";
    public static String[] ExtensionsArray = null;


    /**
     * 保存配置
     * @throws IOException
     */
    public static void save() throws IOException {
        properties.setProperty("compare-type", CompareType.toString());
        properties.setProperty("similarity", Similarity.toString());
        properties.setProperty("line-gap-dis", LineGapDis.toString());
        properties.setProperty("min-line", MinLine.toString());
        properties.setProperty("max-line", MaxLine.toString());
        properties.setProperty("mlc", Mlc.toString());
        properties.setProperty("open-string-hash", OpenStringHash.toString());
        properties.setProperty("language", Language);
        properties.setProperty("extensions", Extensions);
        FileWriter writer = new FileWriter(new File(CONFIG_FILE));
        properties.store(writer, "");
        writer.close();
    }

    /**
     * 加载配置
     * @throws IOException
     */
    public static void load() throws IOException{
        FileReader reader = new FileReader(CONFIG_FILE);
        properties.load(reader);
        reader.close();

        CompareType = Integer.parseInt(properties.getProperty("compare-type"));
        Similarity = Float.parseFloat(properties.getProperty("similarity"));
        LineGapDis = Float.parseFloat(properties.getProperty("line-gap-dis"));
        MinLine = Integer.parseInt(properties.getProperty("min-line"));
        MaxLine = Integer.parseInt(properties.getProperty("max-line"));
        Mlc = Integer.parseInt(properties.getProperty("mlc"));
        OpenStringHash = Integer.parseInt(properties.getProperty("open-string-hash"));
        Language = properties.getProperty("language");
        Extensions = properties.getProperty("extensions");
        ExtensionsArray = Extensions.split(",");
    }
}
