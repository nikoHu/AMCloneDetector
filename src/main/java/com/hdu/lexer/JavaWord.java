package com.hdu.lexer;


import com.hdu.bean.Measure;
import com.hdu.bean.Token;
import com.hdu.common.Constants;
import com.hdu.common.JavaTokens;
import com.hdu.conf.Config;
import com.hdu.simhash.SimHash;
import com.hdu.util.StringUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class JavaWord extends Word {

    @SuppressWarnings("Duplicates")
    @Override
    public List<Token> segment(String inputFileName, int fileID) {
        List<Token> tmpList = new ArrayList<>();
        List<Token> tokenHashList = new ArrayList<>();
        Stack<Integer> stack = new Stack<>();
        Measure measure = null;
        try {
            lexer = new JavaLexer(inputFileName);
            Token word = null;
            int innerClass = 0;

            while (!lexer.getReaderIsEnd()) {
                word = lexer.scan();
                if (word.getTokenHash() >= 0 || word.getTokenHash() <= -3) {
                    tmpList.add(word);

                    //标记内部类
                    if (stack.size() >= 1 && word.getTokenHash() == JavaTokens.CLASSFLAG && lexer.lastIdentifier != '.') {
                        innerClass++;
                    } else if (word.getTokenHash() == JavaTokens.LEFT_LARGE_BRACKET) {
                        stack.push(lexer.line);
                        if (stack.size() - innerClass == 2) {
                            tmpList = new ArrayList<>();
                            tmpList.add(new Token(JavaTokens.BOUND_LEFT, lexer.line));
                            measure = new Measure(tokenID.get() - Integer.MAX_VALUE, lexer.line);
                            measure.setFileId(fileID);
                        } else {
                            tmpList.remove(tmpList.size() - 1);
                        }
                    } else if (word.getTokenHash() == JavaTokens.RIGHT_LARGE_BRACKET) {
                        if (stack.size() > 0) {
                            if (stack.size() - innerClass == 2 && (lexer.line - stack.peek()) > minLine && (lexer.line - stack.peek()) <= maxLine && tmpList.size() >= mlc) {
                                tmpList.remove(tmpList.size() - 1);
                                tmpList.add(new Token(JavaTokens.BOUND_RIGHT, lexer.line));
                                tokenHashList.addAll(tmpList);
                                measure.setEndToken(tokenID.addAndGet(tmpList.size()) - Integer.MAX_VALUE);
                                measure.setEndLine(lexer.line);
                                measure.setId(Measure.measureID.get());
                                measure.setPath(inputFileName);
                                measure.setLineCount(measure.getEndLine() - measure.getStartLine() + 1);
                                Exception e = null;
                                try {
                                    List<String> lines = FileUtils.readLines(new File(measure.getPath()));
                                    List<String> code = lines.subList(measure.getStartLine(), measure.getEndLine());
                                    code = code.parallelStream().map(String::trim).collect(Collectors.toList());
                                    code = code.parallelStream().filter(s->!s.matches(Constants.FILTER_WORDS_REGEX) && !s.isEmpty()).collect(Collectors.toList());

                                    // 若当前行不以分号结尾，则将下一行合并到当前行
                                    int combineLineIndex = 0;
                                    while (combineLineIndex < code.size()){
                                        // 若当前行是注释行，则跳过
                                        if(code.get(combineLineIndex).startsWith("//")){
                                            combineLineIndex++;
                                            continue;
                                        }
                                        // 若当前为注释块的开始行，则跳过
                                        if(code.get(combineLineIndex).startsWith("/*")){
                                            while (!code.get(combineLineIndex).endsWith("*/") && combineLineIndex < code.size() - 1){
                                                combineLineIndex++;
                                            }
                                            combineLineIndex++;
                                            continue;
                                        }
                                        while (!code.get(combineLineIndex).endsWith(";") && combineLineIndex < code.size() - 1){
                                            code.set(combineLineIndex, code.get(combineLineIndex) + code.get(combineLineIndex + 1));
                                            code.remove(combineLineIndex + 1);
                                        }
                                        combineLineIndex++;
                                    }

                                    measure.setCode(code);

                                    if (Config.CompareType == 2){
                                        List<Long> hash = code.stream().mapToLong(SimHash::computeOptimizedSimHashForString).boxed().collect(Collectors.toList());
                                        measure.setHash(hash);
                                    }else if (Config.CompareType == 3) {
                                        List<Byte> tokens = code.stream().map(StringUtil::hashCodeLine2Byte).collect(Collectors.toList());
                                        measure.setToken(tokens);
                                    }
                                }catch (Exception e2){
                                    e = e2;
                                }
                                tmpList.clear();
                                if (e == null){
                                    Measure.measureID.incrementAndGet();
                                    Measure.measureList.add(measure);
                                }
                            } else {
                                tmpList.remove(tmpList.size() - 1);
                            }
                            if (stack.size() - innerClass == 1 && innerClass > 0) {
                                innerClass--;
                            }
                            stack.pop();
                        }
                    }
                }
            }
            line.addAndGet(lexer.line);
            return tokenHashList;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            lexer.closeReader();
        }
    }
}
