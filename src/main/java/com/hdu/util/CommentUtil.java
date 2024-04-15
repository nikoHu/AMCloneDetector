package com.hdu.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommentUtil {

    public static final String COMMENT_SINGLE_LINE = "//[\\s\\S]*?\n";
    public static final String COMMENT_MULTI_LINE = "/\\*[\\s\\S]*?\\*/";

    /**
     * 根据传入的语言类型，移除代码中的注释
     * @param code
     * @return
     */
    public static List<String> removeComments2(List<String> code){
        String content = String.join("\n", code);
        content = removeComments(content, COMMENT_SINGLE_LINE);
        content = removeComments(content, COMMENT_MULTI_LINE);
        return Arrays.stream(content.split("\n")).collect(Collectors.toList());
    }

    /**
     * 移除代码中的注释
     * @param code
     * @param pattern
     * @return
     */
    public static String removeComments(String code, String pattern){
        return code.replaceAll(pattern, "");
    }
}
