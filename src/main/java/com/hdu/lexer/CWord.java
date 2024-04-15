package com.hdu.lexer;

import com.hdu.bean.Measure;
import com.hdu.bean.Token;
import com.hdu.common.CTokens;
import com.hdu.common.Constants;
import com.hdu.conf.Config;
import com.hdu.simhash.SimHash;
import com.hdu.util.StringUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class CWord extends Word {

    @SuppressWarnings("Duplicates")
    @Override
    public List<Token> segment(String inputFileName, int fileID) {
        List<Token> tmpList = new ArrayList<>();
        List<Token> tokenHashList = new ArrayList<>();
        Stack<Integer> stack = new Stack<>();
        Measure measure = null;

        try {
            lexer = new CLexer(inputFileName);
            Token word = null;
            int functionPos = -1;

            while (!lexer.getReaderIsEnd()) {
                word = lexer.scan();
                if (word.getTokenHash() >= 0 || word.getTokenHash() <= -3) {
                    tmpList.add(word);
                    if (word.getTokenHash() == CTokens.LEFT_BRACE) {
                        stack.push(lexer.line);
                        if (isFunction(tmpList) && functionPos == -1) {
                            functionPos = stack.size();
                            tmpList.clear();
                            tmpList.add(new Token(CTokens.BOUND_LEFT, lexer.line));
                            measure = new Measure(tokenID.get() - Integer.MAX_VALUE, lexer.line);
                        }
                    } else if (word.getTokenHash() == CTokens.RIGHT_BRACE) {
                        if (stack.size() > 0) {
                            if (stack.size() == functionPos) {
                                if ((lexer.line - stack.peek()) > minLine && (lexer.line - stack.peek()) <= maxLine && tmpList.size() >= mlc) {
                                    tmpList.remove(tmpList.size() - 1);
                                    tmpList.add(new Token(CTokens.BOUND_RIGHT, lexer.line));
                                    measure.setFileId(fileID);
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
                                }
                                functionPos = -1;
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

    /**
     * 判断是否为c函数
     * @param tmpList
     * @return
     */
    private boolean isFunction(List<Token> tmpList){
        int size = tmpList.size();
        if (size == 0){
            return false;
        }

        int index = size - 1;
        byte word = tmpList.get(index).getTokenHash();

        /*//判断是否为结构体
        if (index - 1 >= 0){
            if (tmpList.get(index - 1).getTokenHash() == CTokens.STRUCT){
                return true;
            }
        }
        if (index - 2 >= 0){
            if (tmpList.get(index - 1).getTokenHash() == CTokens.STRUCT || tmpList.get(index - 2).getTokenHash() == CTokens.STRUCT){
                return true;
            }
        }*/

        byte midWord = -1;
        int limit = 0;
        while (word != CTokens.RIGHT_PAREN){
            index--;
            if (index < 0){
                break;
            }
            word = tmpList.get(index).getTokenHash();
            limit++;

            //如果遇到=，或者超过20个token都没有找到右小括号，直接返回false
            if (word == CTokens.ASSIGN || limit >= 20){
                return false;
            }

            if (word != CTokens.RIGHT_PAREN){
                midWord = word;
            }
        }
        if (midWord == CTokens.CONST){
            return true;
        }
        if (midWord != -1){
            return false;
        }

        int rightParenCnt = 1;
        while (rightParenCnt != 0){
            index--;
            if (index < 0){
                break;
            }
            word = tmpList.get(index).getTokenHash();
            if (word == CTokens.LEFT_PAREN){
                rightParenCnt--;
            }else if (word == CTokens.RIGHT_PAREN){
                rightParenCnt++;
            }
        }

        index--;
        if (index < 0){
            return false;
        }
        word = tmpList.get(index).getTokenHash();
        if (word == CTokens.FOR || word == CTokens.SWITCH || word == CTokens.IF || word == CTokens.WHILE){
            return false;
        }

        return true;
    }
}
