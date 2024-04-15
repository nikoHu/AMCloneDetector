package com.hdu.lexer;

import com.hdu.bean.Measure;
import com.hdu.bean.Token;
import com.hdu.common.Constants;
import com.hdu.common.GoTokens;
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
public class GoWord extends Word{

    @Override
    public List<Token> segment(String inputFileName, int fileID) {
        List<Token> tmpList = new ArrayList<>();
        List<Token> tokenHashList = new ArrayList<>();
        Stack<Integer> stack = new Stack<>();
        Measure measure = null;

        try {
            lexer = new GoLexer(inputFileName);
            Token word = null;
            while (!lexer.getReaderIsEnd()){
                word = lexer.scan();
                if (word.getTokenHash() >= 0 || word.getTokenHash() <= -3){
                    tmpList.add(word);
                    if (word.getTokenHash() == GoTokens.LEFT_LARGE_BRACKET){
                        stack.push(lexer.line);
                        if (stack.size() == 1) {
                            tmpList.clear();
                            tmpList.add(new Token(GoTokens.BOUND_LEFT, lexer.line));
                            measure = new Measure(tokenID.get() - Integer.MAX_VALUE, lexer.line);
                        }
                    }else if (word.getTokenHash() == GoTokens.RIGHT_LARGE_BRACKET){
                        if (stack.size() > 0){
                            if (stack.size() == 1){
                                if ((lexer.line - stack.peek()) > minLine && (lexer.line - stack.peek()) <= maxLine && tmpList.size() >= mlc){
                                    tmpList.remove(tmpList.size() - 1);
                                    tmpList.add(new Token(GoTokens.BOUND_RIGHT, lexer.line));
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
        } finally {
            lexer.closeReader();
        }
    }
}
