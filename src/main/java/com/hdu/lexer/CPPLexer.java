package com.hdu.lexer;


import com.hdu.bean.Token;
import com.hdu.common.CPPTokens;
import com.hdu.conf.Config;

import java.io.IOException;

@SuppressWarnings("Duplicates")
public class CPPLexer extends Lexer {

    public CPPLexer(String inputFileName) throws IOException {
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
                return new Token(CPPTokens.LEFT_BRACKET,line);
            case ']':
                getChar();
                return new Token(CPPTokens.RIGHT_BRACKET, line);
            case '(':
                getChar();
                return new Token(CPPTokens.LEFT_PAREN, line);
            case ')':
                getChar();
                return new Token(CPPTokens.RIGHT_PAREN, line);
            case '.':
                getChar();
                return new Token(CPPTokens.DOT, line);
            case ',':
                getChar();
                return new Token(CPPTokens.COMMA, line);
            case ':':
                getChar();
                return new Token(CPPTokens.COLON, line);
            case ';':
                getChar();
                return new Token(CPPTokens.SEMI, line);
            case '{':
                getChar();
                return new Token(CPPTokens.LEFT_BRACE, line);
            case '}':
                getChar();
                return new Token(CPPTokens.RIGHT_BRACE, line);
            case '+':
                getChar();
                if (peek == '+') {
                    getChar();
                    return new Token(CPPTokens.PLUS_PLUS, line);
                } else if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.PLUS_ASSIGN, line);
                } else {
                    return new Token(CPPTokens.PLUS, line);
                }
            case '-':
                getChar();
                if (peek == '-') {
                    getChar();
                    return new Token(CPPTokens.MINUS_MINUS, line);
                } else if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.MINUS_ASSIGN, line);
                } else if (peek == '>') {
                    getChar();
                    return new Token(CPPTokens.ARROW, line);
                } else {
                    return new Token(CPPTokens.MINUS, line);
                }
            case '*':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.STAR_ASSIGN, line);
                } else {
                    return new Token(CPPTokens.STAR, line);
                }
            case '/':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.DIV_ASSIGN, line);
                } else {
                    return new Token(CPPTokens.DIV, line);
                }
            case '%':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.MOD_ASSIGN, line);
                } else {
                    return new Token(CPPTokens.MOD, line);
                }
            case '=':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.EQUAL, line);
                } else {
                    return new Token(CPPTokens.ASSIGN, line);
                }
            case '>':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.GREATER_EQUAL, line);
                } else if (peek == '>') {
                    getChar();
                    return new Token(CPPTokens.RIGHT_SHIFT, line);
                } else {
                    return new Token(CPPTokens.GREATER, line);
                }
            case '<':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.LESS_EQUAL, line);
                } else if (peek == '<') {
                    getChar();
                    return new Token(CPPTokens.LEFT_SHIFT, line);
                } else {
                    return new Token(CPPTokens.LESS, line);
                }
            case '!':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.NOT_EQUAL, line);
                } else {
                    return new Token(CPPTokens.NOT, line);
                }
            case '&':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.AND_ASSIGN, line);
                } else if (peek == '&') {
                    getChar();
                    return new Token(CPPTokens.AND_AND, line);
                } else {
                    return new Token(CPPTokens.AND, line);
                }
            case '|':
                getChar();
                if (peek == '=') {
                    getChar();
                    return new Token(CPPTokens.OR_ASSIGN, line);
                } else if (peek == '|') {
                    getChar();
                    return new Token(CPPTokens.OR_OR, line);
                } else {
                    return new Token(CPPTokens.OR, line);
                }
        }

        // 处理字符串
        if (peek == '\"' && lastPeek != '\'') {
            StringBuilder builder = new StringBuilder();
            builder.append(peek);
            do {
                if (peek == '\\') {
                    getChar();
                    builder.append(peek);
                    if (peek == '\n'){
                        line++;
                        indexOfChar = 0;
                    }
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
            return new Token(CPPTokens.IDENTIFIER, line);
        }

        // 处理数字
        if (Character.isDigit(peek)) {
            int value = 0;
            do {
                value = 10 * value + Character.digit(peek, 10);
                getChar();
            } while (Character.isDigit(peek));
            return new Token(CPPTokens.DIGIT, line);
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
                Byte token = CPPTokens.tokenMap.get(s);
                if (token != null) {
                    return new Token(token, line);
                }
                if (Config.OpenStringHash == 1){
                    return new Token(str2hash(s), line);
                }
                return new Token(CPPTokens.IDENTIFIER, line);
            }

        }
        if ((int) peek != 0xffff) {
            getChar();
            return new Token((byte)-1, line);
        }
        return new Token((byte)-2, line);
    }
}
