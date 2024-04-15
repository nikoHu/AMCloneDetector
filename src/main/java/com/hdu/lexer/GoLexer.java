package com.hdu.lexer;

import com.hdu.bean.Token;
import com.hdu.common.GoTokens;
import com.hdu.conf.Config;

import java.io.IOException;

@SuppressWarnings("Duplicates")
public class GoLexer extends Lexer{
    
    public GoLexer(String inputFileName) throws IOException {
        super(inputFileName);
    }

    @Override
    public Token scan() throws IOException {
        for (; ; getChar()) {
            if (getReaderIsEnd()) {
                break;
            } else if (peek == ' ' || peek == '\t' || peek == '\r') {
            } else if (peek == '\n') {
                line = line + 1;
                indexOfChar = 0;
            } else if (peek == '/') {
                getChar();
                if (peek == '*') {
                    for (; ; getChar()) {
                        if (peek == '\n'){
                            line++;
                            indexOfChar = 0;
                        }
                        if (lastPeek == '*' && peek == '/') {
                            break;
                        }
                        if (getReaderIsEnd()) {
                            break;
                        }
                    }
                } else if (peek == '/') {
                    reader.readLine();
                    indexOfChar = 0;
                    line = line + 1;
                } else if (peek == '\n') {
                    line = line + 1;
                }
            } else
                break;
        }
        wordIndexOfChar = indexOfChar;
        switch (peek) {
            case '\\':
                getChar();
                if (peek == '\n') {
                    line++;
                    indexOfChar = 0;
                }
                getChar();
                return new Token((byte)-1, line);
            case '[':
                getChar();
                return new Token(GoTokens.LEFT_MID_BRACKET, line);
            case ']':
                getChar();
                return new Token(GoTokens.RIGHT_MID_BRACKET, line);
            case '(':
                getChar();
                return new Token(GoTokens.LEFT_SMALL_BRACKET, line);
            case ')':
                getChar();
                return new Token(GoTokens.RIGHT_SMALL_BRACKET, line);
            case '.':
                getChar();
                return new Token(GoTokens.DOT, line);
            case ',':
                getChar();
                return new Token(GoTokens.COMMA, line);
            case ':':
                getChar();
                return new Token(GoTokens.COLON, line);
            case ';':
                getChar();
                return new Token(GoTokens.SEMI, line);
            case '{':
                getChar();
                return new Token(GoTokens.LEFT_LARGE_BRACKET, line);
            case '}':
                getChar();
                return new Token(GoTokens.RIGHT_LARGE_BRACKET, line);
            case '+':
                getChar();
                if (peek == '+') {
                    getChar();
                    return new Token(GoTokens.PLUS_PLUS, line);
                } else if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.PLUS_ASSIGN, line);
                } else {
                    return new Token(GoTokens.PLUS, line);
                }
            case '-':
                getChar();
                if (peek == '-') {
                    getChar();
                    return new Token(GoTokens.MINUS_MINUS, line);
                } else if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.MINUS_ASSIGN, line);
                } else {
                    return new Token(GoTokens.MINUS, line);
                }
            case '*':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.STAR_ASSIGN, line);
                } else {
                    return new Token(GoTokens.STAR, line);
                }
            case '/':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.DIV_ASSIGN, line);
                } else {
                    return new Token(GoTokens.DIV, line);
                }
            case '%':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.MOD_ASSIGN, line);
                } else {
                    return new Token(GoTokens.MOD, line);
                }
            case '=':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.EQUAL, line);
                } else {
                    return new Token(GoTokens.ASSIGN, line);
                }
            case '>':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.GREATER_EQUAL, line);
                } else if (peek == '>') {
                    getChar();
                    return new Token(GoTokens.RIGHT_SHIFT, line);
                } else {
                    return new Token(GoTokens.GREATER, line);
                }
            case '<':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.LESS_EQUAL, line);
                } else if (peek == '<') {
                    getChar();
                    return new Token(GoTokens.LEFT_SHIFT, line);
                } else {
                    return new Token(GoTokens.LESS, line);
                }
            case '!':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.NOT_EQUAL, line);
                } else {
                    return new Token(GoTokens.NOT, line);
                }
            case '&':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.AND_ASSIGN, line);
                } else if (peek == '&') {
                    getChar();
                    return new Token(GoTokens.AND, line);
                } else {
                    return new Token(GoTokens.AND, line);
                }
            case '|':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(GoTokens.OR_ASSIGN, line);
                } else if (peek == '|') {
                    getChar();
                    return new Token(GoTokens.OR, line);
                } else {
                    return new Token(GoTokens.OR, line);
                }
            case '^':
                getChar();
                return new Token(GoTokens.CARET, line);
        }

        // 处理字符串
        if (peek == '\"' && lastPeek != '\'') {
            StringBuilder builder = new StringBuilder();
            builder.append(peek);
            do {
                if (peek == '\\') {
                    getChar();
                    builder.append(peek);
                    getChar();
                } else {
                    getChar();
                }
                builder.append(peek);
            } while (peek != '\"' && !getReaderIsEnd());
            getChar();
            if (Config.OpenStringHash == 1){
                return new Token(str2hash(builder.toString()), line);
            }
            return new Token(GoTokens.ID, line);

        }

        // 处理数字
        if (Character.isDigit(peek)) {
            int value = 0;
            do {
                value = 10 * value + Character.digit(peek, 10);
                getChar();
            } while (Character.isDigit(peek));
            return new Token(GoTokens.DIGIT, line);
        }

        // 处理标识符
        if (Character.isLetter(peek)) {
            StringBuilder buffer = new StringBuilder();
            do {
                buffer.append(peek);
                getChar();
            } while (Character.isLetterOrDigit(peek) || peek == '_');
            String s = buffer.toString();

            if (s.equals("include")) {
                reader.readLine();
                getChar();
                indexOfChar = 0;
                line = line + 1;
                return new Token((byte)-1, line);
            } else {
                Byte token = GoTokens.tokenMap.get(s);
                if (token != null) {
                    return new Token(token, line);
                }
                if (Config.OpenStringHash == 1){
                    return new Token(str2hash(s), line);
                }
                return new Token(GoTokens.ID, line);
            }
        }
        if ((int) peek != 0xffff) {
            getChar();
            return new Token((byte)-1, line);
        }
        return new Token((byte)-2, line);
    }
}
