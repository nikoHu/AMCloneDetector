package com.hdu.lexer;


import com.hdu.bean.Measure;
import com.hdu.bean.Token;
import com.hdu.common.Constants;
import com.hdu.common.JSTokens;
import com.hdu.conf.Config;
import com.hdu.simhash.SimHash;
import com.hdu.util.StringUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class JSWord extends Word{


    @Override
    public List<Token> segment(String inputFileName, int fileID) {
        List<Token> tmpList = new ArrayList<>();
        List<Token> tokenHashList = new ArrayList<>();
        Stack<Integer> stack = new Stack<>();
        Measure measure = null;

        try {
            lexer = new JSLexer(inputFileName);
            Token word = null;
            int functionPos = -1;
            while (!lexer.getReaderIsEnd()){
                word = lexer.scan();
                if (word.getTokenHash() >= 0 || word.getTokenHash() <= -3){
                    tmpList.add(word);
                    if (word.getTokenHash() == JSTokens.LEFT_LARGE_BRACKET){
                        stack.push(lexer.line);
                        if (isFunction(tmpList) && functionPos == -1) {
                            functionPos = stack.size();
                            tmpList.clear();
                            tmpList.add(new Token(JSTokens.BOUND_LEFT, lexer.line));
                            measure = new Measure(tokenID.get() - Integer.MAX_VALUE, lexer.line);
                        }
                    }else if (word.getTokenHash() == JSTokens.RIGHT_LARGE_BRACKET){
                        if (stack.size() > 0){
                            if (stack.size() == functionPos){
                                if ((lexer.line - stack.peek()) > minLine && (lexer.line - stack.peek()) <= maxLine && tmpList.size() >= mlc){
                                    tmpList.remove(tmpList.size() - 1);
                                    tmpList.add(new Token(JSTokens.BOUND_RIGHT, lexer.line));
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
        }catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }finally {
            lexer.closeReader();
        }
    }

    /**
     * 判断大括号是否为函数
     * @param tmpList
     * @return
     */
    private boolean isFunction(List<Token> tmpList){
        int size = tmpList.size();
        if (size < 2){
            return false;
        }

        if (tmpList.get(size-2).getTokenHash() != JSTokens.RIGHT_SMALL_BRACKET){
            return false;
        }
        int index = size - 2;
        byte word = tmpList.get(index).getTokenHash();
        int rightSmallBracketCnt = 1;
        while (rightSmallBracketCnt != 0){
            index--;
            if (index < 0){
                break;
            }
            word = tmpList.get(index).getTokenHash();
            if (word == JSTokens.RIGHT_SMALL_BRACKET){
                rightSmallBracketCnt++;
            }else if (word == JSTokens.LEFT_SMALL_BRACKET){
                rightSmallBracketCnt--;
            }
        }

        if (index - 2 >= 0){
            if (tmpList.get(index - 2).getTokenHash() == JSTokens.FUNCTION || tmpList.get(index - 1).getTokenHash() == JSTokens.FUNCTION){
                return true;
            }
            byte lastWord = tmpList.get(index - 1).getTokenHash();
            if (lastWord != JSTokens.FOR
                    && lastWord != JSTokens.SWITCH
                    && lastWord != JSTokens.CATCH
                    && lastWord != JSTokens.IF
                    && lastWord != JSTokens.WHILE){
                return true;
            }
        }

        if (index - 1 >= 0){
            if (tmpList.get(index - 1).getTokenHash() == JSTokens.FUNCTION){
                return true;
            }
            byte lastWord = tmpList.get(index - 1).getTokenHash();
            if (lastWord != JSTokens.FOR
                    && lastWord != JSTokens.SWITCH
                    && lastWord != JSTokens.CATCH
                    && lastWord != JSTokens.IF
                    && lastWord != JSTokens.WHILE){
                return true;
            }
        }

        return false;
    }
}
