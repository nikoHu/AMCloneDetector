package com.hdu.lexer;

import com.hdu.bean.Measure;
import com.hdu.bean.Token;
import com.hdu.common.CommonTokens;
import com.hdu.common.DetectLanguage;
import com.hdu.conf.Config;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class CommonFileWord extends Word{

    @Override
    public List<Token> segment(String inputFileName, int fileID) {
        try {
            List<Token> tokenHashList = new ArrayList<>();
            Measure measure = new Measure();
            measure.setFileId(fileID);
            measure.setStartLine(1);
            measure.setStartToken(tokenID.get() - Integer.MAX_VALUE);

            switch (Config.Language){
                case DetectLanguage.JAVA:
                    lexer = new JavaLexer(inputFileName);
                    break;
                case DetectLanguage.C:
                    lexer = new CLexer(inputFileName);
                    break;
                case DetectLanguage.CPP:
                    lexer = new CPPLexer(inputFileName);
                    break;
                case DetectLanguage.JAVA_SCRIPT:
                    lexer = new JSLexer(inputFileName);
                    break;
                case DetectLanguage.GO:
                    lexer = new GoLexer(inputFileName);
                    break;
                case DetectLanguage.PYTHON:
                    lexer = new PythonLexer(inputFileName);
                    break;
            }
            Token word = null;
            tokenHashList.add(new Token(CommonTokens.BOUND_LEFT, 1));

            while(!lexer.getReaderIsEnd()){
                word = lexer.scan();
                if (word.getTokenHash() >= 0 || word.getTokenHash() <= -3){
                    tokenHashList.add(word);
                }
            }
            tokenHashList.add(new Token(CommonTokens.BOUND_RIGHT, lexer.line));
            if (tokenHashList.size() < mlc || lexer.line < minLine){
                tokenHashList.clear();
                return new ArrayList<>();
            }

            measure.setEndToken(tokenID.addAndGet(tokenHashList.size()) - Integer.MAX_VALUE);
            measure.setEndLine(lexer.line);
            Measure.measureID.incrementAndGet();
            Measure.measureList.add(measure);
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
