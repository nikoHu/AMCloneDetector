package com.hdu.lexer;

import com.hdu.bean.Token;
import com.hdu.common.PythonTokens;
import com.hdu.conf.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("Duplicates")
public class PythonLexer extends Lexer {

    //当前行的缩进
    public List<Integer> indentPos = new ArrayList<>();

    //上一个不是空行的行
    public int lastNotBlankLine = 1;

    //右小括号所在行
    public int rightParenLine = 0;

    public PythonLexer(String inputFileName) throws IOException {
        super(inputFileName);
    }

    @Override
    public Token scan() throws IOException {
        for (; ; getChar()) {
            if (getReaderIsEnd()) {
                break;
            } else if (peek == ' ') {
                indentPos.add(indexOfChar);
            } else if (peek == '\t' || peek == '\r') {
            }
            else if (peek == '\n') {
                lastNotBlankLine = (indexOfChar > 1)? line: lastNotBlankLine;
                line++;
                indexOfChar = 0;
                indentPos.clear();
            } else if (peek == '#') {
                lastNotBlankLine = (indexOfChar > 1)? line: lastNotBlankLine;
                reader.readLine();
                line++;
                indexOfChar = 0;
                indentPos.clear();
            } else
                break;
        }
        wordIndexOfChar = indexOfChar;
        switch (peek) {
            case '[':
                getChar();
                return new Token(PythonTokens.LEFT_BRACKET, line);
            case ']':
                getChar();
                return new Token(PythonTokens.RIGHT_BRACKET, line);
            case '(':
                getChar();
                return new Token(PythonTokens.LEFT_PAREN, line);
            case ')':
                rightParenLine = line;
                getChar();
                return new Token(PythonTokens.RIGHT_PAREN, line);
            case '{':
                getChar();
                return new Token(PythonTokens.LEFT_BRACE, line);
            case '}':
                getChar();
                return new Token(PythonTokens.RIGHT_BRACE, line);
            case '.':
                getChar();
                return new Token(PythonTokens.DOT, line);
            case ',':
                getChar();
                return new Token(PythonTokens.COMMA, line);
            case ':':
                getChar();
                return new Token(PythonTokens.COLON, line);
            case '+':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(PythonTokens.PLUS_ASSIGN, line);
                } else {
                    return new Token(PythonTokens.PLUS, line);
                }
            case '-':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(PythonTokens.MINUS_ASSIGN, line);
                } else {
                    return new Token(PythonTokens.MINUS, line);
                }
            case '*':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(PythonTokens.STAR_ASSIGN, line);
                } else {
                    return new Token(PythonTokens.STAR, line);
                }
            case '/':
                getChar();
                if (peek == '/') {
                    getChar();
                    return new Token(PythonTokens.DIV2, line);
                } else if (peek == '=') {
                    getChar();
                    return new Token(PythonTokens.DIV_ASSIGN, line);
                } else {
                    return new Token(PythonTokens.DIV, line);
                }
            case '%':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(PythonTokens.MOD_ASSIGN, line);
                } else {
                    return new Token(PythonTokens.MOD, line);
                }
            case '=':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(PythonTokens.EQUAL, line);
                } else {
                    return new Token(PythonTokens.ASSIGN, line);
                }
            case '>':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(PythonTokens.GREATER_EQUAL, line);
                } else if (peek == '>') {
                    getChar();
                    return new Token(PythonTokens.RIGHT_SHIFT, line);
                } else {
                    return new Token(PythonTokens.GREATER, line);
                }
            case '<':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(PythonTokens.LESS_EQUAL, line);
                } else if (peek == '<') {
                    getChar();
                    return new Token(PythonTokens.LEFT_SHIFT, line);
                } else {
                    return new Token(PythonTokens.LESS, line);
                }
            case '&':
                getChar();
                return new Token(PythonTokens.BIT_AND, line);
            case '|':
                getChar();
                return new Token(PythonTokens.BIT_OR, line);
            case '^':
                getChar();
                return new Token(PythonTokens.BIT_CARET, line);
            case '~':
                getChar();
                return new Token(PythonTokens.BIT_TILDE, line);
        }

        // 处理字符串
        if (peek == '\"' && lastPeek != '\'') {
            StringBuilder builder = new StringBuilder();
            builder.append(peek);
            do {
                if (peek == '\\') {
                    getChar();
                    builder.append(peek);
                }else if(peek == ' '){
                    indentPos.add(indexOfChar);
                } else if(peek == '\n'){
                    lastNotBlankLine = line;
                    line++;
                    indexOfChar = 0;
                    indentPos.clear();
                }
                getChar();
                builder.append(peek);
            } while (peek != '\"' && !getReaderIsEnd());
            getChar();
            if (Config.OpenStringHash == 1) {
                return new Token(str2hash(builder.toString()), line);
            }
            return new Token(PythonTokens.IDENTIFIER, line);
        }

        if (peek == '\'') {
            StringBuilder sb = new StringBuilder();
            sb.append(peek);
            do {
                if (peek == '\\') {
                    getChar();
                    sb.append(peek);
                }else if(peek == ' '){
                    indentPos.add(indexOfChar);
                }else if(peek == '\n'){
                    lastNotBlankLine = line;
                    line++;
                    indexOfChar = 0;
                    indentPos.clear();
                }
                getChar();
                sb.append(peek);
            } while (peek != '\'' && !getReaderIsEnd());
            getChar();
            if (Config.OpenStringHash == 1){
                return new Token(str2hash(sb.toString()), line);
            }
            return new Token(PythonTokens.IDENTIFIER, line);
        }

        // 处理数字
        if (Character.isDigit(peek)) {
            int value = 0;
            do {
                value = 10 * value + Character.digit(peek, 10);
                getChar();
            } while (Character.isDigit(peek));
            return new Token(PythonTokens.DIGIT, line);
        }

        // 处理标识符
        if (Character.isLetter(peek)) {
            StringBuilder buffer = new StringBuilder();
            do {
                buffer.append(peek);
                getChar();
            } while (Character.isLetterOrDigit(peek) || peek == '_');
            String s = buffer.toString();

            if (s.equals("import")) {
                reader.readLine();
                getChar();
                lastNotBlankLine = line;
                indexOfChar = 0;
                line++;
                indentPos.clear();
                return new Token((byte)-1, line);
            } else {
                Byte token = PythonTokens.tokenMap.get(s);
                if (token != null) {
                    return new Token(token, line);
                }
                if (Config.OpenStringHash == 1){
                    return new Token(str2hash(s), line);
                }
                return new Token(PythonTokens.IDENTIFIER, line);
            }
        }
        if ((int) peek != 0xffff) {
            getChar();
            return new Token((byte)-1, line);
        }
        return new Token((byte)-2, line);
    }

    /**
     * 获取当前行的缩进
     * @return
     */
    public int getIndent(){
        if (indentPos.size() == 0){
            return 0;
        }
        int indent = 0;
        for (int i=0; i<indexOfChar && i<indentPos.size(); i++){
            if (indentPos.get(i) != i + 1){
                break;
            }
            indent++;
        }
        return indent;
    }
}
