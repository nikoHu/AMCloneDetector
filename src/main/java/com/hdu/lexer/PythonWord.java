package com.hdu.lexer;

import com.hdu.bean.Measure;
import com.hdu.bean.Token;
import com.hdu.common.Constants;
import com.hdu.common.PythonTokens;
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
public class PythonWord extends Word{

    private PythonLexer lexer;

    @Override
    public List<Token> segment(String inputFileName, int fileID){
        List<Token> tmpList = new ArrayList<>();
        List<Token> tokenHashList = new ArrayList<>();
        Stack<Integer> stack = new Stack<>();
        Measure measure = null;

        try {
            lexer = new PythonLexer(inputFileName);
            Token word = null;
            int functionIndent = -1;
            while (!lexer.getReaderIsEnd()){
                word = lexer.scan();
                if (word.getTokenHash() >= 0 || word.getTokenHash() <= -3){
                    tmpList.add(word);
                    if (lexer.getIndent() <= functionIndent && lexer.line != stack.peek() && lexer.line != lexer.rightParenLine){
                        if (stack.size() > 0){
                            if ((lexer.line - stack.peek()) > minLine && (lexer.line - stack.peek()) <= maxLine && tmpList.size() >= mlc){
                                tmpList.remove(tmpList.size() - 1);
                                tmpList.add(new Token(PythonTokens.BOUND_RIGHT, lexer.line));
                                measure.setFileId(fileID);
                                tokenHashList.addAll(tmpList);
                                measure.setEndToken(tokenID.addAndGet(tmpList.size()) - Integer.MAX_VALUE);
                                measure.setEndLine(lexer.lastNotBlankLine);
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
                            functionIndent = -1;
                            stack.pop();
                        }
                    }
                    if (word.getTokenHash() == PythonTokens.DEF){
                        stack.push(lexer.line);
                        functionIndent = lexer.getIndent();
                        tmpList.clear();
                        tmpList.add(new Token(PythonTokens.BOUND_LEFT, lexer.line));
                        measure = new Measure(tokenID.get() - Integer.MAX_VALUE, lexer.line);
                    }
                }
            }
            line.addAndGet(lexer.line);
            return tokenHashList;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lexer.closeReader();
        }

        return new ArrayList<>();
    }
}
